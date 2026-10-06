-- 기기 분류를 자유 문자열에서 정해진 값으로 바꾼다. 예전 별칭은 현재 분류로 옮기고, 알 수 없는 값은 기타로 둔다.
UPDATE reservation SET device_type = LOWER(TRIM(device_type));
UPDATE reservation SET device_type = 'computer' WHERE device_type = 'laptop';
UPDATE reservation SET device_type = 'washing' WHERE device_type IN ('washer', '세탁기');
UPDATE reservation SET device_type = 'etc'
WHERE device_type NOT IN ('smartphone', 'computer', 'tv', 'console', 'aircon', 'washing', 'fridge',
                          'microwave', 'cleaner', 'internet', 'audio', 'repair', 'etc');
ALTER TABLE reservation ALTER COLUMN device_type TYPE VARCHAR(20);
