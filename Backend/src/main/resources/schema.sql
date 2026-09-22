-- Documents uploaded for the agents to read. Applied on every boot (spring.sql.init.mode=always);
-- every statement is IF NOT EXISTS, so re-running it is a no-op rather than an error.
--
-- Deliberately portable between PostgreSQL (what this runs on) and H2 (what the tests run on), so
-- the schema under test is the same file that ships. TEXT, VARCHAR, INTEGER and TIMESTAMP WITH TIME
-- ZONE all mean the same thing to both.
--
-- The conversation-memory table is NOT here: Spring AI owns SPRING_AI_CHAT_MEMORY and ships its own
-- per-dialect DDL, enabled by spring.ai.chat.memory.repository.jdbc.initialize-schema.

CREATE TABLE IF NOT EXISTS platform_documents (
    id          VARCHAR(64)  NOT NULL PRIMARY KEY,
    name        VARCHAR(512) NOT NULL,
    media_type  VARCHAR(255),
    content     TEXT         NOT NULL,
    pages       INTEGER,
    -- WITH TIME ZONE so the stored instant does not depend on the server's default zone.
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Matches the ORDER BY that JdbcDocumentStore uses for both all() and eviction.
CREATE INDEX IF NOT EXISTS platform_documents_uploaded_at_idx
    ON platform_documents (uploaded_at DESC, id DESC);
