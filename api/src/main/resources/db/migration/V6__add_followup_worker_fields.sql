ALTER TABLE external_send_request
    ADD COLUMN max_retry_count INTEGER DEFAULT 5 NOT NULL;

ALTER TABLE external_send_request
    ADD COLUMN processing_started_at TIMESTAMP;

ALTER TABLE alimtalk_queue
    ADD COLUMN max_retry_count INTEGER DEFAULT 5 NOT NULL;

ALTER TABLE alimtalk_queue
    ADD COLUMN processing_started_at TIMESTAMP;
