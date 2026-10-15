package com.kafe.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

// Databases created before Firebase Authentication have NOT NULL password_hash and
// phone_number columns. ddl-auto=update never relaxes constraints, and Firebase users
// have neither value, so make them nullable once.
@Component
@Order(0)
public class LegacySchemaFix implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(LegacySchemaFix.class);

    private final JdbcTemplate jdbcTemplate;

    public LegacySchemaFix(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        try {
            List<String> columns = jdbcTemplate.queryForList(
                    "SELECT column_name FROM information_schema.columns " +
                    "WHERE table_schema = DATABASE() AND table_name = 'users' " +
                    "AND column_name IN ('password_hash', 'phone_number') AND is_nullable = 'NO'",
                    String.class);
            for (String column : columns) {
                // Column names come from the fixed list above, not from user input
                jdbcTemplate.execute("ALTER TABLE users MODIFY " + column + " VARCHAR(255) NULL");
                log.info("Made users.{} nullable for Firebase Authentication", column);
            }
        } catch (Exception e) {
            log.warn("Could not check legacy users columns: {}", e.getMessage());
        }
    }
}
