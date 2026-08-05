package com.example.payments.repository;

import com.example.payments.model.entity.Account;
import com.example.payments.model.entity.User;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class AccountRepository {

    private static final RowMapper<Account> ACCOUNT_ROW_MAPPER = (rs, rowNum) -> mapAccount(rs);

    private final JdbcTemplate jdbcTemplate;

    public AccountRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean existsByAccountNumberAndUser_Id(String accountNumber, Long userId) {
        String sql = "select count(*) from accounts where account_number = ? and user_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, accountNumber, userId);
        return count != null && count > 0;
    }

    public boolean existsById(String accountNumber) {
        String sql = "select count(*) from accounts where account_number = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, accountNumber);
        return count != null && count > 0;
    }

    public List<Account> findByUser_IdOrderByAccountNumberAsc(Long userId) {
        String sql = """
            select account_number, user_id, display_name, balance, created_at
            from accounts
            where user_id = ?
            order by account_number asc
            """;
        return jdbcTemplate.query(sql, ACCOUNT_ROW_MAPPER, userId);
    }

    public Optional<Account> findById(String accountNumber) {
        String sql = """
            select account_number, user_id, display_name, balance, created_at
            from accounts
            where account_number = ?
            """;
        return jdbcTemplate.query(sql, ACCOUNT_ROW_MAPPER, accountNumber).stream().findFirst();
    }

    public Account save(Account account) {
        String upsertSql = """
            insert into accounts (account_number, user_id, display_name, balance, created_at)
            values (?, ?, ?, ?, ?)
            on duplicate key update
                user_id = values(user_id),
                display_name = values(display_name),
                balance = values(balance),
                created_at = values(created_at)
            """;
        jdbcTemplate.update(
            upsertSql,
            account.getAccountNumber(),
            account.getUser().getId(),
            account.getDisplayName(),
            account.getBalance(),
            toTimestamp(account.getCreatedAt())
        );
        return account;
    }

    private static Account mapAccount(ResultSet rs) throws SQLException {
        Account account = new Account();
        account.setAccountNumber(rs.getString("account_number"));
        User user = new User();
        user.setId(rs.getLong("user_id"));
        account.setUser(user);
        account.setDisplayName(rs.getString("display_name"));
        account.setBalance(rs.getBigDecimal("balance"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        account.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
        return account;
    }

    private static Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }
}
