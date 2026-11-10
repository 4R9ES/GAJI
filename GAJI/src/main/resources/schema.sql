CREATE TABLE IF NOT EXISTS location (
    location_id INT PRIMARY KEY,
    name VARCHAR(20),
    lat DECIMAL(10,8),
    lng DECIMAL(11,8)
);

CREATE TABLE IF NOT EXISTS User (
    login_id INT PRIMARY KEY,
    password INT,
    name VARCHAR(20),
    email VARCHAR(30),
    bio VARCHAR(20),
    location_ID INT,
    user_rank INT,
    account INT,
    follower_count INT,
    following_count INT,
    FOREIGN KEY (location_ID) REFERENCES location(location_id)
);

CREATE TABLE IF NOT EXISTS follow (
    s_ID INT,
    r_ID INT,
    PRIMARY KEY (s_ID, r_ID),
    FOREIGN KEY (s_ID) REFERENCES User(login_id),
    FOREIGN KEY (r_ID) REFERENCES User(login_id)
);

CREATE TABLE IF NOT EXISTS Post (
    ID INT PRIMARY KEY,
    user_ID INT,
    content VARCHAR(100),
    content_img VARCHAR(255),
    price INT,
    item_condition VARCHAR(2),
    deal_status VARCHAR(10),
    likes_count VARCHAR(100),
    FOREIGN KEY (user_ID) REFERENCES User(login_id)
);

CREATE TABLE IF NOT EXISTS Likes (
    user_ID INT,
    post_ID INT,
    PRIMARY KEY (user_ID, post_ID),
    FOREIGN KEY (user_ID) REFERENCES User(login_id),
    FOREIGN KEY (post_ID) REFERENCES Post(ID)
);

CREATE TABLE IF NOT EXISTS Comment (
    ID INT PRIMARY KEY,
    post_ID INT,
    user_ID INT,
    c_content TEXT,
    FOREIGN KEY (post_ID) REFERENCES Post(ID),
    FOREIGN KEY (user_ID) REFERENCES User(login_id)
);

CREATE TABLE IF NOT EXISTS Deal_log (
    deal_ID INT PRIMARY KEY,
    post_ID INT UNIQUE,
    s_ID INT,
    money INT,
    deal_date DATE,
    buyer_score DECIMAL(2,1),
    FOREIGN KEY (post_ID) REFERENCES Post(ID),
    FOREIGN KEY (s_ID) REFERENCES User(login_id)
);
