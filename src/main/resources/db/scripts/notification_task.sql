--liquibase formatted sql
--changeSet aPakhomov:1
CREATE TABLE  if not exists notification_task
(
    id      bigserial PRIMARY KEY,
    chat_id bigint,
    task_message varchar(255),
    task_time    timestamp
);

CREATE TABLE  if not exists user_properties
(
    id      bigserial PRIMARY KEY,
    chat_id bigint,
    latitude numeric,
    longitude numeric
);

