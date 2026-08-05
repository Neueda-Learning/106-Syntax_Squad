CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE accounts (
    account_number VARCHAR(20) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    display_name VARCHAR(255),
    balance DECIMAL(15,2) NOT NULL DEFAULT 50000.00,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_accounts_user FOREIGN KEY (user_id) REFERENCES users(id)
);

ALTER TABLE payments
    ADD COLUMN idempotency_key VARCHAR(64) UNIQUE,
    ADD COLUMN created_by_user_id BIGINT,
    ADD COLUMN version INT NOT NULL DEFAULT 0,
    ADD COLUMN payment_intent_id BIGINT,
    ADD CONSTRAINT fk_payments_created_by_user FOREIGN KEY (created_by_user_id) REFERENCES users(id);

CREATE TABLE payment_send_attempts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id BIGINT NOT NULL,
    attempt_number INT NOT NULL,
    outcome VARCHAR(20) NOT NULL,
    attempted_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_send_attempt_payment FOREIGN KEY (payment_id) REFERENCES payments(id)
);

CREATE TABLE payees (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_user_id BIGINT NOT NULL,
    payee_account_number VARCHAR(20) NOT NULL,
    nickname VARCHAR(255) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payees_owner FOREIGN KEY (owner_user_id) REFERENCES users(id),
    CONSTRAINT fk_payees_account FOREIGN KEY (payee_account_number) REFERENCES accounts(account_number),
    CONSTRAINT uk_payee_owner_account UNIQUE (owner_user_id, payee_account_number)
);

CREATE TABLE payment_intents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    idempotency_key VARCHAR(64) UNIQUE NOT NULL,
    initiated_by_user_id BIGINT NOT NULL,
    payee_account_number VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payment_intents_user FOREIGN KEY (initiated_by_user_id) REFERENCES users(id),
    CONSTRAINT fk_payment_intents_account FOREIGN KEY (payee_account_number) REFERENCES accounts(account_number)
);

ALTER TABLE payments
    ADD CONSTRAINT fk_payments_intent FOREIGN KEY (payment_intent_id) REFERENCES payment_intents(id);
