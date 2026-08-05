CREATE TABLE payments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    source_account VARCHAR(255) NOT NULL,
    dest_account VARCHAR(255) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    reference VARCHAR(255) NULL,
    status VARCHAR(32) NOT NULL,
    error_code VARCHAR(64) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE payment_status_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    payment_id BIGINT NOT NULL,
    from_status VARCHAR(32) NULL,
    to_status VARCHAR(32) NOT NULL,
    changed_at DATETIME NOT NULL,
    reason VARCHAR(255) NULL,
    CONSTRAINT fk_payment_status_history_payment
        FOREIGN KEY (payment_id) REFERENCES payments(id)
);
