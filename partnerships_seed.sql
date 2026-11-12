-- 제휴 상점 시드 (시연용)
-- 대학별로 캠퍼스 안(100m 이내)을 뺀 뒤, 캠퍼스에서 가까운 순으로 고른다.
--   식당 4곳, 카페 4곳 (반경 1km), 놀거리 3곳 (반경 1km, 한국외대는 1.5km)
-- 제휴 기간: 2026-11-01 ~ 2027-12-31 (겨울·여름 방학을 모두 덮는다)
--
-- 사용 순서: universities.sql → campus_places_vacations.sql → stores_seed.sql → 이 파일
-- 실행: mysql --default-character-set=utf8mb4 <DB이름> < partnerships_seed.sql   (한글이 깨지지 않게 utf8mb4 필수)
--
-- 여러 번 실행해도 안전하다. store_partnerships는 상점당 한 행이고, 이미 제휴가 있는 상점은
-- (사장님이 직접 설정한 것 포함) 건드리지 않는다.
--
-- 시연이 끝난 뒤 이 시드로 넣은 제휴만 지우려면(사장님이 같은 문구로 설정한 것도 함께 지워지니 주의):
--   DELETE FROM store_partnerships
--   WHERE discount_description IN ('음료 1,000원 할인', '사이드 메뉴 1개 서비스', '이용 요금 10% 할인');

INSERT INTO store_partnerships
    (store_id, discount_description, start_date, end_date, created_at, updated_at)
SELECT
    r.store_id,
    CASE r.category
        WHEN '카페' THEN '음료 1,000원 할인'
        WHEN '식당' THEN '사이드 메뉴 1개 서비스'
        ELSE '이용 요금 10% 할인'
    END,
    '2026-11-01',
    '2027-12-31',
    NOW(),
    NOW()
FROM (
    SELECT
        d.store_id,
        d.category,
        ROW_NUMBER() OVER (PARTITION BY d.university_id, d.category ORDER BY d.dist, d.store_id) AS rn
    FROM (
        SELECT
            u.id AS university_id,
            s.id AS store_id,
            s.category,
            ST_Distance_Sphere(POINT(s.longitude, s.latitude), POINT(u.longitude, u.latitude)) AS dist
        FROM universities u
        JOIN stores s
    ) d
    WHERE d.dist > 100
      AND d.dist <= CASE WHEN d.university_id = 5 THEN 1500 ELSE 1000 END
) r
WHERE (r.category IN ('식당', '카페') AND r.rn <= 4)
   OR (r.category = '놀거리' AND r.rn <= 3)
ON DUPLICATE KEY UPDATE id = id;
