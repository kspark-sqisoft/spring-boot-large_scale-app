CREATE TABLE posts (
    id BIGINT NOT NULL,
    title VARCHAR(500) NOT NULL,
    content VARCHAR(10000) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_posts_created_at ON posts (created_at DESC);
