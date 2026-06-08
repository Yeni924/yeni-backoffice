CREATE UNIQUE INDEX IF NOT EXISTS uk_payment_transaction_approval_key_idx
    ON payment_transaction (approval_request_key);

CREATE UNIQUE INDEX IF NOT EXISTS uk_payment_transaction_order_no_idx
    ON payment_transaction (order_no);

CREATE UNIQUE INDEX IF NOT EXISTS uk_payment_transaction_tid_idx
    ON payment_transaction (tid);

CREATE UNIQUE INDEX IF NOT EXISTS uk_payment_cancel_request_key_idx
    ON payment_cancel (cancel_request_key);

CREATE UNIQUE INDEX IF NOT EXISTS uk_sales_transaction_source_idx
    ON sales_transaction (source_type, source_id);

CREATE UNIQUE INDEX IF NOT EXISTS uk_external_send_request_key_idx
    ON external_send_request (request_key);

CREATE UNIQUE INDEX IF NOT EXISTS uk_alimtalk_queue_message_key_idx
    ON alimtalk_queue (message_key);

CREATE UNIQUE INDEX IF NOT EXISTS uk_payment_recovery_task_key_idx
    ON payment_recovery_task (task_key);
