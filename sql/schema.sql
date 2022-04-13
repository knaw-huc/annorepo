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
  id serial primary key,
  name text,
  created timestamp without time zone NOT NULL,
  modified timestamp without time zone
);

drop table if exists annotations cascade;
create table annotations (
  id serial primary key,
  name text,
  container_id int,
  content jsonb,
  created timestamp without time zone NOT NULL,
  modified timestamp without time zone
);
alter table annotations add foreign key (container_id) references containers (id);
