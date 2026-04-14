ALTER TABLE posts
    ADD COLUMN author_user_id BIGINT NULL,
    ADD CONSTRAINT fk_posts_author_user FOREIGN KEY (author_user_id) REFERENCES users (id);

CREATE INDEX idx_posts_author_user ON posts (author_user_id);

-- 시드로 넣은 글은 noa99kee@gmail.com 계정 소유로 표시 (해당 사용자가 있을 때만)
UPDATE posts p
SET p.author_user_id = (SELECT u.id FROM users u WHERE u.email = 'noa99kee@gmail.com' LIMIT 1)
WHERE p.title LIKE '[시드]%'
  AND EXISTS (SELECT 1 FROM users u2 WHERE u2.email = 'noa99kee@gmail.com');
