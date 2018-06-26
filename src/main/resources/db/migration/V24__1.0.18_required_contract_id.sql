ALTER TABLE sht.invoice
  ALTER COLUMN contract_id SET NOT NULL;
ALTER TABLE sht.invoice
  ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE sht.payment
  ALTER COLUMN contract_id SET NOT NULL;

ALTER TABLE sht.payout
  ADD COLUMN contract_id CHARACTER VARYING;

UPDATE sht.payout SET contract_id = (SELECT contract_id FROM sht.payment WHERE payout_id = payout.id LIMIT 1);
UPDATE sht.payout SET contract_id = (SELECT contract_id FROM sht.payment WHERE party_id = payout.party_id AND shop_id = payout.shop_id AND status = 'CAPTURED' LIMIT 1) WHERE status = 'CANCELLED';

ALTER TABLE sht.payout
  ALTER COLUMN contract_id SET NOT NULL;

ALTER TABLE sht.payout_event ADD COLUMN contract_id CHARACTER VARYING;

UPDATE sht.payout_event SET contract_id = (SELECT contract_id FROM sht.payout WHERE cast(id AS CHARACTER VARYING) = payout_event.payout_id);
