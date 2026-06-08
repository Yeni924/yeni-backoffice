ALTER TABLE sales_transaction
    ADD COLUMN IF NOT EXISTS payment_id BIGINT;

ALTER TABLE sales_transaction
    ADD COLUMN IF NOT EXISTS cancel_id BIGINT;

ALTER TABLE sales_transaction
    ADD COLUMN IF NOT EXISTS original_sales_transaction_id BIGINT;

ALTER TABLE sales_transaction
    ADD COLUMN IF NOT EXISTS pg_transaction_id VARCHAR(120);

ALTER TABLE sales_transaction
    ADD COLUMN IF NOT EXISTS supply_amount NUMERIC(19, 2) DEFAULT 0 NOT NULL;

ALTER TABLE sales_transaction
    ADD COLUMN IF NOT EXISTS total_amount NUMERIC(19, 2) DEFAULT 0 NOT NULL;

ALTER TABLE sales_transaction
    ADD COLUMN IF NOT EXISTS ledger_status VARCHAR(30) DEFAULT 'POSTED' NOT NULL;

ALTER TABLE sales_transaction
    ADD COLUMN IF NOT EXISTS settlement_status VARCHAR(30) DEFAULT 'NOT_SETTLED' NOT NULL;

ALTER TABLE sales_transaction
    ADD COLUMN IF NOT EXISTS pg_code VARCHAR(40);

ALTER TABLE sales_transaction
    ADD COLUMN IF NOT EXISTS payment_method VARCHAR(40);

ALTER TABLE sales_transaction
    ADD COLUMN IF NOT EXISTS seller_id BIGINT;

ALTER TABLE sales_transaction
    ADD COLUMN IF NOT EXISTS order_item_id BIGINT;

UPDATE sales_transaction
SET pg_transaction_id = tid
WHERE pg_transaction_id IS NULL;

UPDATE sales_transaction
SET total_amount = sale_amount
WHERE total_amount = 0;

UPDATE sales_transaction
SET supply_amount = ROUND(total_amount * 10 / 11, 0),
    vat_amount = total_amount - ROUND(total_amount * 10 / 11, 0)
WHERE supply_amount = 0;
