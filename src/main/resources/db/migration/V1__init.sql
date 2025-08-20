create table if not exists app_user (
                                        id          bigserial primary key,
                                        email       varchar(255) not null unique,
    password    varchar(255) not null,
    role        varchar(32)  not null default 'USER',
    created_at  timestamptz  not null default now()
    );

create table if not exists bike (
                                    id          bigserial primary key,
                                    owner_id    bigint not null references app_user(id) on delete cascade,
    name        varchar(100) not null,
    brand       varchar(100),
    model       varchar(100),
    type        varchar(50),
    created_at  timestamptz  not null default now()
    );
