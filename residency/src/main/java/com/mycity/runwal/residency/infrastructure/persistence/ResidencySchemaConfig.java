package com.mycity.runwal.residency.infrastructure.persistence;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * This context's own schema and migrations. Several contexts share one app (and one DataSource), so each brings its
 * own Flyway with its own history table; Spring Boot makes JPA wait until every Flyway bean has migrated.
 */
@Configuration
class ResidencySchemaConfig {

    static final String SCHEMA = "residency";

    @Bean(initMethod = "migrate")
    Flyway residencyFlyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .schemas(SCHEMA)
                .defaultSchema(SCHEMA)
                .locations("classpath:db/migration/" + SCHEMA)
                .load();
    }
}
