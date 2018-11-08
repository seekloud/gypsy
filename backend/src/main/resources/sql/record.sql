

CREATE TABLE game_record(
  record_id SERIAL8 PRIMARY KEY NOT NULL ,
  room_id BIGINT NOT NULL ,
  user_id BIGINT NOT NULL ,
  start_time BIGINT NOT NULL ,
  end_time BIGINT NOT NULL ,
  file_path TEXT NOT NULL,
  initial_time BIGINT NOT NULL
);


-- record_id 对应多个user_id
create TABLE user_record_map(
record_id BIGINT NOT NULL ,
user_id BIGINT NOT NULL ,
room_id BIGINT NOT NULL
);

ALTER TABLE user_record_map ALTER COLUMN user_id TYPE TEXT;

ALTER TABLE user_record_map ADD startFrame BIGINT DEFAULT 0;
ALTER TABLE user_record_map ADD endFrame BIGINT DEFAULT -1;
-- ALTER TABLE game_record ADD totalFrame BIGINT DEFAULT 0;
ALTER TABLE user_record_map DROP COLUMN startFrame;
ALTER TABLE user_record_map DROP COLUMN endFrame;
