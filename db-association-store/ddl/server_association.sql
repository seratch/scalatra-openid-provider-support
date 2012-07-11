create table server_association (
  handle character varying(16) primary key,
  _type character varying(16) not null,
  mac_key character varying(64) not null,
  datetime_to_expire timestamp without time zone not null
);

