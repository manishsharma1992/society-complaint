package com.mycity.runwal.feed.infrastructure.persistence;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** This context's own schema and migrations, with its own Flyway history table (see the other contexts). */
@Configuration
class FeedSchemaConfig {

    static final String SCHEMA = "feed";

    @Bean(initMethod = "migrate")
    Flyway feedFlyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .schemas(SCHEMA)
                .defaultSchema(SCHEMA)
                .locations("classpath:db/migration/" + SCHEMA)
                .load();
    }
}
