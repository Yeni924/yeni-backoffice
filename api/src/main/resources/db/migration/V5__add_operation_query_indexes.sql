CREATE INDEX IF NOT EXISTS ix_sales_transaction_business_date
    ON sales_transaction (business_date);

CREATE INDEX IF NOT EXISTS ix_sales_transaction_occurred_at
    ON sales_transaction (occurred_at);

CREATE INDEX IF NOT EXISTS ix_sales_transaction_order_no
    ON sales_transaction (order_no);

CREATE INDEX IF NOT EXISTS ix_sales_transaction_tid
    ON sales_transaction (tid);

CREATE INDEX IF NOT EXISTS ix_sales_transaction_pg_transaction_id
    ON sales_transaction (pg_transaction_id);

CREATE INDEX IF NOT EXISTS ix_sales_transaction_settlement_status
    ON sales_transaction (settlement_status);

CREATE INDEX IF NOT EXISTS ix_sales_transaction_sale_type
    ON sales_transaction (sale_type);

CREATE INDEX IF NOT EXISTS ix_sales_transaction_payment_id
    ON sales_transaction (payment_id);

CREATE INDEX IF NOT EXISTS ix_sales_transaction_cancel_id
    ON sales_transaction (cancel_id);

CREATE INDEX IF NOT EXISTS ix_sales_transaction_original_sales_id
    ON sales_transaction (original_sales_transaction_id);

CREATE INDEX IF NOT EXISTS ix_sales_transaction_date_type_status
    ON sales_transaction (business_date, sale_type, settlement_status);

CREATE INDEX IF NOT EXISTS ix_sales_transaction_date_occurred_at
    ON sales_transaction (business_date, occurred_at);

CREATE INDEX IF NOT EXISTS ix_payment_transaction_status_approved_at
    ON payment_transaction (payment_status, approved_at);

CREATE INDEX IF NOT EXISTS ix_payment_cancel_payment_id
    ON payment_cancel (payment_id);

CREATE INDEX IF NOT EXISTS ix_payment_cancel_status_canceled_at
    ON payment_cancel (cancel_status, canceled_at);

CREATE INDEX IF NOT EXISTS ix_external_send_request_sales_id
    ON external_send_request (sales_id);

CREATE INDEX IF NOT EXISTS ix_external_send_request_status_created_at
    ON external_send_request (send_status, created_at);

CREATE INDEX IF NOT EXISTS ix_alimtalk_queue_sales_id
    ON alimtalk_queue (sales_id);

CREATE INDEX IF NOT EXISTS ix_alimtalk_queue_payment_id
    ON alimtalk_queue (payment_id);

CREATE INDEX IF NOT EXISTS ix_alimtalk_queue_status_created_at
    ON alimtalk_queue (status, created_at);

CREATE INDEX IF NOT EXISTS ix_payment_recovery_task_payment_id
    ON payment_recovery_task (payment_id);

CREATE INDEX IF NOT EXISTS ix_payment_recovery_task_cancel_id
    ON payment_recovery_task (cancel_id);

CREATE INDEX IF NOT EXISTS ix_payment_recovery_task_status_type_created_at
    ON payment_recovery_task (status, recovery_type, created_at);

CREATE INDEX IF NOT EXISTS ix_settlement_statement_date_mid_status
    ON settlement_statement (settlement_date, mid, settlement_status);

CREATE INDEX IF NOT EXISTS ix_settlement_detail_statement_id
    ON settlement_detail (settlement_statement_id);

CREATE INDEX IF NOT EXISTS ix_settlement_detail_sales_id
    ON settlement_detail (sales_id);

CREATE INDEX IF NOT EXISTS ix_pg_api_log_payment_id
    ON pg_api_log (payment_id);

CREATE INDEX IF NOT EXISTS ix_pg_api_log_order_no
    ON pg_api_log (order_no);

CREATE INDEX IF NOT EXISTS ix_pg_api_log_request_id
    ON pg_api_log (request_id);

CREATE INDEX IF NOT EXISTS ix_pg_api_log_api_type_logged_at
    ON pg_api_log (api_type, logged_at);
