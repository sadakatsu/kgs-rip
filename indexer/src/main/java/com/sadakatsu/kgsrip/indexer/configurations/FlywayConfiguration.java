package com.sadakatsu.kgsrip.indexer.configurations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfiguration {
    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy(@Value("${indexer.clean_database:#{false}}") boolean clean) {
        return flyway -> {
            if (clean) {
                flyway.clean();
            }
            flyway.repair();
            flyway.migrate();
        };
    }
}
