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
ALTER TABLE sht.adjustment DROP COLUMN payment_amount;
ALTER TABLE sht.adjustment DROP COLUMN payment_fee;
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

