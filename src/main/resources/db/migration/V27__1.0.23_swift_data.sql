ALTER TABLE sht.payout
  ADD COLUMN bank_aba_rtn CHARACTER VARYING;
ALTER TABLE sht.payout
  ADD COLUMN bank_country_code CHARACTER VARYING;

ALTER TABLE sht.payout_event
  ADD COLUMN payout_account_bank_aba_rtn CHARACTER VARYING;
ALTER TABLE sht.payout_event
  ADD COLUMN payout_account_bank_country_code CHARACTER VARYING;