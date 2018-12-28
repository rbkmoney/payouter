alter table sht.payout_event add column wallet_id character varying;
alter table sht.payout add column wallet_id character varying;
alter table sht.payout add column payout_tool_id character varying;
alter table sht.payout add column party_revision bigint;
alter table sht.payout add column payout_ref character varying;