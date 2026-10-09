-- Documents uploaded for the agents to read. Applied on every boot (spring.sql.init.mode=always);
-- every statement is IF NOT EXISTS, so re-running it is a no-op rather than an error.
--
-- Written to work on both H2 (the default) and PostgreSQL, so switching databases later is a
-- change of spring.datasource.url, not of this file.
--
-- The conversation-memory table is NOT here: Spring AI owns SPRING_AI_CHAT_MEMORY and ships its
-- own DDL, enabled by spring.ai.chat.memory.repository.jdbc.initialize-schema.

CREATE TABLE IF NOT EXISTS platform_documents (
    id          VARCHAR(64)  NOT NULL PRIMARY KEY,
    name        VARCHAR(512) NOT NULL,
    media_type  VARCHAR(255),
    content     TEXT         NOT NULL,
    pages       INTEGER,
    -- WITH TIME ZONE so the stored instant does not depend on the server's default zone
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Matches the ORDER BY that JdbcDocumentStore uses to find the oldest upload.
CREATE INDEX IF NOT EXISTS platform_documents_uploaded_at_idx
    ON platform_documents (uploaded_at DESC, id DESC);
