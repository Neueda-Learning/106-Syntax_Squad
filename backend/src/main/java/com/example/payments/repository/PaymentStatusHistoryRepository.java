package com.example.payments.repository;

import com.example.payments.model.PaymentStatus;
import com.example.payments.model.entity.Payment;
import com.example.payments.model.entity.PaymentStatusHistory;
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
public class PaymentStatusHistoryRepository {

    private static final RowMapper<PaymentStatusHistory> PAYMENT_STATUS_HISTORY_ROW_MAPPER =
        (rs, rowNum) -> mapHistory(rs);

    private final JdbcTemplate jdbcTemplate;

    public PaymentStatusHistoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<PaymentStatusHistory> findByPaymentIdOrderByChangedAtAsc(Long paymentId) {
        String sql = """
            select id, payment_id, from_status, to_status, changed_at, reason
            from payment_status_history
            where payment_id = ?
            order by changed_at asc
            """;
        return jdbcTemplate.query(sql, PAYMENT_STATUS_HISTORY_ROW_MAPPER, paymentId);
    }

    public PaymentStatusHistory save(PaymentStatusHistory history) {
        if (history.getId() == null) {
            String insertSql = """
                insert into payment_status_history (payment_id, from_status, to_status, changed_at, reason)
                values (?, ?, ?, ?, ?)
                """;
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(
                connection -> {
                    var statement = connection.prepareStatement(insertSql, new String[] { "id" });
                    statement.setLong(1, history.getPayment().getId());
                    statement.setString(2, history.getFromStatus() == null ? null : history.getFromStatus().name());
                    statement.setString(3, history.getToStatus().name());
                    statement.setTimestamp(4, toTimestamp(history.getChangedAt()));
                    statement.setString(5, history.getReason());
                    return statement;
                },
                keyHolder
            );
            Number key = keyHolder.getKey();
            if (key != null) {
                history.setId(key.longValue());
            }
            return history;
        }

        String updateSql = """
            update payment_status_history
            set payment_id = ?,
                from_status = ?,
                to_status = ?,
                changed_at = ?,
                reason = ?
            where id = ?
            """;
        jdbcTemplate.update(
            updateSql,
            history.getPayment().getId(),
            history.getFromStatus() == null ? null : history.getFromStatus().name(),
            history.getToStatus().name(),
            toTimestamp(history.getChangedAt()),
            history.getReason(),
            history.getId()
        );
        return history;
    }

    private static PaymentStatusHistory mapHistory(ResultSet rs) throws SQLException {
        PaymentStatusHistory history = new PaymentStatusHistory();
        history.setId(rs.getLong("id"));
        Payment payment = new Payment();
        payment.setId(rs.getLong("payment_id"));
        history.setPayment(payment);

        String fromStatus = rs.getString("from_status");
        history.setFromStatus(fromStatus == null ? null : PaymentStatus.valueOf(fromStatus));
        history.setToStatus(PaymentStatus.valueOf(rs.getString("to_status")));
        Timestamp changedAt = rs.getTimestamp("changed_at");
        history.setChangedAt(changedAt == null ? null : changedAt.toLocalDateTime());
        history.setReason(rs.getString("reason"));
        return history;
    }

    private static Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }
}
