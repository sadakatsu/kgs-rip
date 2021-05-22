create table user (
    id int not null auto_increment,
    name varchar(64) unique not null,
    indexed timestamp null,
    primary key (id)
);

create index user_indexed on user (indexed);

insert into user (name, indexed) values ('sadakatsu', '2020-01-31 19:00:00');

create table game (
    id int not null auto_increment,
    url varchar(256) unique null,
    white int not null,
    white_rank varchar(4) null,
    black int not null,
    black_rank varchar(4) null,
    setup varchar(16) null,
    start_time timestamp not null,
    type varchar(16) null,
    result varchar(32) null,
    random float not null,
    downloaded tinyint(1) not null default false,
    primary key(id),
    foreign key (white) references user (id),
    foreign key (black) references user (id),
    unique key white_black_start (white, black, start_time)
);

create index game_white_rank on game (white_rank);
create index game_black_rank on game (black_rank);
create index game_start_time on game (start_time);
create index game_type on game (type);
create index game_random on game (random);
create index game_downloaded on game (downloaded);

create table rating (
    id int not null,
    user int not null,
    date timestamp not null,
    rating float not null,
    primary key (id),
    unique key user_date (user, date)
);

create index rating_value on rating (rating);
