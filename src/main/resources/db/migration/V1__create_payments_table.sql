CREATE TABLE payments (
    id               UUID PRIMARY KEY,
    amount           NUMERIC(19, 2) NOT NULL,
    currency         VARCHAR(3) NOT NULL,
    method           VARCHAR(20) NOT NULL,
    status           VARCHAR(20) NOT NULL,
    merchant_reference VARCHAR(100),
    idempotency_key  VARCHAR(100) NOT NULL,
    gateway_reference VARCHAR(100),
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    version          BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_payments_idempotency_key UNIQUE (idempotency_key)
);

CREATE INDEX idx_payments_status ON payments (status);
