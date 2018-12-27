alter table sht.refund alter column payout_id type character varying using cast(payout_id as character varying);
create index refund_payout_id_idx on sht.refund using btree (payout_id);