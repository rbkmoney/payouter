alter table sht.payment alter column payout_id type character varying using cast(payout_id as character varying);
create index payment_payout_id_idx on sht.payment using btree (payout_id);