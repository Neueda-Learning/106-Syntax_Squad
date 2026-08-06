package com.example.payments.repository;

import java.sql.Connection;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

/**
 * Base class providing a fresh in-memory H2 database (MySQL compatibility mode)
 * for each test method, with the production schema applied.
 */
abstract class AbstractRepositoryTest {

    protected JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpSchema() throws Exception {
        SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
        dataSource.setDriverClass(org.h2.Driver.class);
        dataSource.setUrl("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");

        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("schema-h2.sql"));
        }

        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    protected DataSource dataSource() {
        return jdbcTemplate.getDataSource();
    }
}
