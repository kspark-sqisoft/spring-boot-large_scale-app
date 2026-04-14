CREATE TABLE post_comments (
    id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    parent_id BIGINT NULL,
    author_user_id BIGINT NOT NULL,
    content VARCHAR(2000) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_post_comments_post FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE,
    CONSTRAINT fk_post_comments_parent FOREIGN KEY (parent_id) REFERENCES post_comments (id) ON DELETE CASCADE,
    CONSTRAINT fk_post_comments_author FOREIGN KEY (author_user_id) REFERENCES users (id)
);

CREATE INDEX idx_post_comments_post_created ON post_comments (post_id, created_at);
