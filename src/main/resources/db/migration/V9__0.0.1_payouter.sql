--drop unused table
DROP TABLE sht.payout_old;
DROP TYPE sht.PAYOUTSTATUS_OLD;

-- refactor payment table
DELETE FROM sht.payment WHERE test = TRUE;
ALTER TABLE sht.payment DROP COLUMN test;
-- rename commission to fee
ALTER TABLE sht.payment RENAME COLUMN provider_commission TO provider_fee;
ALTER TABLE sht.payment RENAME COLUMN external_commission TO external_fee;
-- drop unused columns
ALTER TABLE sht.payment DROP COLUMN paid;
ALTER TABLE sht.payment DROP COLUMN payment_status;
ALTER TABLE sht.payment DROP COLUMN inn;
-- rename wtime to created_at, drop default
ALTER TABLE sht.payment ALTER COLUMN wtime DROP DEFAULT;
ALTER TABLE sht.payment RENAME COLUMN wtime TO created_at;
-- add terminal id
ALTER TABLE sht.payment ADD COLUMN terminal_id INT;
--add domain revision
ALTER TABLE sht.payment ADD COLUMN domain_revision INT;

--refactor adjustment table
--drop unused columns
ALTER TABLE sht.adjustment DROP COLUMN paid;
ALTER TABLE sht.adjustment DROP COLUMN payment_amount;
ALTER TABLE sht.adjustment DROP COLUMN payment_fee;
-- rename commission to fee
ALTER TABLE sht.adjustment RENAME COLUMN new_provider_commission to new_provider_fee;
ALTER TABLE sht.adjustment RENAME COLUMN new_external_commission to new_external_fee;

--refactor refund table
--drop unused columns
ALTER TABLE sht.refund DROP COLUMN paid;


