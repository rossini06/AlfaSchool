ALTER TABLE audit_logs ADD COLUMN modulo VARCHAR(60);
ALTER TABLE audit_logs ADD COLUMN acao VARCHAR(60);
ALTER TABLE audit_logs ADD COLUMN dados_anteriores JSON;
ALTER TABLE audit_logs ADD COLUMN dados_novos JSON;
ALTER TABLE audit_logs ADD COLUMN ip VARCHAR(45);
ALTER TABLE audit_logs ADD COLUMN user_agent VARCHAR(255);

CREATE INDEX idx_audit_logs_modulo ON audit_logs(modulo);
CREATE INDEX idx_audit_logs_acao ON audit_logs(acao);
