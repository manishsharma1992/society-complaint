package com.mycity.runwal.workforce.infrastructure.persistence;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** This context's own schema and migrations, with its own Flyway history table (see the other contexts). */
@Configuration
class WorkforceSchemaConfig {

    static final String SCHEMA = "workforce";

    @Bean(initMethod = "migrate")
    Flyway workforceFlyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .schemas(SCHEMA)
                .defaultSchema(SCHEMA)
                .locations("classpath:db/migration/" + SCHEMA)
                .load();
    }
}
