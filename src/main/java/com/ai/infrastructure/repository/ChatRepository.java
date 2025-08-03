package com.ai.infrastructure.repository;

import com.ai.domain.entity.Chat;
import org.springframework.ai.chat.messages.Message;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;

@Repository
public class ChatRepository {

    private final JdbcTemplate jdbcTemplate;

    public ChatRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Persists a new Chat entity to the database.
     *
     * @param chatName the name to assign to the new chat
     * @return a {@link Chat} instance with its database-generated ID populated
     */
    public Chat save(String chatName) {
        Chat chat = new Chat(chatName);

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

    /**
     * Retrieves all chats from the database.
     *
     * @return a {@link List} containing every {@link Chat} stored in the database
     */
    public List<Chat> findAll() {
        String sql = "SELECT id, name, created_at FROM chat ORDER BY created_at DESC";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Chat chat = new Chat();
            chat.setId(rs.getString("id"));
            chat.setName(rs.getString("name"));
            chat.setCreatedAt(rs.getTimestamp("created_at").toInstant());
            return chat;
        });
    }

    /**
     * Retrieves a paginated list of chat messages for a given conversation ID.
     *
     * @param chatId the {@code conversation_id} whose messages should be fetched
     * @param page   the zero‑based page index (0 means start at the first message)
     * @param size   the maximum number of messages to return for this page
     * @return a {@link java.util.List} of {@link org.springframework.ai.chat.messages.Message}
     *         instances representing the conversation’s messages
     */
    public List<Message> findAllMessagesByChatId(String chatId, int page, int size) {
        String sql = """
        SELECT content, type, "timestamp"
        FROM SPRING_AI_CHAT_MEMORY
        WHERE conversation_id = ?
        ORDER BY "timestamp" ASC
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

}
