package com.ai.infrastructure.repository;

import com.ai.application.dto.ChatMessageDto;
import com.ai.domain.entity.NoSqlChat;
import com.ai.domain.model.pagination.ChatPage;
import com.ai.domain.model.pagination.CursorMeta;
import com.ai.domain.model.pagination.PageMeta;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.core.cql.SimpleStatementBuilder;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.data.cassandra.core.cql.CqlTemplate;
import org.springframework.data.cassandra.core.cql.SessionCallback;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class CassandraChatRepository extends ChatRepository<NoSqlChat, String> {

    private static final Logger log = LoggerFactory.getLogger(CassandraChatRepository.class);

    private final CqlTemplate cqlTemplate;

    private CassandraChatRepository(CqlTemplate cqlTemplate, ChatMemoryRepository chatMemoryRepository, int maxMessages) {
        super(chatMemoryRepository, maxMessages);
        this.cqlTemplate = cqlTemplate;
    }

    @PostConstruct
    public void initSchema() {
        log.info("Initializing Cassandra schema for AI chat memory...");

        // Create keyspace if not exists
        cqlTemplate.execute("""
                    CREATE KEYSPACE IF NOT EXISTS spring
                    WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1}
                """);

        // Create UDT
        cqlTemplate.execute("""
                    CREATE TYPE IF NOT EXISTS spring.ai_chat_message (
                        msg_timestamp timestamp,
                        msg_type text,
                        msg_content text
                    )
                """);

        // Create table
        cqlTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS spring.ai_chat_memory (
                        session_id text,
                        message_timestamp timestamp,
                        messages list<ai_chat_message>,
                        PRIMARY KEY (session_id, message_timestamp)
                    ) WITH CLUSTERING ORDER BY (message_timestamp DESC)
                      AND compaction = { 'class' : 'UnifiedCompactionStrategy' }
                """);

        log.info("Cassandra schema initialization complete.");
    }

    @Override
    public NoSqlChat save(String chatName) {
        NoSqlChat noSqlChat = new NoSqlChat(chatName);

        cqlTemplate.execute(
                "INSERT INTO spring.ai_chat_memory (session_id, message_timestamp, messages) VALUES (?, ?, ?)",
                noSqlChat.getId(),
                noSqlChat.getCreatedAt(),
                null
        );

        return noSqlChat;
    }


    @Override
    public List<Message> getMaxMessages(String conversationId) {
        throw new UnsupportedOperationException("getMaxMessages is not supported");
    }


    @Override
    public List<NoSqlChat> findAll() {
        String cqlQuery = """
                    SELECT session_id, message_timestamp
                    FROM spring.ai_chat_memory
                """;

        return cqlTemplate.query(cqlQuery, (row, rowNum) -> new NoSqlChat(
                row.getString("session_id"),
                row.getInstant("message_timestamp")
        ));
    }

    @Override
    public ChatPage findMessagesByChatId(String chatId, PageMeta pageMeta) {
        if (!(pageMeta instanceof CursorMeta cursor)) {
            throw new IllegalArgumentException("Expected CursorMeta but got " + pageMeta.getClass().getSimpleName());
        }
        return findMessagesByChatId(chatId, cursor.pageSize(), cursor.nextCursor());
    }

    /**
     * Gets one "page" of messages for a specific chat.
     *
     * @param chatId      Maps to session_id
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
                            // add ORDER BY if you have a clustering column (e.g., ts DESC)
                            "SELECT type, content FROM chat_messages WHERE session_id = ?")
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

    public static Builder builder() {
        return new CassandraChatRepository.Builder();
    }

    public static final class Builder {
        private ChatMemoryRepository chatMemoryRepository;
        private int maxMessages = 20;
        private CqlTemplate cqlTemplate;

        private Builder() {
        }

        public Builder chatMemoryRepository(ChatMemoryRepository chatMemoryRepository) {
            this.chatMemoryRepository = chatMemoryRepository;
            return this;
        }

        public Builder maxMessages(int maxMessages) {
            this.maxMessages = maxMessages;
            return this;
        }

        public Builder cqlTemplate(CqlTemplate cqlTemplate) {
            this.cqlTemplate = cqlTemplate;
            return this;
        }

        public CassandraChatRepository build() {
            if (this.chatMemoryRepository == null) {
                throw new IllegalStateException("ChatMemoryRepository is not configured");
            }
            if (this.cqlTemplate == null) {
                throw new IllegalStateException("CqlTemplate is not configured");
            }
            return new CassandraChatRepository(this.cqlTemplate, this.chatMemoryRepository, this.maxMessages);
        }
    }
}
