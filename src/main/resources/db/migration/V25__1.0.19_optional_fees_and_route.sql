ALTER TABLE sht.payment
  ALTER COLUMN provider_id DROP NOT NULL;
ALTER TABLE sht.payment
  ALTER COLUMN provider_fee DROP NOT NULL;
ALTER TABLE sht.payment
  ALTER COLUMN external_fee DROP NOT NULL;
ALTER TABLE sht.payment
  ALTER COLUMN fee DROP NOT NULL;
