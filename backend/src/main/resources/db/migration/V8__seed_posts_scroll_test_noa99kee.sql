-- noa99kee@gmail.com 계정이 있을 때만: 무한 스크롤·커서 페이징 연습용 시드 글 30건
-- created_at 이 1분씩 증가(글 01이 가장 오래됨, 글 30이 가장 최신 → 목록 상단에 쌓임)
-- id 는 앱 Snowflake 와 겹치기 어려운 고정 블록(9100…)

INSERT INTO posts (id, author_user_id, title, content, created_at, updated_at)
WITH RECURSIVE seq AS (SELECT 1 AS n
                       UNION ALL
                       SELECT n + 1
                       FROM seq
                       WHERE n < 30)
SELECT 9100000000000000000 + seq.n,
       u.id,
       CONCAT('[시드] 페이징 테스트 ', LPAD(seq.n, 2, '0')),
       CONCAT(
               '무한 스크롤·커서 목록 테스트용 본문입니다. (', seq.n, '/30)\n',
               '같은 시각이면 id 역순으로도 정렬됩니다.\n',
               '작성자 이메일: noa99kee@gmail.com'
           ),
       TIMESTAMPADD(MINUTE, seq.n - 1, '2026-02-15 03:00:00'),
       TIMESTAMPADD(MINUTE, seq.n - 1, '2026-02-15 03:00:00')
FROM seq
         CROSS JOIN users u
WHERE u.email = 'noa99kee@gmail.com'
  AND NOT EXISTS(SELECT 1 FROM posts p WHERE p.id = 9100000000000000000 + seq.n);
