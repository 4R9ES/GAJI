USE gaji;
DELETE FROM `user` WHERE login_id = 'han';
-- schema.sql / data.sql 기준 테스트 값
SET @me_id     = 'kim';  -- 로그인한 회원 (login_id 는 문자열)
SET @target_id = 'lee';  -- 조회·팔로우 대상 회원
SET @post_id   = 1;      -- 조회할 게시글 (kim 의 '아이폰 케이스')

-- A. 기본 조회

-- 01. 전체 게시글 목록
SELECT p.id, p.content, p.price, p.`condition`, p.deal_status,
       u.name AS seller, l.name AS location
FROM post p
JOIN `user` u   ON u.login_id = p.user_id
JOIN location l ON l.location_id = u.location_id
ORDER BY p.id DESC;

-- 02. 게시글별 좋아요 수
SELECT p.id, p.content, COUNT(l.user_id) AS likes_count
FROM post p
LEFT JOIN likes l ON l.post_id = p.id
GROUP BY p.id, p.content
ORDER BY p.id;

-- 03. 특정 게시글의 댓글 목록 (작성자 이름 포함)
SELECT c.id, u.name AS commenter, c.c_content
FROM comment c
JOIN `user` u ON u.login_id = c.user_id
WHERE c.post_id = @post_id
ORDER BY c.id;

-- 04. 회원이 좋아요 누른 게시글
SELECT p.id, p.content, p.price
FROM likes l
JOIN post p ON p.id = l.post_id
WHERE l.user_id = @me_id
ORDER BY p.id;

-- 05. 내가 팔로우하는 사람들의 게시글을 보는 피드
SELECT p.id, p.content, p.price, p.deal_status, u.name AS seller
FROM follow f
JOIN post p   ON p.user_id = f.r_id
JOIN `user` u ON u.login_id = p.user_id
WHERE f.s_id = @me_id
ORDER BY p.id DESC;

-- 06. 좋아요 많은 게시글 Top 3 (동점이면 id 가 작은 글 먼저)
SELECT p.id, p.content, COUNT(l.user_id) AS likes_count
FROM post p
LEFT JOIN likes l ON l.post_id = p.id
GROUP BY p.id, p.content
ORDER BY likes_count DESC, p.id
LIMIT 3;

-- B. 회원 · 팔로우 기능

-- B0. 동네 목록 조회
SELECT location_id, name
FROM location
ORDER BY location_id;

-- B1. 회원가입
SET @new_login_id    = 'han';
SET @new_email       = 'han@test.com';
SET @new_location_id = 1;

-- 아이디 중복 확인 (1이면 USER409_1)
SELECT EXISTS (SELECT 1 FROM `user` WHERE login_id = @new_login_id) AS login_id_exists;
-- 이메일 중복 확인 (1이면 USER409_2)
SELECT EXISTS (SELECT 1 FROM `user` WHERE email = @new_email) AS email_exists;
-- 동네 존재 확인 (0이면 USER400_1)
SELECT EXISTS (SELECT 1 FROM location WHERE location_id = @new_location_id) AS location_exists;

START TRANSACTION;
INSERT INTO `user` (login_id, password, name, email, bio, location_id, account)
VALUES (@new_login_id, 'test1234', '한지민', @new_email, '새로 가입했어요.', @new_location_id, 'han_account');
ROLLBACK;

-- B2. 로그인
SET @email = 'kim@test.com';
SET @pw    = 'test1234';
SELECT u.login_id, u.name, u.email, u.bio, u.location_id, l.name AS location, u.account,
       u.user_rank, u.follower_count, u.following_count
FROM `user` u
JOIN location l ON l.location_id = u.location_id
WHERE u.email = @email AND u.password = @pw;

-- B3. 내 프로필 정보 (이름, 이메일, 소개, 동네, 계좌, 신뢰도 + 팔로워/팔로잉 수)
SELECT u.login_id, u.name, u.email, u.bio, u.location_id, l.name AS location, u.account,
       u.user_rank, u.follower_count, u.following_count
FROM `user` u
JOIN location l ON l.location_id = u.location_id
WHERE u.login_id = @me_id;

-- B4. 비밀번호 변경 (현재 비밀번호가 맞을 때만 변경, changed 가 1이면 성공)
SET @old_pw = 'test1234';
SET @new_pw = 'test5678';
START TRANSACTION;
UPDATE `user` SET password = @new_pw
WHERE login_id = @me_id AND password = @old_pw;
SELECT ROW_COUNT() AS changed;
ROLLBACK;

-- B5. 팔로우 / 언팔로우 (자기 자신 팔로우와 중복 팔로우는 저장x)
SET @follow_target = 'jung';   -- kim 은 아직 jung 을 팔로우하지 않음

INSERT INTO follow (s_id, r_id)
SELECT @me_id, @follow_target FROM DUAL WHERE @me_id <> @follow_target
ON DUPLICATE KEY UPDATE s_id = s_id;

DELETE FROM follow WHERE s_id = @me_id AND r_id = @follow_target;

UPDATE `user` SET following_count = (SELECT COUNT(*) FROM follow WHERE s_id = @me_id)
WHERE login_id = @me_id;          -- 나
UPDATE `user` SET follower_count  = (SELECT COUNT(*) FROM follow WHERE r_id = @follow_target)
WHERE login_id = @follow_target;  -- 상대

-- B6. 팔로잉 목록 / 팔로워 목록
SELECT u.login_id, u.name, u.bio
FROM follow f JOIN `user` u ON u.login_id = f.r_id
WHERE f.s_id = @me_id ORDER BY u.login_id;

SELECT u.login_id, u.name, u.bio
FROM follow f JOIN `user` u ON u.login_id = f.s_id
WHERE f.r_id = @me_id ORDER BY u.login_id;

-- C. 게시글·댓글·좋아요 CRUD  (id 는 auto_increment 라 LAST_INSERT_ID() 로 받음)

-- C1. 게시글 작성 / 수정 / 삭제 (본인 글만)
INSERT INTO post (user_id, content, content_img, price, `condition`)
VALUES (@me_id, '카메라 판매합니다.', 'camera.jpg', 120000, 'USED');
SET @new_post_id = LAST_INSERT_ID();

UPDATE post SET content = '카메라 판매합니다. (가격 내림)', price = 100000
WHERE id = @new_post_id AND user_id = @me_id;

-- 삭제: likes·comment 는 ON DELETE CASCADE 라 글만 지우면 같이 지워집니다.
--       deal_log 는 cascade 가 아니라서 거래가 완료된 글은 삭제되지 않습니다.
DELETE FROM post WHERE id = @new_post_id AND user_id = @me_id;

-- C2. 사용자별 게시글 / 게시글 상세 (작성자, 좋아요 수)
SELECT id, content, price, deal_status FROM post
WHERE user_id = @target_id ORDER BY id DESC;

SELECT p.id, p.content, u.name AS seller,
       (SELECT COUNT(*) FROM likes WHERE post_id = p.id) AS likes_count
FROM post p JOIN `user` u ON u.login_id = p.user_id
WHERE p.id = @post_id;

-- C3. 댓글 작성 / 내 댓글 삭제
INSERT INTO comment (post_id, user_id, c_content) VALUES (@post_id, @me_id, '좋은 상품이네요!');
SET @new_comment_id = LAST_INSERT_ID();
DELETE FROM comment WHERE id = @new_comment_id AND user_id = @me_id;

-- C4. 좋아요 / 좋아요 취소 (한 글에 한 번만) + post.likes_count 갱신
SET @like_post_id = 5;   -- kim 이 아직 좋아요 안 누른 글
INSERT INTO likes (user_id, post_id) VALUES (@me_id, @like_post_id)
ON DUPLICATE KEY UPDATE user_id = user_id;
DELETE FROM likes WHERE user_id = @me_id AND post_id = @like_post_id;
UPDATE post SET likes_count = (SELECT COUNT(*) FROM likes WHERE post_id = @like_post_id)
WHERE id = @like_post_id;

-- D. 중고거래 변형 쿼리 (3개 이상 필수)

-- D1. 판매중인 글을 가격 범위로 검색 (가격 낮은 순)
SET @min_price = 10000;
SET @max_price = 200000;
SELECT p.id, p.content, p.price, p.`condition`
FROM post p
WHERE p.deal_status = 'ON_SALE' AND p.price BETWEEN @min_price AND @max_price
ORDER BY p.price;

-- D2. 동네별 판매중 글 수 (판매자 동네 기준)
SELECT l.name AS location, COUNT(*) AS available_posts
FROM post p
JOIN `user` u   ON u.login_id = p.user_id
JOIN location l ON l.location_id = u.location_id
WHERE p.deal_status = 'ON_SALE'
GROUP BY l.location_id, l.name
ORDER BY available_posts DESC, l.name;

-- D3. 내 동네 근처 판매글 (위도·경도 거리, km)
SET @radius_km = 30;
SELECT * FROM (
    SELECT p.id, p.content, p.price, l.name AS location,
           ROUND(ST_Distance_Sphere(POINT(l.lng, l.lat), POINT(ml.lng, ml.lat)) / 1000, 1) AS distance_km
    FROM `user` me
    JOIN location ml   ON ml.location_id = me.location_id
    JOIN post p        ON p.deal_status = 'ON_SALE' AND p.user_id <> me.login_id
    JOIN `user` seller ON seller.login_id = p.user_id
    JOIN location l    ON l.location_id = seller.location_id
    WHERE me.login_id = @me_id
) near
WHERE near.distance_km <= @radius_km
ORDER BY near.distance_km, near.id;

-- D4. 거래 완료 내역 (상품, 판매자, 구매자, 금액, 평가)
SELECT d.deal_id, p.content AS item, s.name AS seller, b.name AS buyer,
       d.money, d.deal_date, d.buyer_score
FROM deal_log d
JOIN post p   ON p.id = d.post_id
JOIN `user` s ON s.login_id = p.user_id
JOIN `user` b ON b.login_id = d.buyer_id
ORDER BY d.deal_date;

-- D5. 판매자별 거래 횟수와 평균 평점
SELECT s.login_id, s.name, COUNT(d.deal_id) AS deals, ROUND(AVG(d.buyer_score), 2) AS avg_score
FROM `user` s
JOIN post p     ON p.user_id = s.login_id
JOIN deal_log d ON d.post_id = p.id
GROUP BY s.login_id, s.name
ORDER BY avg_score DESC, deals DESC;

-- D6. 거래 완료 처리 (글 상태 변경 + 거래 기록 저장을 한 묶음으로)
SET @sell_post_id = 1;      -- kim 의 판매중 글
SET @buyer_id     = 'lee';  -- 이 글에 댓글을 남긴 회원
START TRANSACTION;
UPDATE post SET deal_status = 'SOLD'
WHERE id = @sell_post_id AND deal_status IN ('ON_SALE', 'RESERVED');
SET @updated = ROW_COUNT();
-- 위 UPDATE 가 성공해서 SOLD 가 된 경우에만 거래 기록 저장 (post_id unique 라 중복 거래도 막힘)
INSERT INTO deal_log (post_id, buyer_id, money)
SELECT id, @buyer_id, price FROM post
WHERE id = @sell_post_id AND @updated = 1;
ROLLBACK;
