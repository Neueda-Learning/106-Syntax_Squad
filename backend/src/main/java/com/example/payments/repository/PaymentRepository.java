package com.example.payments.repository;

import com.example.payments.model.PaymentStatus;
import com.example.payments.model.entity.Payment;
import com.example.payments.model.entity.PaymentIntent;
import com.example.payments.model.entity.User;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class PaymentRepository {

    private static final RowMapper<Payment> PAYMENT_ROW_MAPPER = (rs, rowNum) -> mapPayment(rs);

    private final JdbcTemplate jdbcTemplate;

    public PaymentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Payment> findBySourceAccountOrderByCreatedAtDesc(String sourceAccount) {
        return queryPayments(
            """
            select id, source_account, dest_account, amount, currency, reference, status, error_code,
                   idempotency_key, created_by_user_id, payment_intent_id, version, created_at, updated_at
            from payments
            where source_account = ?
            order by created_at desc
            """,
            sourceAccount
        );
    }

    public List<Payment> findByCreatedByUser_IdOrderByCreatedAtDesc(Long createdByUserId) {
        return queryPayments(
            """
            select id, source_account, dest_account, amount, currency, reference, status, error_code,
                   idempotency_key, created_by_user_id, payment_intent_id, version, created_at, updated_at
            from payments
            where created_by_user_id = ?
            order by created_at desc
            """,
            createdByUserId
        );
    }

    public List<Payment> findBySourceAccountInOrderByCreatedAtDesc(List<String> sourceAccounts) {
        return findByAccountList(sourceAccounts, "source_account", null);
    }

    public List<Payment> findBySourceAccountAndStatusOrderByCreatedAtDesc(String sourceAccount, PaymentStatus status) {
        return queryPayments(
            """
            select id, source_account, dest_account, amount, currency, reference, status, error_code,
                   idempotency_key, created_by_user_id, payment_intent_id, version, created_at, updated_at
            from payments
            where source_account = ? and status = ?
            order by created_at desc
            """,
            sourceAccount,
            status.name()
        );
    }

    public List<Payment> findByCreatedByUser_IdAndStatusOrderByCreatedAtDesc(Long createdByUserId, PaymentStatus status) {
        return queryPayments(
            """
            select id, source_account, dest_account, amount, currency, reference, status, error_code,
                   idempotency_key, created_by_user_id, payment_intent_id, version, created_at, updated_at
            from payments
            where created_by_user_id = ? and status = ?
            order by created_at desc
            """,
            createdByUserId,
            status.name()
        );
    }

    public List<Payment> findBySourceAccountInAndStatusOrderByCreatedAtDesc(List<String> sourceAccounts, PaymentStatus status) {
        return findByAccountList(sourceAccounts, "source_account", status);
    }

    public List<Payment> findByDestAccountOrderByCreatedAtDesc(String destAccount) {
        return queryPayments(
            """
            select id, source_account, dest_account, amount, currency, reference, status, error_code,
                   idempotency_key, created_by_user_id, payment_intent_id, version, created_at, updated_at
            from payments
            where dest_account = ?
            order by created_at desc
            """,
            destAccount
        );
    }

    public List<Payment> findByDestAccountInOrderByCreatedAtDesc(List<String> destAccounts) {
        return findByAccountList(destAccounts, "dest_account", null);
    }

    public List<Payment> findByDestAccountAndStatusOrderByCreatedAtDesc(String destAccount, PaymentStatus status) {
        return queryPayments(
            """
            select id, source_account, dest_account, amount, currency, reference, status, error_code,
                   idempotency_key, created_by_user_id, payment_intent_id, version, created_at, updated_at
            from payments
            where dest_account = ? and status = ?
            order by created_at desc
            """,
            destAccount,
            status.name()
        );
    }

    public List<Payment> findByDestAccountInAndStatusOrderByCreatedAtDesc(List<String> destAccounts, PaymentStatus status) {
        return findByAccountList(destAccounts, "dest_account", status);
    }

    public Optional<Payment> findByIdempotencyKey(String idempotencyKey) {
        String sql = """
            select id, source_account, dest_account, amount, currency, reference, status, error_code,
                   idempotency_key, created_by_user_id, payment_intent_id, version, created_at, updated_at
            from payments
            where idempotency_key = ?
            """;
        return jdbcTemplate.query(sql, PAYMENT_ROW_MAPPER, idempotencyKey).stream().findFirst();
    }

    public boolean existsByIdAndSourceAccountOrIdAndDestAccount(Long id1, String sourceAccount, Long id2, String destAccount) {
        String sql = """
            select count(*)
            from payments
            where (id = ? and source_account = ?)
               or (id = ? and dest_account = ?)
            """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id1, sourceAccount, id2, destAccount);
        return count != null && count > 0;
    }

    public Optional<Payment> findById(Long id) {
        String sql = """
            select id, source_account, dest_account, amount, currency, reference, status, error_code,
                   idempotency_key, created_by_user_id, payment_intent_id, version, created_at, updated_at
            from payments
            where id = ?
            """;
        return jdbcTemplate.query(sql, PAYMENT_ROW_MAPPER, id).stream().findFirst();
    }

    public Payment save(Payment payment) {
        if (payment.getId() == null) {
            String insertSql = """
                insert into payments (
                    source_account,
                    dest_account,
                    amount,
                    currency,
                    reference,
                    status,
                    error_code,
                    idempotency_key,
                    created_by_user_id,
                    payment_intent_id,
                    version,
                    created_at,
                    updated_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(
                connection -> {
                    var statement = connection.prepareStatement(insertSql, new String[] { "id" });
                    statement.setString(1, payment.getSourceAccount());
                    statement.setString(2, payment.getDestAccount());
                    statement.setBigDecimal(3, payment.getAmount());
                    statement.setString(4, payment.getCurrency());
                    statement.setString(5, payment.getReference());
                    statement.setString(6, payment.getStatus().name());
                    statement.setString(7, payment.getErrorCode());
                    statement.setString(8, payment.getIdempotencyKey());
                    statement.setObject(9, payment.getCreatedByUser() == null ? null : payment.getCreatedByUser().getId());
                    statement.setObject(10, payment.getPaymentIntent() == null ? null : payment.getPaymentIntent().getId());
                    statement.setInt(11, payment.getVersion() == null ? 0 : payment.getVersion());
                    statement.setTimestamp(12, toTimestamp(payment.getCreatedAt()));
                    statement.setTimestamp(13, toTimestamp(payment.getUpdatedAt()));
                    return statement;
                },
                keyHolder
            );
            Number key = keyHolder.getKey();
            if (key != null) {
                payment.setId(key.longValue());
            }
            return payment;
        }

        String updateSql = """
            update payments
            set source_account = ?,
                dest_account = ?,
                amount = ?,
                currency = ?,
                reference = ?,
                status = ?,
                error_code = ?,
                idempotency_key = ?,
                created_by_user_id = ?,
                payment_intent_id = ?,
                version = ?,
                created_at = ?,
                updated_at = ?
            where id = ?
            """;
        jdbcTemplate.update(
            updateSql,
            payment.getSourceAccount(),
            payment.getDestAccount(),
            payment.getAmount(),
            payment.getCurrency(),
            payment.getReference(),
            payment.getStatus().name(),
            payment.getErrorCode(),
            payment.getIdempotencyKey(),
            payment.getCreatedByUser() == null ? null : payment.getCreatedByUser().getId(),
            payment.getPaymentIntent() == null ? null : payment.getPaymentIntent().getId(),
            payment.getVersion(),
            toTimestamp(payment.getCreatedAt()),
            toTimestamp(payment.getUpdatedAt()),
            payment.getId()
        );
        return payment;
    }

    public int compareAndSwapStatus(
        Long paymentId,
        PaymentStatus expectedStatus,
        Integer expectedVersion,
        PaymentStatus newStatus,
        String errorCode,
        LocalDateTime updatedAt
    ) {
        String sql = """
            update payments
            set status = ?,
                error_code = ?,
                updated_at = ?,
                version = version + 1
            where id = ?
              and status = ?
              and version = ?
            """;
        return jdbcTemplate.update(
            sql,
            newStatus.name(),
            errorCode,
            toTimestamp(updatedAt),
            paymentId,
            expectedStatus.name(),
            expectedVersion
        );
    }

    private List<Payment> findByAccountList(List<String> accounts, String accountColumn, PaymentStatus status) {
        if (accounts == null || accounts.isEmpty()) {
            return List.of();
        }

        String placeholders = accounts.stream().map(value -> "?").collect(Collectors.joining(","));
        StringBuilder sql = new StringBuilder(
            """
            select id, source_account, dest_account, amount, currency, reference, status, error_code,
                   idempotency_key, created_by_user_id, payment_intent_id, version, created_at, updated_at
            from payments
            where
            """
        );
        sql.append(accountColumn).append(" in (").append(placeholders).append(")");

        List<Object> params = new ArrayList<>(accounts);
        if (status != null) {
            sql.append(" and status = ?");
            params.add(status.name());
        }
        sql.append(" order by created_at desc");

        return jdbcTemplate.query(sql.toString(), PAYMENT_ROW_MAPPER, params.toArray());
    }

    private List<Payment> queryPayments(String sql, Object... params) {
        return jdbcTemplate.query(sql, PAYMENT_ROW_MAPPER, params);
    }

    private static Payment mapPayment(ResultSet rs) throws SQLException {
        Payment payment = new Payment();
        payment.setId(rs.getLong("id"));
        payment.setSourceAccount(rs.getString("source_account"));
        payment.setDestAccount(rs.getString("dest_account"));
        payment.setAmount(rs.getBigDecimal("amount"));
        payment.setCurrency(rs.getString("currency"));
        payment.setReference(rs.getString("reference"));
        payment.setStatus(PaymentStatus.valueOf(rs.getString("status")));
        payment.setErrorCode(rs.getString("error_code"));
        payment.setIdempotencyKey(rs.getString("idempotency_key"));

        Long createdByUserId = rs.getObject("created_by_user_id", Long.class);
        if (createdByUserId != null) {
            User user = new User();
            user.setId(createdByUserId);
            payment.setCreatedByUser(user);
        }

        Long paymentIntentId = rs.getObject("payment_intent_id", Long.class);
        if (paymentIntentId != null) {
            PaymentIntent intent = new PaymentIntent();
            intent.setId(paymentIntentId);
            payment.setPaymentIntent(intent);
        }

        payment.setVersion(rs.getInt("version"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        payment.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        payment.setUpdatedAt(updatedAt == null ? null : updatedAt.toLocalDateTime());
        return payment;
    }

    private static Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }
}
