ALTER TABLE sht.invoice
  ADD COLUMN contract_id CHARACTER VARYING;
ALTER TABLE sht.invoice
  ADD COLUMN created_at TIMESTAMP WITHOUT TIME ZONE;
ALTER TABLE sht.invoice
  ADD COLUMN party_revision BIGINT;

ALTER TABLE sht.payment
  ADD COLUMN contract_id CHARACTER VARYING;
ALTER TABLE sht.payment
  ADD COLUMN party_revision BIGINT;
