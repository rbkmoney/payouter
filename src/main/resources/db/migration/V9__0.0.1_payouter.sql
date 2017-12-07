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
UPDATE pg_enum SET enumlabel = 'PENDING'
WHERE enumlabel = 'NEW' AND enumtypid = (SELECT oid FROM pg_type WHERE typname = 'payment_status');
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

--refactor refund table
--drop unused columns
ALTER TABLE sht.refund DROP COLUMN paid;
-- rename updated_at to succeeded_at
ALTER TABLE sht.refund RENAME COLUMN update_at TO succeeded_at;
ALTER TABLE sht.refund ALTER COLUMN succeeded_at DROP DEFAULT;

-- refactor payout table
ALTER TABLE sht.payout DROP COLUMN cor_account;
-- canceled -> cancelled
UPDATE pg_enum SET enumlabel = 'CANCELLED'
WHERE enumlabel = 'CANCELED' AND enumtypid = (SELECT oid FROM pg_type WHERE typname = 'payout_status');
-- change payout_type names
UPDATE pg_enum SET enumlabel = 'bank_card'
WHERE enumlabel = 'CardPayout' AND enumtypid = (SELECT oid FROM pg_type WHERE typname = 'payout_type');
UPDATE pg_enum SET enumlabel = 'bank_account'
WHERE enumlabel = 'AccountPayout' AND enumtypid = (SELECT oid FROM pg_type WHERE typname = 'payout_type');

-- shop_meta table
CREATE TABLE sht.shop_meta (
  party_id CHARACTER VARYING           NOT NULL,
  shop_id  CHARACTER VARYING           NOT NULL,
  wtime    TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
  CONSTRAINT shop_meta_pkey PRIMARY KEY (party_id, shop_id)
);

-- account type
CREATE TYPE sht.ACCOUNT_TYPE AS ENUM ('merchant', 'provider', 'system', 'external');

-- cash_flow_posting table
CREATE TABLE sht.cash_flow_posting (
  id                BIGSERIAL                   NOT NULL,
  payout_id         BIGINT                      NOT NULL,
  plan_id           CHARACTER VARYING           NOT NULL,
  batch_id          BIGINT                      NOT NULL,
  from_account_id   BIGINT                      NOT NULL,
  from_account_type sht.ACCOUNT_TYPE            NOT NULL,
  to_account_id     BIGINT                      NOT NULL,
  to_account_type   sht.ACCOUNT_TYPE            NOT NULL,
  amount            BIGINT                      NOT NULL,
  currency_code     CHARACTER VARYING           NOT NULL,
  description       CHARACTER VARYING,
  created_at        TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
  CONSTRAINT posting_pkey PRIMARY KEY (id)
);
