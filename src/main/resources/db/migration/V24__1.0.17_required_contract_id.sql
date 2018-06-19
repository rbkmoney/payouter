ALTER TABLE sht.invoice
  ALTER COLUMN contract_id SET NOT NULL;
ALTER TABLE sht.invoice
  ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE sht.payment
  ALTER COLUMN contract_id SET NOT NULL;

ALTER TABLE sht.payout
  ADD COLUMN contract_id CHARACTER VARYING;

UPDATE sht.payout SET contract_id = (SELECT contract_id FROM sht.payment WHERE payout_id = payout.id LIMIT 1);

ALTER TABLE sht.payout
  ALTER COLUMN contract_id SET NOT NULL;