alter table sht.payout add column payout_id character varying;
update sht.payout set payout_id = id::character varying;
alter table sht.payout alter column payout_id set not null;

alter table sht.payout alter column payout_id set not null;
alter table sht.payout add constraint payout_ukey unique (payout_id);

alter table sht.payout_event add column amount bigint;
alter table sht.payout_event add column fee bigint;
alter table sht.payout_event add column currency_code character varying;
update sht.payout_event
set amount = payout.amount, fee = payout.fee, currency_code = payout.currency_code
from sht.payout where payout_event.payout_id = payout.payout_id;

alter table sht.payout_event add column wallet_id character varying;
alter table sht.payout add column wallet_id character varying;
alter table sht.payout add column payout_tool_id character varying;
alter table sht.payout add column party_revision bigint;

alter table sht.payment alter column payout_id type character varying using cast('payout_id' as character varying);
alter table sht.refund alter column payout_id type character varying using cast('payout_id' as character varying);
alter table sht.adjustment alter column payout_id type character varying using cast('payout_id' as character varying);

create table sht.payout_range_data (
   id bigserial not null,
   party_id character varying not null,
   shop_id character varying not null,
   payout_id character varying not null,
   from_time timestamp without time zone not null,
   to_time timestamp without time zone not null,
   constraint payout_range_data_pkey primary key (id),
   constraint payout_range_data_ukey unique (payout_id)
);

insert into sht.payout_range_data(party_id, shop_id, payout_id, from_time, to_time)
  select party_id, shop_id, payout_id, from_time, to_time
  from sht.payout
  order by id;

alter table sht.payout drop column from_time;
alter table sht.payout drop column to_time;

alter table sht.payout add column payout_ref character varying;