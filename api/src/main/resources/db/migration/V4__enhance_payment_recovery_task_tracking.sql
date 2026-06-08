alter table if exists payment_recovery_task
    alter column payment_id drop not null;

alter table if exists payment_recovery_task
    add column if not exists order_no varchar(80);

alter table if exists payment_recovery_task
    add column if not exists tid varchar(120);

alter table if exists payment_recovery_task
    add column if not exists idempotency_key varchar(120);

alter table if exists payment_recovery_task
    add column if not exists last_tried_at timestamp;

create index if not exists ix_payment_recovery_task_status_type
    on payment_recovery_task(status, recovery_type);

create index if not exists ix_payment_recovery_task_trace
    on payment_recovery_task(order_no, tid, idempotency_key);
