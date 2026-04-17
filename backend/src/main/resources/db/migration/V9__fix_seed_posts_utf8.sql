-- PowerShell 파이프 등으로 V8 수동 실행 시 깨진 한글 복구 (Flyway UTF-8 로 실행돼도 무해)
UPDATE posts
SET title = CONCAT('[시드] 페이징 테스트 ', LPAD(CAST(id - 9100000000000000000 AS CHAR), 2, '0')),
    content = CONCAT(
            '무한 스크롤·커서 목록 테스트용 본문입니다. (',
            CAST(id - 9100000000000000000 AS CHAR),
            '/30)',
            CHAR(10),
            '같은 시각이면 id 역순으로도 정렬됩니다.',
            CHAR(10),
            '작성자 이메일: noa99kee@gmail.com'
        )
WHERE id BETWEEN 9100000000000000001 AND 9100000000000000030;
