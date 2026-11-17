use gazi;

-- location
insert into location (name, lat, lng) values
('seoul', 37.56650000, 126.97800000),
('seongnam', 37.42000000, 127.12650000),
('suwon', 37.26360000, 127.02860000),
('incheon', 37.45630000, 126.70520000),
('daejeon', 36.35040000, 127.38450000);


-- user
insert into `user`
(login_id, password, name, email, bio, location_id, user_rank, account)
values
('kim', 'test1234', '김민수', 'kim@test.com', '전자제품에 관심이 많아요.', 1, 80, 'kim_account'),
('lee', 'test1234', '이서연', 'lee@test.com', '책과 문구류를 좋아해요.', 2, 90, 'lee_account'),
('park', 'test1234', '박지훈', 'park@test.com', '운동용품을 거래합니다.', 3, 70, 'park_account'),
('choi', 'test1234', '최유진', 'choi@test.com', '다양한 물건을 구경해요.', 4, 85, 'choi_account'),
('jung', 'test1234', '정하늘', 'jung@test.com', '중고거래 환영합니다.', 5, 75, 'jung_account');


-- post
insert into post
(user_id, content, content_img, price, `condition`, deal_status, likes_count)
values
('kim', '아이폰 케이스 판매합니다.', null, 5000, 'NEW', 'ON_SALE', 0),
('lee', '읽기 좋은 소설책 판매해요.', null, 8000, 'LIKE_NEW', 'ON_SALE', 0),
('park', '덤벨 세트 판매합니다.', null, 20000, 'USED', 'ON_SALE', 0),
('choi', '무선 키보드 판매합니다.', null, 15000, 'LIKE_NEW', 'RESERVED', 0),
('jung', '텀블러 판매합니다.', null, 7000, 'NEW', 'ON_SALE', 0),
('kim', '블루투스 이어폰 판매해요.', null, 30000, 'USED', 'ON_SALE', 0),
('lee', '필기 노트 여러 권 판매합니다.', null, 4000, 'NEW', 'SOLD', 0),
('park', '운동용 매트 판매합니다.', null, 12000, 'LIKE_NEW', 'ON_SALE', 0),
('choi', '책상 스탠드 판매합니다.', null, 10000, 'USED', 'ON_SALE', 0),
('jung', '백팩 판매합니다.', null, 18000, 'LIKE_NEW', 'SOLD', 0);


-- follow
insert into follow (s_id, r_id) values
('kim', 'lee'),
('kim', 'park'),
('lee', 'kim'),
('lee', 'choi'),
('park', 'kim'),
('park', 'jung'),
('choi', 'lee'),
('choi', 'jung'),
('jung', 'kim'),
('jung', 'park');


-- likes
insert into likes (user_id, post_id) values
('kim', 2),
('kim', 3),
('lee', 1),
('lee', 5),
('park', 1),
('park', 4),
('choi', 2),
('choi', 6),
('jung', 3),
('jung', 8);


-- comment
insert into comment (post_id, user_id, c_content) values
(1, 'lee', '아직 판매 중인가요?'),
(1, 'park', '가격 조정 가능한가요?'),
(2, 'kim', '책 상태가 어떤가요?'),
(2, 'jung', '직거래도 가능한가요?'),
(3, 'choi', '구성품은 모두 있나요?'),
(3, 'lee', '관심 있습니다!'),
(4, 'jung', '예약 가능한가요?'),
(5, 'kim', '색상이 궁금합니다.'),
(5, 'park', '구매하고 싶어요.'),
(6, 'lee', '사용 기간이 얼마나 되나요?'),
(6, 'choi', '배터리 상태는 괜찮나요?'),
(7, 'jung', '거래 완료된 상품인가요?'),
(8, 'kim', '직접 확인할 수 있을까요?'),
(9, 'park', '아직 구매할 수 있나요?'),
(10, 'lee', '거래 장소가 어디인가요?');


-- deal_log
insert into deal_log
(post_id, s_id, money, buyer_score)
values
(7, 'lee', 4000, 9),
(10, 'jung', 18000, 10);
```