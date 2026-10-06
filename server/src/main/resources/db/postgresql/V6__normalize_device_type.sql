-- 기기 분류를 자유 문자열에서 예약 화면의 12종으로 바꾼다. 예전 별칭은 현재 분류로 옮기고, 목록에 없는 값은 긴급 출장 A/S로 둔다.
UPDATE reservation SET device_type = LOWER(TRIM(device_type));
UPDATE reservation SET device_type = 'computer' WHERE device_type = 'laptop';
UPDATE reservation SET device_type = 'washing' WHERE device_type IN ('washer', '세탁기');
UPDATE reservation SET device_type = 'repair'
WHERE device_type NOT IN ('smartphone', 'computer', 'tv', 'console', 'aircon', 'washing', 'fridge',
                          'microwave', 'cleaner', 'internet', 'audio', 'repair');
ALTER TABLE reservation ALTER COLUMN device_type TYPE VARCHAR(20);
