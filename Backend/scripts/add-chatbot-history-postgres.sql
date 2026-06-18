-- Chatbot history storage for PostgreSQL
-- Run once on database KLTN before starting backend.

CREATE TABLE IF NOT EXISTS chatbot_messages (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    patient_id BIGINT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chatbot_messages_user FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_chatbot_messages_patient FOREIGN KEY (patient_id)
        REFERENCES patients (id)
        ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_chatbot_messages_user_created
    ON chatbot_messages (user_id, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_chatbot_messages_patient_created
    ON chatbot_messages (patient_id, created_at DESC, id DESC);
