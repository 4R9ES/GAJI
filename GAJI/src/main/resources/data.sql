INSERT IGNORE INTO location (location_id, name, lat, lng) VALUES
(1, 'Gangnam', 37.49790000, 127.02760000),
(2, 'Hongdae', 37.55630000, 126.92360000),
(3, 'Jamsil', 37.51330000, 127.10010000),
(4, 'Suwon', 37.26360000, 127.02860000),
(5, 'Pangyo', 37.39480000, 127.11120000);

INSERT IGNORE INTO User (login_id, password, name, email) VALUES
(1, 1234, 'Kim', 'kim@test.com'),
(2, 1234, 'Lee', 'lee@test.com'),
(3, 1234, 'Park', 'park@test.com'),
(4, 1234, 'Choi', 'choi@test.com'),
(5, 1234, 'Jung', 'jung@test.com');

INSERT IGNORE INTO Post (ID, user_ID, content, content_img, price, item_condition, deal_status, likes_count) VALUES
(101, 1, 'Selling an iPhone 13', 'iphone.jpg', 400000, 'A', 'available', '3'),
(102, 2, 'Selling a MacBook Air', 'macbook.jpg', 800000, 'A', 'available', '2'),
(103, 3, 'Selling a keyboard', 'keyboard.jpg', 50000, 'B', 'completed', '2'),
(104, 4, 'Selling a gaming mouse', 'mouse.jpg', 30000, 'A', 'available', '2'),
(105, 5, 'Selling a monitor', 'monitor.jpg', 150000, 'B', 'available', '1'),
(106, 1, 'Selling an iPad', 'ipad.jpg', 350000, 'A', 'completed', '1'),
(107, 2, 'Selling headphones', 'headphone.jpg', 70000, 'A', 'available', '1'),
(108, 3, 'Selling a desk lamp', 'lamp.jpg', 20000, 'B', 'available', '1'),
(109, 4, 'Selling a backpack', 'bag.jpg', 40000, 'A', 'completed', '1'),
(110, 5, 'Selling a tablet', 'tablet.jpg', 200000, 'B', 'available', '1');

INSERT IGNORE INTO follow (s_ID, r_ID) VALUES
(1, 2), (1, 3), (2, 1), (2, 3), (2, 4), (3, 1), (3, 5), (4, 1), (4, 2), (5, 2);

INSERT IGNORE INTO Likes (user_ID, post_ID) VALUES
(1, 101), (1, 102), (1, 103),
(2, 101), (2, 104), (2, 105),
(3, 102), (3, 106), (3, 107),
(4, 103), (4, 108), (4, 109),
(5, 101), (5, 104), (5, 110);

INSERT IGNORE INTO Comment (ID, post_ID, user_ID, c_content) VALUES
(1, 101, 2, 'Is this still available?'),
(2, 101, 3, 'Can you lower the price?'),
(3, 101, 4, 'I am interested.'),
(4, 102, 1, 'Is the battery condition good?'),
(5, 102, 5, 'Can I meet you today?'),
(6, 103, 2, 'Is this keyboard mechanical?'),
(7, 103, 4, 'I want to buy this.'),
(8, 104, 1, 'Is this mouse wireless?'),
(9, 104, 3, 'Can you offer a discount?'),
(10, 105, 2, 'What is the screen size?'),
(11, 106, 5, 'Is the iPad still available?'),
(12, 107, 1, 'Does it include the charger?'),
(13, 108, 4, 'I like this lamp.'),
(14, 109, 3, 'Is delivery possible?'),
(15, 110, 2, 'Can I purchase this tomorrow?');

INSERT IGNORE INTO Deal_log (deal_ID, post_ID, s_ID, money, deal_date, buyer_score) VALUES
(1, 103, 2, 50000, '2026-10-01', 4.5),
(2, 106, 5, 350000, '2026-10-03', 5.0),
(3, 109, 1, 40000, '2026-10-05', 4.0),
(4, 104, 3, 30000, '2026-10-07', 4.5);
