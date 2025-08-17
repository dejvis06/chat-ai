package com.ai.config;

import com.datastax.oss.driver.api.core.CqlSession;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.cassandra.CassandraContainer;
import org.testcontainers.utility.DockerImageName;

import java.net.InetSocketAddress;

@TestConfiguration(proxyBeanMethods = false)
public class CassandraTestConfig {

    public static final String CASSANDRA_V5_0_5 = "cassandra:5.0.5";
    private static final String KEYSPACE = "spring_ai";

    @Bean
    @ServiceConnection
    CassandraContainer cassandraContainer() {
        return new CassandraContainer(DockerImageName.parse(CASSANDRA_V5_0_5))
                .withInitScript("schema.cql");
    }

    @Bean
    CqlSession session(CassandraContainer container) {
        return CqlSession.builder()
                .addContactPoint(new InetSocketAddress(container.getHost(), container.getFirstMappedPort()))
                .withLocalDatacenter("datacenter1")
                .withKeyspace(KEYSPACE)
                .build();
    }
}
