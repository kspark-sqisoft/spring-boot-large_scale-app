-- Hibernate maps @Column(length=64) String to VARCHAR; CHAR(64) from V2 fails ddl-auto validate.
ALTER TABLE refresh_tokens MODIFY COLUMN token_hash VARCHAR(64) NOT NULL;
