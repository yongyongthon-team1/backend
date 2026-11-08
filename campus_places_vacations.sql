-- 캠퍼스 장소와 방학 기간 시드 (universities는 universities.sql로 먼저 넣을 것: ID 1~5 순서)
-- 재실행하면 중복되므로 한 번만 실행한다.

-- campus_places
INSERT INTO campus_places (university_id, name, latitude, longitude) VALUES (1, '도서관', 37.276560, 127.132419);
INSERT INTO campus_places (university_id, name, latitude, longitude) VALUES (1, '본관', 37.276099, 127.133307);
INSERT INTO campus_places (university_id, name, latitude, longitude) VALUES (1, '샬롬관', 37.274942, 127.130071);
INSERT INTO campus_places (university_id, name, latitude, longitude) VALUES (1, '대운동장', 37.274749, 127.131423);
INSERT INTO campus_places (university_id, name, latitude, longitude) VALUES (2, '도서관', 37.241003, 127.079712);
INSERT INTO campus_places (university_id, name, latitude, longitude) VALUES (2, '학생회관', 37.242033, 127.080046);
INSERT INTO campus_places (university_id, name, latitude, longitude) VALUES (2, '예술디자인대학관', 37.241807, 127.084514);
INSERT INTO campus_places (university_id, name, latitude, longitude) VALUES (2, '사색의광장', 37.240854, 127.081041);
INSERT INTO campus_places (university_id, name, latitude, longitude) VALUES (3, '도서관', 37.321200, 127.127420);
INSERT INTO campus_places (university_id, name, latitude, longitude) VALUES (3, '단국역사관', 37.319505, 127.130063);
INSERT INTO campus_places (university_id, name, latitude, longitude) VALUES (3, '혜당관', 37.320439, 127.128385);
INSERT INTO campus_places (university_id, name, latitude, longitude) VALUES (3, '평화의 광장', 37.320171, 127.129048);
INSERT INTO campus_places (university_id, name, latitude, longitude) VALUES (4, '60주년 채플관', 37.223831, 127.186871);
INSERT INTO campus_places (university_id, name, latitude, longitude) VALUES (4, '학생회관', 37.223398, 127.187163);
INSERT INTO campus_places (university_id, name, latitude, longitude) VALUES (4, '명진당', 37.222098, 127.188570);
INSERT INTO campus_places (university_id, name, latitude, longitude) VALUES (4, '실내체육관', 37.221804, 127.190181);
INSERT INTO campus_places (university_id, name, latitude, longitude) VALUES (5, '도서관', 37.336812, 127.268290);
INSERT INTO campus_places (university_id, name, latitude, longitude) VALUES (5, '학생회관', 37.337157, 127.269699);
INSERT INTO campus_places (university_id, name, latitude, longitude) VALUES (5, '백년관', 37.337366, 127.265471);
INSERT INTO campus_places (university_id, name, latitude, longitude) VALUES (5, '명수당', 37.338819, 127.270130);

-- university_vacations
INSERT INTO university_vacations (university_id, start_date, end_date, label) VALUES (1, '2026-12-21', '2027-02-28', '2026-27 겨울방학');
INSERT INTO university_vacations (university_id, start_date, end_date, label) VALUES (1, '2027-06-21', '2027-08-31', '2027 여름방학');
INSERT INTO university_vacations (university_id, start_date, end_date, label) VALUES (2, '2026-12-21', '2027-02-28', '2026-27 겨울방학');
INSERT INTO university_vacations (university_id, start_date, end_date, label) VALUES (2, '2027-06-21', '2027-08-31', '2027 여름방학');
INSERT INTO university_vacations (university_id, start_date, end_date, label) VALUES (3, '2026-12-21', '2027-02-28', '2026-27 겨울방학');
INSERT INTO university_vacations (university_id, start_date, end_date, label) VALUES (3, '2027-06-21', '2027-08-31', '2027 여름방학');
INSERT INTO university_vacations (university_id, start_date, end_date, label) VALUES (4, '2026-12-21', '2027-02-28', '2026-27 겨울방학');
INSERT INTO university_vacations (university_id, start_date, end_date, label) VALUES (4, '2027-06-21', '2027-08-31', '2027 여름방학');
INSERT INTO university_vacations (university_id, start_date, end_date, label) VALUES (5, '2026-12-21', '2027-02-28', '2026-27 겨울방학');
INSERT INTO university_vacations (university_id, start_date, end_date, label) VALUES (5, '2027-06-21', '2027-08-31', '2027 여름방학');
