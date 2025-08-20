package com.ai.infrastructure.config;

import com.datastax.oss.driver.api.core.CqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.core.cql.CqlTemplate;

import java.net.InetSocketAddress;

@Configuration
@ConditionalOnProperty(
        value = "app.cassandra.enabled",
        havingValue = "true"
)
public class CassandraSchemaV1 {

    private static final Logger log = LoggerFactory.getLogger(CassandraSchemaV1.class);

    @Bean(destroyMethod = "close")
    CqlSession cqlSession(
            @Value("${spring.cassandra.contact-points}") String host,
            @Value("${spring.cassandra.port}") int port,
            @Value("${spring.cassandra.local-datacenter}") String dc,
            @Value("${spring.cassandra.keyspace-name}") String keyspaceName,
            @Value("${spring.cassandra.username}") String username,
            @Value("${spring.cassandra.password}") String password
    ) {
        // bootstrap session (no keyspace) → create keyspace
        try (CqlSession bootstrap = CqlSession.builder()
                .addContactPoint(new InetSocketAddress(host, port))
                .withLocalDatacenter(dc)
                .build()) {
            bootstrap.execute("""
                        CREATE KEYSPACE IF NOT EXISTS spring_ai
                        WITH REPLICATION = {'class':'SimpleStrategy','replication_factor':1}
                    """);
        }
        // main session bound to keyspace
        return CqlSession.builder()
                .addContactPoint(new InetSocketAddress(host, port))
                .withLocalDatacenter(dc)
                .withKeyspace(keyspaceName)
                .withAuthCredentials(username, password)
                .build();
    }

    @Bean
    CommandLineRunner cassandraSchema(CqlTemplate cqlTemplate) {
        return args -> {
            log.info("Creating table ai_chat_message if not exists");
            cqlTemplate.execute("""
                      CREATE TABLE IF NOT EXISTS ai_chat_message(
                        session_id text, msg_timestamp timestamp, msg_type text, msg_content text,
                        PRIMARY KEY ((session_id), msg_timestamp)
                      ) WITH CLUSTERING ORDER BY (msg_timestamp DESC)
                    """);

            log.info("Creating table ai_chat_memory if not exists");
            cqlTemplate.execute("""
                      CREATE TABLE IF NOT EXISTS ai_chat_memory(
                        session_id text PRIMARY KEY, session_name text, created_at timestamp
                      )
                    """);

            log.info("Creating table chats_by_created if not exists");
            cqlTemplate.execute("""
                      CREATE TABLE IF NOT EXISTS chats_by_created(
                        bucket text, created_at timeuuid, session_id text, session_name text,
                        PRIMARY KEY ((bucket), created_at, session_id)
                      ) WITH CLUSTERING ORDER BY (created_at DESC)
                    """);

            log.info("Cassandra schema initialization complete");
        };
    }
}
