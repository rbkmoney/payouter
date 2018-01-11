ALTER TABLE sht.payment
  ALTER COLUMN guarantee_deposit SET DEFAULT 0;

UPDATE sht.payment
SET guarantee_deposit = 0
WHERE guarantee_deposit IS NULL;