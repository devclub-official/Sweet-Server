-- users 테이블의 password 컬럼을 NULL 허용으로 변경

ALTER TABLE users
    MODIFY COLUMN password VARCHAR(255) NULL;