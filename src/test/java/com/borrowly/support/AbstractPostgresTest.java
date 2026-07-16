package com.borrowly.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

// Shared base for tests that boot a Spring context. Starts one throwaway Postgres for the
// whole JVM run (same image as production) and points the datasource at it, so tests never
// touch the local dev database and don't need it started by hand. Testcontainers' reaper
// removes the container when the JVM exits.
public abstract class AbstractPostgresTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
