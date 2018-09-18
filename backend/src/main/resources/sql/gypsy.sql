--20180709创建
-- 管理员列表
CREATE TABLE admin (
  admin_name    VARCHAR(255) PRIMARY KEY NOT NULL,
  pwd_MD5  VARCHAR(255)             NOT NULL,
  register_time BIGINT NOT NULL
);

-- 用户列表
CREATE TABLE public."user"
(
  id BIGSERIAL PRIMARY KEY NOT NULL,
  name VARCHAR(255) NOT NULL,
  password VARCHAR(255) NOT NULL,
  head_img VARCHAR(255),
  is_ban BOOLEAN DEFAULT FALSE  NOT NULL,
  register_time BIGINT NOT NULL,
  score INT DEFAULT 0 NOT NULL
);
CREATE INDEX user_id_index
  ON public."user"(id);
-- 房间列表
CREATE TABLE public.room
(
  id BIGSERIAL PRIMARY KEY NOT NULL,
  room_name VARCHAR(255) NOT NULL,
  creater VARCHAR(255) DEFAULT 'admin' NOT NULL,
  create_time BIGINT NOT NULL,
  room_type INT DEFAULT 0 NOT NULL,
  is_close INT DEFAULT 0 NOT NULL,
  limit_number INT DEFAULT 30 NOT NULL
);
COMMENT ON COLUMN public.room.room_type IS '0为无尽房，正整数为限时房';
COMMENT ON COLUMN public.room.is_close IS '0为开启，1为关闭';


drop table room;

