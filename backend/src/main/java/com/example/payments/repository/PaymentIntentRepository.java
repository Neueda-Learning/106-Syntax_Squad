package com.example.payments.repository;

import com.example.payments.model.PaymentIntentStatus;
import com.example.payments.model.entity.PaymentIntent;
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
public class PaymentIntentRepository {

    private static final RowMapper<PaymentIntent> PAYMENT_INTENT_ROW_MAPPER = (rs, rowNum) -> mapPaymentIntent(rs);

    private final JdbcTemplate jdbcTemplate;

    public PaymentIntentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<PaymentIntent> findByIdAndInitiatedByUser_Id(Long id, Long initiatedByUserId) {
        String sql = """
            select id, idempotency_key, initiated_by_user_id, payee_account_number, status, created_at
            from payment_intents
            where id = ? and initiated_by_user_id = ?
            """;
        return jdbcTemplate.query(sql, PAYMENT_INTENT_ROW_MAPPER, id, initiatedByUserId).stream().findFirst();
    }

    public Optional<PaymentIntent> findById(Long id) {
        String sql = """
            select id, idempotency_key, initiated_by_user_id, payee_account_number, status, created_at
            from payment_intents
            where id = ?
            """;
        return jdbcTemplate.query(sql, PAYMENT_INTENT_ROW_MAPPER, id).stream().findFirst();
    }

    public PaymentIntent save(PaymentIntent intent) {
        if (intent.getId() == null) {
            String insertSql = """
                insert into payment_intents (idempotency_key, initiated_by_user_id, payee_account_number, status, created_at)
                values (?, ?, ?, ?, ?)
                """;
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(
                connection -> {
                    var statement = connection.prepareStatement(insertSql, new String[] { "id" });
                    statement.setString(1, intent.getIdempotencyKey());
                    statement.setLong(2, intent.getInitiatedByUser().getId());
                    statement.setString(3, intent.getPayeeAccountNumber());
                    statement.setString(4, intent.getStatus().name());
                    statement.setTimestamp(5, toTimestamp(intent.getCreatedAt()));
                    return statement;
                },
                keyHolder
            );
            Number key = keyHolder.getKey();
            if (key != null) {
                intent.setId(key.longValue());
            }
            return intent;
        }

        String updateSql = """
            update payment_intents
            set idempotency_key = ?,
                initiated_by_user_id = ?,
                payee_account_number = ?,
                status = ?,
                created_at = ?
            where id = ?
            """;
        jdbcTemplate.update(
            updateSql,
            intent.getIdempotencyKey(),
            intent.getInitiatedByUser().getId(),
            intent.getPayeeAccountNumber(),
            intent.getStatus().name(),
            toTimestamp(intent.getCreatedAt()),
            intent.getId()
        );
        return intent;
    }

    private static PaymentIntent mapPaymentIntent(ResultSet rs) throws SQLException {
        PaymentIntent intent = new PaymentIntent();
        intent.setId(rs.getLong("id"));
        intent.setIdempotencyKey(rs.getString("idempotency_key"));
        User user = new User();
        user.setId(rs.getLong("initiated_by_user_id"));
        intent.setInitiatedByUser(user);
        intent.setPayeeAccountNumber(rs.getString("payee_account_number"));
        intent.setStatus(PaymentIntentStatus.valueOf(rs.getString("status")));
        Timestamp createdAt = rs.getTimestamp("created_at");
        intent.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
        return intent;
    }

    private static Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }
}
