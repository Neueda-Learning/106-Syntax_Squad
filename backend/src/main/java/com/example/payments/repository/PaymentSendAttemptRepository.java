package com.example.payments.repository;

import com.example.payments.model.SendAttemptOutcome;
import com.example.payments.model.entity.Payment;
import com.example.payments.model.entity.PaymentSendAttempt;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class PaymentSendAttemptRepository {

    private static final RowMapper<PaymentSendAttempt> PAYMENT_SEND_ATTEMPT_ROW_MAPPER =
        (rs, rowNum) -> mapAttempt(rs);

    private final JdbcTemplate jdbcTemplate;

    public PaymentSendAttemptRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<PaymentSendAttempt> findByPaymentIdOrderByAttemptNumberAsc(Long paymentId) {
        String sql = """
            select id, payment_id, attempt_number, outcome, attempted_at
            from payment_send_attempts
            where payment_id = ?
            order by attempt_number asc
            """;
        return jdbcTemplate.query(sql, PAYMENT_SEND_ATTEMPT_ROW_MAPPER, paymentId);
    }

    public PaymentSendAttempt save(PaymentSendAttempt attempt) {
        if (attempt.getId() == null) {
            String insertSql = """
                insert into payment_send_attempts (payment_id, attempt_number, outcome, attempted_at)
                values (?, ?, ?, ?)
                """;
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(
                connection -> {
                    var statement = connection.prepareStatement(insertSql, new String[] { "id" });
                    statement.setLong(1, attempt.getPayment().getId());
                    statement.setInt(2, attempt.getAttemptNumber());
                    statement.setString(3, attempt.getOutcome().name());
                    statement.setTimestamp(4, toTimestamp(attempt.getAttemptedAt()));
                    return statement;
                },
                keyHolder
            );
            Number key = keyHolder.getKey();
            if (key != null) {
                attempt.setId(key.longValue());
            }
            return attempt;
        }

        String updateSql = """
            update payment_send_attempts
            set payment_id = ?,
                attempt_number = ?,
                outcome = ?,
                attempted_at = ?
            where id = ?
            """;
        jdbcTemplate.update(
            updateSql,
            attempt.getPayment().getId(),
            attempt.getAttemptNumber(),
            attempt.getOutcome().name(),
            toTimestamp(attempt.getAttemptedAt()),
            attempt.getId()
        );
        return attempt;
    }

    private static PaymentSendAttempt mapAttempt(ResultSet rs) throws SQLException {
        PaymentSendAttempt attempt = new PaymentSendAttempt();
        attempt.setId(rs.getLong("id"));
        Payment payment = new Payment();
        payment.setId(rs.getLong("payment_id"));
        attempt.setPayment(payment);
        attempt.setAttemptNumber(rs.getInt("attempt_number"));
        attempt.setOutcome(SendAttemptOutcome.valueOf(rs.getString("outcome")));
        Timestamp attemptedAt = rs.getTimestamp("attempted_at");
        attempt.setAttemptedAt(attemptedAt == null ? null : attemptedAt.toLocalDateTime());
        return attempt;
    }

    private static Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }
}
