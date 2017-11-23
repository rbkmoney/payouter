CREATE TYPE sht.PAYMENT_STATUS AS ENUM ('NEW', 'CAPTURED');

ALTER TABLE sht.payment
  ALTER COLUMN status DROP DEFAULT;

ALTER TABLE sht.payment
  ALTER COLUMN status TYPE sht.PAYMENT_STATUS USING status :: sht.PAYMENT_STATUS;

ALTER TABLE sht.payment
  ALTER COLUMN status SET DEFAULT 'NEW' :: sht.PAYMENT_STATUS;

ALTER TABLE sht.payment
  ALTER COLUMN provider_id TYPE INTEGER USING provider_id :: INTEGER;

ALTER TABLE sht.payment
  ADD COLUMN paid BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE sht.payment
  DROP COLUMN adjusted_by;

ALTER TABLE sht.payment
  DROP COLUMN is_adjustment_payment;

ALTER TABLE sht.payment
  RENAME COLUMN rbk_commission TO fee;


ALTER TABLE sht.adjustment
  DROP COLUMN test_shop;

ALTER TABLE sht.adjustment
  DROP COLUMN inversed_old_amount;
ALTER TABLE sht.adjustment
  DROP COLUMN inversed_old_provider_commission;
ALTER TABLE sht.adjustment
  DROP COLUMN inversed_old_rbk_commission;
ALTER TABLE sht.adjustment
  DROP COLUMN inversed_old_external_commission;
ALTER TABLE sht.adjustment
  ADD COLUMN payment_amount BIGINT;
ALTER TABLE sht.adjustment
  ADD COLUMN payment_fee BIGINT;

ALTER TABLE sht.adjustment
  RENAME COLUMN new_rbk_commission TO new_fee;



ALTER TABLE sht.adjustment
  ADD COLUMN paid BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE sht.adjustment
  ADD COLUMN payout_id BIGINT;

ALTER TABLE sht.adjustment
  RENAME COLUMN adjustment_status TO status;


CREATE TABLE sht.report (
  id          BIGSERIAL                   NOT NULL,
  created_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  payoutIds   CHARACTER VARYING           NOT NULL,
  name        CHARACTER VARYING           NOT NULL,
  content     CHARACTER VARYING           NOT NULL,
  description CHARACTER VARYING           NOT NULL,
  CONSTRAINT report_pkey PRIMARY KEY (id)
);