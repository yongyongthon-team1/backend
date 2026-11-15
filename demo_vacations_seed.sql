-- 시연용 방학 기간 시드
-- 탐방 개설은 선택한 날짜가 그 대학의 방학 기간(university_vacations) 안일 때만 된다.
-- 기본 시드의 방학은 2026-12-21부터라서, 시연일(2026-11-15)에는 탐방을 열 수 없다.
-- 그래서 모든 대학에 시연 기간(2026-11-10 ~ 2026-12-20)을 하나씩 더한다. 기존 방학 행은 건드리지 않는다.
--
-- 사용 순서: universities.sql → campus_places_vacations.sql → 이 파일
-- 실행: mysql --default-character-set=utf8mb4 <DB이름> < demo_vacations_seed.sql   (한글이 깨지지 않게 utf8mb4 필수)
--
-- 여러 번 실행해도 안전하다. 같은 이름('2026 방학')의 행이 이미 있는 대학은 건너뛴다.
-- 이 이름은 캠퍼스 목록 API의 방학 기간에 그대로 보인다.
--
-- 시연이 끝난 뒤 이 시드로 넣은 방학만 지우려면:
--   DELETE FROM university_vacations WHERE label = '2026 방학';

INSERT INTO university_vacations (university_id, start_date, end_date, label)
SELECT u.id, '2026-11-10', '2026-12-20', '2026 방학'
FROM universities u
WHERE NOT EXISTS (
    SELECT 1
    FROM university_vacations v
    WHERE v.university_id = u.id
      AND v.label = '2026 방학'
);
