ALTER TABLE sht.adjustment
  ADD COLUMN payment_guarantee_deposit BIGINT DEFAULT 0;

ALTER TABLE sht.adjustment
  ADD COLUMN new_guarantee_deposit BIGINT DEFAULT 0;

