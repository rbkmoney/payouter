create table sht.payout_range_data
(
  id        bigserial                   not null,
  party_id  character varying           not null,
  shop_id   character varying           not null,
  payout_id character varying           not null,
  from_time timestamp without time zone not null,
  to_time   timestamp without time zone not null,
  constraint payout_range_data_pkey primary key (id),
  constraint payout_range_data_ukey unique (payout_id)
);

insert into sht.payout_range_data(party_id, shop_id, payout_id, from_time, to_time)
select party_id, shop_id, payout_id, from_time, to_time
from sht.payout
order by id;

alter table sht.payout
  drop column from_time;
alter table sht.payout
  drop column to_time;