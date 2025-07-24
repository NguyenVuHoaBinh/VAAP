-- V2__Insert_default_providers.sql
-- Insert default LLM providers

INSERT INTO llm_providers (provider_name, api_endpoint, vault_path, is_active, rate_limit) VALUES
                                                                                               ('OPENAI', 'https://api.openai.com/v1/', 'openai', true, 1000),
                                                                                               ('ANTHROPIC', 'https://api.anthropic.com/v1', 'anthropic', true, 1000),
                                                                                               ('COHERE', 'https://api.cohere.ai/v1', 'cohere', true, 1000),
                                                                                               ('GEMINI','https://generativelanguage.googleapis.com/v1beta/models','gemini',true,1000),
                                                                                               ('DEEPSEEK','https://api.deepseek.com/chat/completions','deepseek',true,1000);
