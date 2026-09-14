CREATE TABLE webhook_endpoints (
    id                 UUID PRIMARY KEY,
    merchant_reference VARCHAR(100) NOT NULL,
    url                VARCHAR(2048) NOT NULL,
    active             BOOLEAN NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL
);
