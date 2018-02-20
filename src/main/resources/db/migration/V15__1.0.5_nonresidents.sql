-- Payout event
ALTER TABLE sht.payout_event ADD COLUMN payout_account_type CHARACTER VARYING;
ALTER TABLE sht.payout_event ADD COLUMN payout_account_bank_address CHARACTER VARYING;
ALTER TABLE sht.payout_event ADD COLUMN payout_account_bank_bic CHARACTER VARYING;
ALTER TABLE sht.payout_event ADD COLUMN payout_account_bank_iban CHARACTER VARYING;
ALTER TABLE sht.payout_event ADD COLUMN payout_account_legal_name CHARACTER VARYING;
ALTER TABLE sht.payout_event ADD COLUMN payout_account_trading_name CHARACTER VARYING;
ALTER TABLE sht.payout_event ADD COLUMN payout_account_registered_address CHARACTER VARYING;
ALTER TABLE sht.payout_event ADD COLUMN payout_account_actual_address CHARACTER VARYING;
ALTER TABLE sht.payout_event ADD COLUMN payout_account_registered_number CHARACTER VARYING;
ALTER TABLE sht.payout_event RENAME COLUMN payout_account_bank_bik TO payout_account_bank_local_code;

-- Payout
CREATE TYPE sht.payout_account_type AS ENUM ('russian_payout_account', 'international_payout_account');
ALTER TABLE sht.payout ADD COLUMN account_type sht.payout_account_type;
UPDATE sht.payout SET account_type = 'russian_payout_account' WHERE account_type IS NULL;

ALTER TABLE sht.payout ADD COLUMN fee BIGINT DEFAULT 0;
ALTER TABLE sht.payout ADD COLUMN bank_address CHARACTER VARYING;
ALTER TABLE sht.payout ADD COLUMN bank_bic CHARACTER VARYING;
ALTER TABLE sht.payout ADD COLUMN bank_iban CHARACTER VARYING;
ALTER TABLE sht.payout ADD COLUMN account_legal_name CHARACTER VARYING;
ALTER TABLE sht.payout ADD COLUMN account_trading_name CHARACTER VARYING;
ALTER TABLE sht.payout ADD COLUMN account_registered_address CHARACTER VARYING;
ALTER TABLE sht.payout ADD COLUMN account_actual_address CHARACTER VARYING;
ALTER TABLE sht.payout ADD COLUMN account_registered_number CHARACTER VARYING;
ALTER TABLE sht.payout RENAME COLUMN payout_type TO type;
ALTER TABLE sht.payout RENAME COLUMN bank_bik TO bank_local_code;

