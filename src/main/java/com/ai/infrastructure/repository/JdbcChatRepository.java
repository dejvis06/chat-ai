package com.ai.infrastructure.repository;

import com.ai.application.dto.ChatMessageDto;
import com.ai.domain.entity.SqlChat;
import com.ai.domain.model.pagination.ChatPage;
import com.ai.domain.model.pagination.OffsetMeta;
import com.ai.domain.model.pagination.PageMeta;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;

public class JdbcChatRepository extends ChatRepository<SqlChat, String> {

    private final JdbcTemplate jdbcTemplate;

    public JdbcChatRepository(JdbcTemplate jdbcTemplate, ChatMemoryRepository chatMemoryRepository, int maxMessages) {
        super(chatMemoryRepository, maxMessages);
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public SqlChat save(String chatName) {
        SqlChat chat = new SqlChat(chatName);

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO chat (name, created_at) VALUES (?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, chatName);
            ps.setTimestamp(2, Timestamp.from(chat.getCreatedAt()));
            return ps;
        }, keyHolder);

        chat.setId(keyHolder.getKey().toString());
        return chat;
    }

    @Override
    public List<SqlChat> findAll() {
        String sql = "SELECT id, name, created_at FROM chat ORDER BY created_at DESC";

        return jdbcTemplate.query(sql, (rs, rowNum) -> new SqlChat(
                rs.getString("id"),
                rs.getString("name"),
                rs.getTimestamp("created_at").toInstant()
        ));
    }

    @Override
    public List<Message> getMaxMessages(String conversationId) {
        throw new UnsupportedOperationException("getMaxMessages is not supported");
    }

    @Override
    public ChatPage findMessagesByChatId(String id, PageMeta pageMeta) {
        if (!(pageMeta instanceof OffsetMeta offsetMeta)) {
            throw new IllegalArgumentException("Expected OffsetMeta but got " + pageMeta.getClass().getSimpleName());
        }
        int page = offsetMeta.nextPage();
        int size = offsetMeta.pageSize();

        List<ChatMessageDto> fetched = this.findMessagesByChatId(id, page, size)
                .stream()
                .map(ChatMessageDto::from)
                .toList();

        boolean hasNext = fetched.size() == size;
        PageMeta nextPage = hasNext ? new OffsetMeta(++page, size, hasNext) : null;

        return new ChatPage(fetched, nextPage);
    }

    public List<Message> findMessagesByChatId(String chatId, int page, int size) {
        String sql = """
                SELECT content, type, "timestamp"
                FROM SPRING_AI_CHAT_MEMORY
                WHERE conversation_id = ?
                ORDER BY "timestamp" DESC
                LIMIT ? OFFSET ?
                """;

        int offset = page * size;

        return jdbcTemplate.query(sql, new Object[]{chatId, size, offset}, (rs, rowNum) -> {
            String type = rs.getString("type");
            String content = rs.getString("content");

            // Map DB type to the correct Message implementation
            return switch (type) {
                case "USER" -> new org.springframework.ai.chat.messages.UserMessage(content);
                case "ASSISTANT" -> new org.springframework.ai.chat.messages.AssistantMessage(content);
                default -> throw new IllegalArgumentException("Unknown message type: " + type);
            };
        });
    }

    public static final class Builder {
        private ChatMemoryRepository chatMemoryRepository;
        private int maxMessages = 20;
        private JdbcTemplate jdbcTemplate;

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

        public Builder cqlTemplate(JdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
            return this;
        }

        public JdbcChatRepository build() {
            if (this.chatMemoryRepository == null) {
                throw new IllegalStateException("ChatMemoryRepository is not configured");
            }
            if (this.jdbcTemplate == null) {
                throw new IllegalStateException("JdbcTemplate is not configured");
            }
            return new JdbcChatRepository(this.jdbcTemplate, this.chatMemoryRepository, this.maxMessages);
        }
    }
}
