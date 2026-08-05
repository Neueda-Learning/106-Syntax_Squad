-- Simulation mode: allow payees and intents that reference accounts not yet present.
-- Keep ownership/user foreign keys; only relax payee-account existence constraints.

SET @payees_fk := (
    SELECT CONSTRAINT_NAME
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'payees'
      AND CONSTRAINT_TYPE = 'FOREIGN KEY'
      AND CONSTRAINT_NAME = 'fk_payees_account'
    LIMIT 1
);

SET @drop_payees_fk_sql := IF(
    @payees_fk IS NULL,
    'SELECT 1',
    'ALTER TABLE payees DROP FOREIGN KEY fk_payees_account'
);

PREPARE drop_payees_fk_stmt FROM @drop_payees_fk_sql;
EXECUTE drop_payees_fk_stmt;
DEALLOCATE PREPARE drop_payees_fk_stmt;

SET @intents_fk := (
    SELECT CONSTRAINT_NAME
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'payment_intents'
      AND CONSTRAINT_TYPE = 'FOREIGN KEY'
      AND CONSTRAINT_NAME = 'fk_payment_intents_account'
    LIMIT 1
);

SET @drop_intents_fk_sql := IF(
    @intents_fk IS NULL,
    'SELECT 1',
    'ALTER TABLE payment_intents DROP FOREIGN KEY fk_payment_intents_account'
);

PREPARE drop_intents_fk_stmt FROM @drop_intents_fk_sql;
EXECUTE drop_intents_fk_stmt;
DEALLOCATE PREPARE drop_intents_fk_stmt;