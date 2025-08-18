package com.ai.infrastructure.repository;

import com.ai.application.dto.ChatMessageDto;
import com.ai.domain.entity.NoSqlChat;
import com.ai.domain.model.pagination.ChatPage;
import com.ai.domain.model.pagination.CursorMeta;
import com.ai.domain.model.pagination.PageMeta;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.*;
import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.cassandra.core.cql.CqlTemplate;
import org.springframework.data.cassandra.core.cql.SessionCallback;
import org.springframework.util.Assert;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public class CassandraChatMemoryRepository implements ChatRepository<NoSqlChat> {

    private static final Logger log = LoggerFactory.getLogger(CassandraChatMemoryRepository.class);

    private static final String ID_CANNOT_BE_NULL_OR_EMPTY = "id cannot be null or empty";

    private final CqlTemplate cqlTemplate;
    private final CqlSession cqlSession;

    public CassandraChatMemoryRepository(CqlTemplate cqlTemplate, CqlSession cqlSession) {
        this.cqlTemplate = cqlTemplate;
        this.cqlSession = cqlSession;
    }

    @Override
    public NoSqlChat save(String chatName) {
        NoSqlChat noSqlChat = new NoSqlChat(
                chatName,
                UUID.randomUUID().toString()
        );

        boolean executed = cqlTemplate.execute(
                "INSERT INTO ai_chat_memory (session_id, session_name, created_at) VALUES (?, ?, ?) IF NOT EXISTS",
                noSqlChat.getId(),
                noSqlChat.getName(),
                noSqlChat.getCreatedAt()
        );
        if (!executed) {
            log.error("Insert failed for chatId={} into ai_chat_memory", noSqlChat.getId());
            throw new IllegalStateException(
                    "Failed to insert chat with id=" + noSqlChat.getId()
            );
        }

        UUID createdAtTimeUuid = Uuids.startOf(noSqlChat.getCreatedAt().toEpochMilli());
        cqlSession.execute(
                "INSERT INTO chats_by_created (bucket, created_at, session_id, session_name) VALUES ('all', ?, ?, ?)",
                createdAtTimeUuid,
                noSqlChat.getId(),
                noSqlChat.getName()
        );

        log.info("Successfully inserted chatId={} into ai_chat_memory", noSqlChat.getId());
        return noSqlChat;
    }

    @Override
    public List<Message> findByConversationId(String chatId) {
        Assert.hasText(chatId, ID_CANNOT_BE_NULL_OR_EMPTY);

        return cqlTemplate.query(
                """
                        SELECT msg_type, msg_content, msg_timestamp
                        FROM ai_chat_message
                        WHERE session_id = ?
                        """,
                (row, rowNum) -> {
                    String type = row.getString("msg_type");
                    String content = row.getString("msg_content");
                    Instant ts = row.getInstant("msg_timestamp");

                    return switch (type) {
                        case "user" -> new UserMessage(content);
                        case "assistant" -> new AssistantMessage(content);
                        case "system" -> new SystemMessage(content);
                        default -> throw new UnsupportedOperationException("Message type not supported" + type);
                    };
                },
                chatId
        );
    }

    @Override
    public List<Message> findLastNByConversationId(String chatId, int limit) {
        Assert.hasText(chatId, ID_CANNOT_BE_NULL_OR_EMPTY);

        return cqlTemplate.query(
                """
                        SELECT msg_type, msg_content, msg_timestamp
                        FROM ai_chat_message
                        WHERE session_id = ?
                        LIMIT ?
                        """,
                (row, rowNum) -> {
                    String type = row.getString("msg_type");
                    String content = row.getString("msg_content");
                    Instant ts = row.getInstant("msg_timestamp");

                    return switch (type) {
                        case "user" -> new UserMessage(content);
                        case "assistant" -> new AssistantMessage(content);
                        case "system" -> new SystemMessage(content);
                        default -> throw new UnsupportedOperationException("Message type not supported" + type);
                    };
                },
                chatId,
                limit
        );
    }

    @Override
    public void deleteById(String chatId) {
        Assert.hasText(chatId, ID_CANNOT_BE_NULL_OR_EMPTY);

        this.deleteByConversationId(chatId);

        Instant createdAt = cqlTemplate.queryForObject(
                "SELECT created_at FROM ai_chat_memory WHERE session_id = ?",
                (row, n) -> row.getInstant("created_at"),
                chatId
        );
        UUID createdAtTimeUuid = Uuids.startOf(createdAt.toEpochMilli());
        cqlSession.execute(
                "DELETE FROM chats_by_created WHERE bucket = 'all' AND created_at = ? AND session_id = ?",
                createdAtTimeUuid, chatId
        );
        cqlTemplate.execute("DELETE FROM ai_chat_memory WHERE session_id = ?", chatId);
    }

    @Override
    public void deleteByConversationId(String chatId) {
        Assert.hasText(chatId, ID_CANNOT_BE_NULL_OR_EMPTY);

        cqlTemplate.execute(
                "DELETE FROM ai_chat_message WHERE session_id = ?",
                chatId
        );
    }

    @Override
    public void saveAll(String chatId, List<Message> messages) {
        Assert.hasText(chatId, ID_CANNOT_BE_NULL_OR_EMPTY);
        Assert.notEmpty(messages, "messages cannot be null or empty");

        var ps = cqlSession.prepare(
                "INSERT INTO ai_chat_message " +
                        "(session_id, msg_timestamp, msg_type, msg_content) " +
                        "VALUES (?, ?, ?, ?)"
        );

        var batch = BatchStatement.builder(DefaultBatchType.UNLOGGED); // Ignore batch log

        for (Message m : messages) {
            batch.addStatement(ps.bind(
                    chatId,
                    m.getMetadata().get("msg_timestamp"),
                    m.getMessageType().getValue(),
                    m.getText()
            ));
        }
        cqlSession.execute(batch.build());
    }

    @Override
    public List<NoSqlChat> findAll() {
        return cqlTemplate.query(
                "SELECT session_id, session_name, created_at " +
                        "FROM chats_by_created WHERE bucket = 'all'",
                (row, rowNum) -> new NoSqlChat(
                        row.getString("session_id"),
                        row.getString("session_name"),
                        // convert timeuuid to Instant
                        Instant.ofEpochMilli(
                                Uuids.unixTimestamp(row.getUuid("created_at"))
                        )
                )
        );
    }

    @Override
    public List<String> findConversationIds() {
        return cqlTemplate.query(
                "SELECT session_id FROM ai_chat_memory",
                (row, rowNum) -> row.getString("session_id")
        );
    }

    @Override
    public ChatPage findByConversationId(String chatId, PageMeta pageMeta) {
        Assert.hasText(chatId, ID_CANNOT_BE_NULL_OR_EMPTY);

        if (!(pageMeta instanceof CursorMeta cursor)) {
            throw new IllegalArgumentException("Expected CursorMeta but got " + pageMeta.getClass().getSimpleName());
        }
        return findMessagesByChatId(chatId, cursor.pageSize(), cursor.nextCursor());
    }

    /**
     * Gets one "page" of messages for a specific chat.
     *
     * @param chatId      Maps to chat_id
     * @param pageSize    How many messages to return at once. For example, if this is 10,
     *                    Cassandra will only send back up to 10 messages in this call.
     *                    If there are more, you’ll get a token (pagingState) to fetch the next batch.
     * @param pagingState A token from a previous call that says "start where I left off".
     *                    If this is null, we start at the newest message. If it’s set,
     *                    Cassandra will skip what you’ve already seen and send the next set.
     */
    private ChatPage findMessagesByChatId(String chatId, int pageSize, String pagingState) {
        return cqlTemplate.execute((SessionCallback<ChatPage>) session -> {
            SimpleStatementBuilder builder = SimpleStatement.builder(
                            "SELECT msg_type, msg_content " +
                                    "FROM ai_chat_message " +
                                    "WHERE session_id = ?")
                    .addPositionalValue(chatId)
                    .setPageSize(pageSize);

            if (pagingState != null && !pagingState.isBlank()) {
                builder.setPagingState(ByteBuffer.wrap(Base64.getDecoder().decode(pagingState)));
            }

            ResultSet rs = session.execute(builder.build());

            int available = rs.getAvailableWithoutFetching();
            List<ChatMessageDto> items = new ArrayList<>(available);
            for (int i = 0; i < available; i++) {
                Row r = rs.one();
                items.add(new ChatMessageDto(r.getString("type"), r.getString("content")));
            }

            // next paging state as Base64
            ByteBuffer next = rs.getExecutionInfo().getPagingState();
            String nextState = getNextPagingState(next);

            return new ChatPage(items, new CursorMeta(nextState, pageSize));
        });
    }

    /**
     * Converts a Cassandra paging state ByteBuffer into a Base64 string
     * safe for transport over HTTP or JSON.
     * <p>
     * Example:
     * Suppose Cassandra returns a paging state with raw bytes:
     * [72, 101, 108, 108, 111]   // ASCII for "Hello"
     * <p>
     * Step-by-step:
     * 1. duplicate() → makes a copy so we don't alter the original buffer's position.
     * 2. new byte[dup.remaining()] → creates an array big enough for all remaining bytes.
     * In this example: length = 5 → [0, 0, 0, 0, 0]
     * 3. dup.get(bytes) → copies buffer contents into the array:
     * [72, 101, 108, 108, 111]
     * 4. Base64 encoding → turns those bytes into the string "SGVsbG8=".
     * <p>
     * Returned value ("SGVsbG8=" in this example) can be sent to the client
     * and later decoded back into the original bytes to resume paging.
     *
     * @param next The paging state from ResultSet.getExecutionInfo().getPagingState().
     * @return Base64-encoded paging state string, or null if no more pages.
     */
    private static String getNextPagingState(ByteBuffer next) {
        String nextState = null;
        if (next != null) {
            ByteBuffer dup = next.duplicate();
            byte[] bytes = new byte[dup.remaining()];
            dup.get(bytes);
            nextState = Base64.getEncoder().encodeToString(bytes);
        }
        return nextState;
    }
}
