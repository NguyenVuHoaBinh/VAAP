-- V4__Add_prompt_tags.sql
-- Add tagging support for prompts

CREATE TABLE prompt_tags (
                             id BIGSERIAL PRIMARY KEY,
                             prompt_id BIGINT NOT NULL,
                             tag VARCHAR(50) NOT NULL,
                             created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             FOREIGN KEY (prompt_id) REFERENCES prompts(id) ON DELETE CASCADE,
                             CONSTRAINT unique_prompt_tag UNIQUE (prompt_id, tag)
);

CREATE INDEX idx_prompt_tags_prompt ON prompt_tags(prompt_id);
CREATE INDEX idx_prompt_tags_tag ON prompt_tags(tag);
