-- 전화번호를 E.164 형식(+국가번호 + 국가 내 번호, 예: +821012345678)으로 통일한다.
-- 기존 데이터는 모두 국내 번호이므로 숫자만 남긴 뒤 국내 접두어 0을 +82로 바꾼다.
-- 애플리케이션은 PhoneNumber(libphonenumber)가 같은 형식으로 정규화한다.
CREATE FUNCTION pg_temp.to_e164(phone VARCHAR) RETURNS VARCHAR AS $$
    SELECT CASE
        WHEN digits LIKE '0%' THEN '+82' || substring(digits FROM 2)
        ELSE '+82' || digits
    END
    FROM (SELECT regexp_replace(phone, '[^0-9]', '', 'g') AS digits) d
$$ LANGUAGE SQL;

UPDATE customer SET phone = pg_temp.to_e164(phone);
UPDATE engineer SET phone = pg_temp.to_e164(phone);
UPDATE reservation SET contact_phone = pg_temp.to_e164(contact_phone)
    WHERE contact_phone IS NOT NULL;

-- DB는 저장 형식(+와 숫자)만 보장하고, 국가별 자릿수 같은 세부 규칙은 PhoneNumber가 맡는다.
ALTER TABLE customer ADD CONSTRAINT ck_customer_phone_e164
    CHECK (phone ~ '^\+[0-9]+$');
ALTER TABLE engineer ADD CONSTRAINT ck_engineer_phone_e164
    CHECK (phone ~ '^\+[0-9]+$');
ALTER TABLE reservation ADD CONSTRAINT ck_reservation_contact_phone_e164
    CHECK (contact_phone ~ '^\+[0-9]+$');
