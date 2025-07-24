-- V3__Create_audit_tables.sql
-- Create audit tables for tracking changes

CREATE TABLE prompt_audit_log (
                                  id BIGSERIAL PRIMARY KEY,
                                  prompt_id BIGINT NOT NULL,
                                  action VARCHAR(50) NOT NULL,
                                  field_name VARCHAR(100),
                                  old_value TEXT,
                                  new_value TEXT,
                                  changed_by VARCHAR(100) NOT NULL,
                                  changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_prompt_audit_prompt ON prompt_audit_log(prompt_id);
CREATE INDEX idx_prompt_audit_changed_by ON prompt_audit_log(changed_by);