package com.example.payments.repository;

import com.example.payments.model.entity.User;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {

    private static final RowMapper<User> USER_ROW_MAPPER = (rs, rowNum) -> mapUser(rs);

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<User> findByEmail(String email) {
        String sql = """
            select id, email, password_hash, full_name, created_at
            from users
            where email = ?
            """;
        return jdbcTemplate.query(sql, USER_ROW_MAPPER, email).stream().findFirst();
    }

    public Optional<User> findById(Long id) {
        String sql = """
            select id, email, password_hash, full_name, created_at
            from users
            where id = ?
            """;
        return jdbcTemplate.query(sql, USER_ROW_MAPPER, id).stream().findFirst();
    }

    public boolean existsByEmail(String email) {
        String sql = "select count(*) from users where email = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, email);
        return count != null && count > 0;
    }

    public User save(User user) {
        if (user.getId() == null) {
            String insertSql = """
                insert into users (email, password_hash, full_name, created_at)
                values (?, ?, ?, ?)
                """;
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(
                connection -> {
                    var statement = connection.prepareStatement(insertSql, new String[] { "id" });
                    statement.setString(1, user.getEmail());
                    statement.setString(2, user.getPasswordHash());
                    statement.setString(3, user.getFullName());
                    statement.setTimestamp(4, toTimestamp(user.getCreatedAt()));
                    return statement;
                },
                keyHolder
            );
            Number key = keyHolder.getKey();
            if (key != null) {
                user.setId(key.longValue());
            }
            return user;
        }

        String updateSql = """
            update users
            set email = ?,
                password_hash = ?,
                full_name = ?,
                created_at = ?
            where id = ?
            """;
        jdbcTemplate.update(
            updateSql,
            user.getEmail(),
            user.getPasswordHash(),
            toTimestamp(user.getCreatedAt()),
            user.getId()
        );
        return user;
    }

    public User getReferenceById(Long id) {
        return findById(id).orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }

    private static User mapUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setFullName(rs.getString("full_name"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        user.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
        return user;
    }

    private static Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }
}
