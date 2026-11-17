create database if not exists gazi;
use gazi;

create table if not exists location (
    location_id int auto_increment not null,
    name varchar(100) not null,
    lat decimal(11, 8) not null,
    lng decimal(11, 8) not null,
    primary key (location_id)
);

create table if not exists `user` (
    login_id varchar(20) not null,
    password varchar(255) not null,
    name varchar(50) not null,
    email varchar(255) not null unique,
    bio varchar(300) null,
    location_id int not null,
    user_rank tinyint not null default 50,
    account varchar(30) null,
    follower_count int not null default 0,
    following_count int not null default 0,
    primary key (login_id),
    check (user_rank between 0 and 100),
    check (follower_count >= 0),
    check (following_count >= 0),
    foreign key (location_id) references location(location_id)
);

create table if not exists follow (
    s_id varchar(20) not null,
    r_id varchar(20) not null,
    primary key (s_id, r_id),
    foreign key (s_id) references `user`(login_id),
    foreign key (r_id) references `user`(login_id)
);

create table if not exists post (
    id int auto_increment not null,
    user_id varchar(20) not null,
    content varchar(1000) not null,
    content_img varchar(255) null,
    price int not null,
    `condition` enum('NEW', 'LIKE_NEW', 'USED') not null,
    deal_status enum('ON_SALE', 'RESERVED', 'SOLD') not null default 'ON_SALE',
    likes_count int not null default 0,
    primary key (id),
    check (price >= 0),
    check (likes_count >= 0),
    foreign key (user_id) references `user`(login_id)
);

create table if not exists likes (
    user_id varchar(20) not null,
    post_id int not null,
    primary key (user_id, post_id),
    foreign key (user_id) references `user`(login_id),
    foreign key (post_id) references post(id) on delete cascade
);

create table if not exists comment (
    id int auto_increment not null,
    post_id int not null,
    user_id varchar(20) not null,
    c_content varchar(300) not null,
    primary key (id),
    foreign key (post_id) references post(id) on delete cascade,
    foreign key (user_id) references `user`(login_id)
);

create table if not exists deal_log (
    deal_id int auto_increment not null,
    post_id int not null unique,
    s_id varchar(20) not null,
    money int not null,
    deal_date datetime not null default current_timestamp,
    buyer_score tinyint null,
    primary key (deal_id),
    check (money >= 0),
    check (buyer_score between 0 and 10),
    foreign key (post_id) references post(id),
    foreign key (s_id) references `user`(login_id)
);

