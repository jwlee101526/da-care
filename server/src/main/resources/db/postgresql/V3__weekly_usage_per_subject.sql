-- 사용량 집계를 하루 서비스 전체 단위에서 주 단위·대상(전체, 계정, 비로그인 IP)별로 바꾼다.
-- 기존 일 단위 집계는 새 기준과 맞지 않아 삭제한다(사용량 초기화와 같다).
DELETE FROM api_usage;
ALTER TABLE api_usage DROP CONSTRAINT uk_api_usage_api_date;
ALTER TABLE api_usage RENAME COLUMN usage_date TO period_start;
ALTER TABLE api_usage ADD COLUMN subject VARCHAR(320) NOT NULL;
ALTER TABLE api_usage ADD CONSTRAINT uk_api_usage_api_subject_period
    UNIQUE (api, subject, period_start);
