-- 고객에게 보여주는 예약 번호(무작위 숫자 8자리). 순번인 id 대신 쓰므로 다른 예약을 짐작할 수 없다.
-- 발급한 번호는 다시 쓰지 않으며, 형식은 ReservationCode와 같다.
ALTER TABLE reservation ADD COLUMN code VARCHAR(8);

DO $$
BEGIN
    UPDATE reservation SET code = lpad(floor(random() * 100000000)::bigint::text, 8, '0');
    -- 기존 예약끼리 번호가 겹치면 겹친 예약만 다시 뽑는다.
    WHILE EXISTS (SELECT 1 FROM reservation GROUP BY code HAVING count(*) > 1) LOOP
        UPDATE reservation SET code = lpad(floor(random() * 100000000)::bigint::text, 8, '0')
        WHERE code IN (SELECT code FROM reservation GROUP BY code HAVING count(*) > 1);
    END LOOP;
END $$;

ALTER TABLE reservation ALTER COLUMN code SET NOT NULL;
ALTER TABLE reservation ADD CONSTRAINT uk_reservation_code UNIQUE (code);
ALTER TABLE reservation ADD CONSTRAINT ck_reservation_code_digits CHECK (code ~ '^[0-9]{8}$');

-- 완료·취소된 시각. 비회원 조회 가능 기간(종료 후 90일)을 이 시각부터 센다.
-- 기존 종료 예약은 정확한 시각을 알 수 없어 확정 일시 또는 접수 일시로 대신한다.
ALTER TABLE reservation ADD COLUMN closed_at TIMESTAMP(6) NULL;
UPDATE reservation SET closed_at = COALESCE(confirmed_at, created_at)
    WHERE status IN ('COMPLETED', 'CANCELLED');

-- 비회원 조회는 예약 번호와 휴대전화 번호로 하므로 조회용 비밀번호는 더 이상 쓰지 않는다.
ALTER TABLE reservation DROP COLUMN guest_password_hash;
