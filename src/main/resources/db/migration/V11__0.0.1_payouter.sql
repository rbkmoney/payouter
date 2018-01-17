--drop unused table
DROP TABLE sht.payout_old;
DROP TYPE sht.payoutstatus_old;

-- refactor payment table
-- rename commission to fee
ALTER TABLE sht.payment RENAME COLUMN provider_commission TO provider_fee;
ALTER TABLE sht.payment RENAME COLUMN external_commission TO external_fee;
-- drop unused columns
ALTER TABLE sht.payment DROP COLUMN paid;
ALTER TABLE sht.payment DROP COLUMN inn;
-- rename wtime to created_at, drop default
ALTER TABLE sht.payment ALTER COLUMN wtime DROP DEFAULT;
ALTER TABLE sht.payment RENAME COLUMN wtime TO created_at;
-- add terminal id
ALTER TABLE sht.payment ADD COLUMN terminal_id INT;
--add domain revision
ALTER TABLE sht.payment ADD COLUMN domain_revision BIGINT;
-- rename status 'NEW' to 'PENDING'
ALTER TABLE sht.payment ALTER COLUMN status TYPE CHARACTER VARYING;
UPDATE sht.payment SET status = 'PENDING' WHERE status = 'NEW';
-- add 'cancelled' status
ALTER TABLE sht.payment ALTER COLUMN status DROP DEFAULT;
ALTER TYPE sht.payment_status RENAME TO old_payment_status;
CREATE TYPE sht.payment_status AS ENUM ('PENDING', 'CAPTURED', 'CANCELLED');
ALTER TABLE sht.payment
  ALTER COLUMN status TYPE sht.payment_status USING status :: TEXT :: sht.payment_status;
DROP TYPE sht.old_payment_status;
-- change status for cancelled payments, drop payment_status column
UPDATE sht.payment SET status = 'CANCELLED' :: sht.payment_status WHERE payment_status = 'cancelled';
ALTER TABLE sht.payment DROP COLUMN payment_status;
-- default status value
ALTER TABLE sht.payment
  ALTER COLUMN status SET DEFAULT 'PENDING' :: sht.payment_status;
ALTER TABLE sht.payment ALTER COLUMN status SET NOT NULL;
-- test not null
ALTER TABLE sht.payment ALTER COLUMN test SET NOT NULL;

--refactor adjustment table
--drop unused columns
ALTER TABLE sht.adjustment DROP COLUMN paid;
-- drop default alter column created_at
ALTER TABLE sht.adjustment ALTER COLUMN created_at DROP DEFAULT;
-- rename commission to fee
ALTER TABLE sht.adjustment RENAME COLUMN new_provider_commission TO new_provider_fee;
ALTER TABLE sht.adjustment RENAME COLUMN new_external_commission TO new_external_fee;
-- rename updated_at to captured_at
ALTER TABLE sht.adjustment RENAME COLUMN update_at TO captured_at;
ALTER TABLE sht.adjustment ALTER COLUMN captured_at DROP DEFAULT;
ALTER TABLE sht.adjustment ALTER COLUMN captured_at DROP NOT NULL;

--refactor refund table
--drop unused columns
ALTER TABLE sht.refund DROP COLUMN paid;
-- rename updated_at to succeeded_at
ALTER TABLE sht.refund RENAME COLUMN update_at TO succeeded_at;
ALTER TABLE sht.refund ALTER COLUMN succeeded_at DROP DEFAULT;
ALTER TABLE sht.refund ALTER COLUMN succeeded_at DROP NOT NULL;
--add domain revision
ALTER TABLE sht.refund ADD COLUMN domain_revision BIGINT;

-- refactor payout table
ALTER TABLE sht.payout DROP COLUMN cor_account;
-- canceled -> cancelled
ALTER TABLE sht.payout ALTER COLUMN status TYPE CHARACTER VARYING;
UPDATE sht.payout SET status = 'CANCELLED' WHERE status = 'CANCELED';
ALTER TYPE sht.payout_status RENAME TO old_payout_status;
CREATE TYPE sht.payout_status AS ENUM ('UNPAID', 'PAID', 'CONFIRMED', 'CANCELLED');
ALTER TABLE sht.payout
  ALTER COLUMN status TYPE sht.payout_status USING status :: TEXT :: sht.payout_status;
DROP TYPE sht.old_payout_status;
-- change payout_type names
ALTER TABLE sht.payout ALTER COLUMN payout_type TYPE CHARACTER VARYING;
UPDATE sht.payout SET payout_type = 'bank_card' WHERE payout_type = 'CardPayout';
UPDATE sht.payout SET payout_type = 'bank_account' WHERE payout_type = 'AccountPayout';
ALTER TYPE sht.payout_type RENAME TO old_payout_type;
CREATE TYPE sht.payout_type AS ENUM ('bank_card', 'bank_account');
ALTER TABLE sht.payout
  ALTER COLUMN payout_type TYPE sht.payout_type USING payout_type :: TEXT :: sht.payout_type;
DROP TYPE sht.old_payout_type;

-- shop_meta table
CREATE TABLE sht.shop_meta (
  party_id               CHARACTER VARYING           NOT NULL,
  shop_id                CHARACTER VARYING           NOT NULL,
  wtime                  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  last_payout_created_at TIMESTAMP WITHOUT TIME ZONE,
  CONSTRAINT shop_meta_pkey PRIMARY KEY (party_id, shop_id)
);

-- account type
CREATE TYPE sht.ACCOUNT_TYPE AS ENUM ('merchant', 'provider', 'system', 'external');

CREATE TYPE sht.REPORT_STATUS AS ENUM ('READY', 'SENT', 'FAILED');
ALTER TABLE sht.report ADD COLUMN status sht.REPORT_STATUS NOT NULL DEFAULT 'READY';
ALTER TABLE sht.report ADD COLUMN last_send_at TIMESTAMP WITHOUT TIME ZONE;

UPDATE sht.report SET status = 'SENT';
UPDATE sht.report SET last_send_at = created_at;

CREATE TABLE sht.event_stock_meta (
  last_event_id BIGINT,
  last_event_created_at TIMESTAMP WITHOUT TIME ZONE
);

