CREATE TABLE app_users (
    id            UUID PRIMARY KEY,
    username      VARCHAR(150) NOT NULL,
    password_hash VARCHAR(200) NOT NULL,
    role          VARCHAR(20) NOT NULL,
    CONSTRAINT uq_app_users_username UNIQUE (username)
);
