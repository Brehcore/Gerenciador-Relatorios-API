ALTER TABLE tb_user ADD COLUMN reset_token VARCHAR(255);
ALTER TABLE tb_user ADD COLUMN reset_token_expiry TIMESTAMP;