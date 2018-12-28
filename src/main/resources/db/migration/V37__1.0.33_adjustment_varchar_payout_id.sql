alter table sht.adjustment alter column payout_id type character varying using cast(payout_id as character varying);
create index adjustment_payout_id_idx on sht.adjustment using btree (payout_id);