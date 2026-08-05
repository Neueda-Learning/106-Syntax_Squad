package com.example.payments.repository;

import com.example.payments.model.entity.Payee;
import com.example.payments.model.entity.User;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class PayeeRepository {

    private static final RowMapper<Payee> PAYEE_ROW_MAPPER = (rs, rowNum) -> mapPayee(rs);

    private final JdbcTemplate jdbcTemplate;

    public PayeeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Payee> findByOwnerUser_IdOrderByCreatedAtDesc(Long ownerUserId) {
        String sql = """
            select id, owner_user_id, payee_account_number, nickname, created_at
            from payees
            where owner_user_id = ?
            order by created_at desc
            """;
        return jdbcTemplate.query(sql, PAYEE_ROW_MAPPER, ownerUserId);
    }

    public Optional<Payee> findByOwnerUser_IdAndPayeeAccountNumber(Long ownerUserId, String payeeAccountNumber) {
        String sql = """
            select id, owner_user_id, payee_account_number, nickname, created_at
            from payees
            where owner_user_id = ? and payee_account_number = ?
            """;
        return jdbcTemplate.query(sql, PAYEE_ROW_MAPPER, ownerUserId, payeeAccountNumber).stream().findFirst();
    }

    public Optional<Payee> findByIdAndOwnerUser_Id(Long id, Long ownerUserId) {
        String sql = """
            select id, owner_user_id, payee_account_number, nickname, created_at
            from payees
            where id = ? and owner_user_id = ?
            """;
        return jdbcTemplate.query(sql, PAYEE_ROW_MAPPER, id, ownerUserId).stream().findFirst();
    }

    public Optional<Payee> findById(Long id) {
        String sql = """
            select id, owner_user_id, payee_account_number, nickname, created_at
            from payees
            where id = ?
            """;
        return jdbcTemplate.query(sql, PAYEE_ROW_MAPPER, id).stream().findFirst();
    }

    public Payee save(Payee payee) {
        if (payee.getId() == null) {
            String insertSql = """
                insert into payees (owner_user_id, payee_account_number, nickname, created_at)
                values (?, ?, ?, ?)
                """;
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(
                connection -> {
                    var statement = connection.prepareStatement(insertSql, new String[] { "id" });
                    statement.setLong(1, payee.getOwnerUser().getId());
                    statement.setString(2, payee.getPayeeAccountNumber());
                    statement.setString(3, payee.getNickname());
                    statement.setTimestamp(4, toTimestamp(payee.getCreatedAt()));
                    return statement;
                },
                keyHolder
            );
            Number key = keyHolder.getKey();
            if (key != null) {
                payee.setId(key.longValue());
            }
            return payee;
        }

        String updateSql = """
            update payees
            set owner_user_id = ?,
                payee_account_number = ?,
                nickname = ?,
                created_at = ?
            where id = ?
            """;
        jdbcTemplate.update(
            updateSql,
            payee.getOwnerUser().getId(),
            payee.getPayeeAccountNumber(),
            payee.getNickname(),
            toTimestamp(payee.getCreatedAt()),
            payee.getId()
        );
        return payee;
    }

    public void delete(Payee payee) {
        jdbcTemplate.update("delete from payees where id = ?", payee.getId());
    }

    private static Payee mapPayee(ResultSet rs) throws SQLException {
        Payee payee = new Payee();
        payee.setId(rs.getLong("id"));
        User owner = new User();
        owner.setId(rs.getLong("owner_user_id"));
        payee.setOwnerUser(owner);
        payee.setPayeeAccountNumber(rs.getString("payee_account_number"));
        payee.setNickname(rs.getString("nickname"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        payee.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
        return payee;
    }

    private static Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }
}
