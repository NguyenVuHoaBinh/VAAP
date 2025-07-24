-- V1__Create_initial_schema.sql
-- Flyway migration script for initial schema

-- LLM Providers table
CREATE TABLE llm_providers (
                               id BIGSERIAL PRIMARY KEY,
                               provider_name VARCHAR(50) NOT NULL UNIQUE,
                               api_endpoint VARCHAR(255) NOT NULL,
                               vault_path VARCHAR(255) NOT NULL,
                               is_active BOOLEAN DEFAULT true,
                               rate_limit INTEGER,
                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Prompts table
CREATE TABLE prompts (
                         id BIGSERIAL PRIMARY KEY,
                         name VARCHAR(100) NOT NULL,
                         description VARCHAR(500),
                         template TEXT NOT NULL,
                         llm_provider_id BIGINT NOT NULL,
                         model_name VARCHAR(100) NOT NULL,
                         is_active BOOLEAN DEFAULT true,
                         current_version INTEGER DEFAULT 1,
                         created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         created_by VARCHAR(100) NOT NULL,
                         FOREIGN KEY (llm_provider_id) REFERENCES llm_providers(id),
                         CONSTRAINT unique_active_prompt_name UNIQUE (name, is_active)
);

-- Prompt versions table
CREATE TABLE prompt_versions (
                                 id BIGSERIAL PRIMARY KEY,
                                 prompt_id BIGINT NOT NULL,
                                 version_number INTEGER NOT NULL,
                                 template TEXT NOT NULL,
                                 change_description TEXT,
                                 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 created_by VARCHAR(100) NOT NULL,
                                 FOREIGN KEY (prompt_id) REFERENCES prompts(id) ON DELETE CASCADE,
                                 CONSTRAINT unique_prompt_version UNIQUE (prompt_id, version_number)
);

-- Prompt variables table
CREATE TABLE prompt_variables (
                                  id BIGSERIAL PRIMARY KEY,
                                  prompt_id BIGINT NOT NULL,
                                  variable_name VARCHAR(100) NOT NULL,
                                  default_value TEXT,
                                  description TEXT,
                                  is_required BOOLEAN DEFAULT true,
                                  data_type VARCHAR(20) DEFAULT 'STRING',
                                  FOREIGN KEY (prompt_id) REFERENCES prompts(id) ON DELETE CASCADE,
                                  CONSTRAINT unique_prompt_variable UNIQUE (prompt_id, variable_name)
);

-- Prompt tests table
CREATE TABLE prompt_tests (
                              id BIGSERIAL PRIMARY KEY,
                              test_name VARCHAR(255) NOT NULL,
                              test_type VARCHAR(50) NOT NULL,
                              prompt_a_id BIGINT NOT NULL,
                              prompt_b_id BIGINT,
                              sample_size INTEGER DEFAULT 100,
                              status VARCHAR(50) NOT NULL DEFAULT 'CREATED',
                              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              started_at TIMESTAMP,
                              completed_at TIMESTAMP,
                              created_by VARCHAR(100) NOT NULL,
                              FOREIGN KEY (prompt_a_id) REFERENCES prompts(id),
                              FOREIGN KEY (prompt_b_id) REFERENCES prompts(id)
);

-- Prompt test results table
CREATE TABLE prompt_test_results (
                                     id BIGSERIAL PRIMARY KEY,
                                     prompt_test_id BIGINT NOT NULL,
                                     prompt_id BIGINT NOT NULL,
                                     execution_time_ms BIGINT,
                                     token_count INTEGER,
                                     cost DECIMAL(10, 4),
                                     success_rate DECIMAL(5, 2),
                                     average_score DECIMAL(5, 2),
                                     notes TEXT,
                                     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     FOREIGN KEY (prompt_test_id) REFERENCES prompt_tests(id) ON DELETE CASCADE,
                                     FOREIGN KEY (prompt_id) REFERENCES prompts(id)
);

-- Test metrics table for additional metrics storage
CREATE TABLE test_metrics (
                              id BIGSERIAL PRIMARY KEY,
                              result_id BIGINT NOT NULL,
                              metric_name VARCHAR(100) NOT NULL,
                              metric_value TEXT,
                              FOREIGN KEY (result_id) REFERENCES prompt_test_results(id) ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX idx_prompts_active ON prompts(is_active);
CREATE INDEX idx_prompts_provider ON prompts(llm_provider_id);
CREATE INDEX idx_prompts_created_by ON prompts(created_by);
CREATE INDEX idx_prompt_versions_prompt ON prompt_versions(prompt_id);
CREATE INDEX idx_prompt_variables_prompt ON prompt_variables(prompt_id);
CREATE INDEX idx_prompt_tests_status ON prompt_tests(status);
CREATE INDEX idx_prompt_tests_created_by ON prompt_tests(created_by);
CREATE INDEX idx_prompt_test_results_test ON prompt_test_results(prompt_test_id);
CREATE INDEX idx_prompt_test_results_prompt ON prompt_test_results(prompt_id);

