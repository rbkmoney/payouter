ALTER TABLE sht.report RENAME payoutIds TO payout_ids;
ALTER TABLE sht.report ADD COLUMN encoding CHARACTER VARYING NOT NULL;