CREATE TABLE payment_operations (
    id               UUID PRIMARY KEY,
    payment_id       UUID NOT NULL REFERENCES payments (id),
    type             VARCHAR(30) NOT NULL,
    resulting_status VARCHAR(20) NOT NULL,
    details          VARCHAR(500),
    occurred_at      TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_payment_operations_payment_id ON payment_operations (payment_id);
