alter table sht.payout add column payout_id character varying;
update sht.payout set payout_id = id::character varying;
alter table sht.payout alter column payout_id set not null;

alter table sht.payout alter column payout_id set not null;
alter table sht.payout add constraint payout_ukey unique (payout_id);