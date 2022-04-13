-- Remember to update the version table if you change this file
drop table if exists version;
create table version (
  version integer not null primary key
);
insert into version (version) values (1);

drop table if exists sequencer cascade;
create table sequencer (
  name varchar(32) primary key,
  seed bigint not null,
  ts bigint not null
);

drop table if exists containers cascade;
create table containers (
  db_id serial primary key,
  id text not null,
  created timestamp without time zone NOT NULL,
  modified timestamp without time zone
);

drop table if exists annotations;
create table annotations (
  db_id serial primary key,
  id text not null,
  json jsonb not null,
  created timestamp without time zone NOT NULL,
  modified timestamp without time zone,
  container_id integer not null
);
alter table annotations add foreign key (container_id) references containers (db_id);
