CREATE TABLE stored_files (
    id BIGINT NOT NULL,
    owner_user_id BIGINT NOT NULL,
    content_type VARCHAR(128) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    size_bytes BIGINT NOT NULL,
    storage_relative_path VARCHAR(512) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_sf_owner FOREIGN KEY (owner_user_id) REFERENCES users (id)
);

CREATE INDEX idx_stored_files_owner ON stored_files (owner_user_id);

CREATE TABLE post_images (
    id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_post_images_post_file (post_id, file_id),
    CONSTRAINT fk_pi_post FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE,
    CONSTRAINT fk_pi_file FOREIGN KEY (file_id) REFERENCES stored_files (id)
);

CREATE INDEX idx_post_images_post ON post_images (post_id);

ALTER TABLE users
    ADD COLUMN display_name VARCHAR(100) NULL,
    ADD COLUMN avatar_file_id BIGINT NULL;

ALTER TABLE users
    ADD CONSTRAINT fk_users_avatar FOREIGN KEY (avatar_file_id) REFERENCES stored_files (id) ON DELETE SET NULL;
