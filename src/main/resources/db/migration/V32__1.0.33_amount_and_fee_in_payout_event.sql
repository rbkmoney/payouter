alter table sht.payout_event add column amount bigint;
alter table sht.payout_event add column fee bigint;
alter table sht.payout_event add column currency_code character varying;
update sht.payout_event
set amount = payout.amount, fee = payout.fee, currency_code = payout.currency_code
from sht.payout where payout_event.payout_id = payout.payout_id;