CREATE UNIQUE INDEX IF NOT EXISTS uk_settlement_statement_date_mid
    ON settlement_statement (settlement_date, mid);
