ALTER TABLE sht.payout
  ADD COLUMN bank_aba_rtn CHARACTER VARYING;
ALTER TABLE sht.payout
  ADD COLUMN bank_country_code CHARACTER VARYING;

ALTER TABLE sht.payout
  ADD COLUMN int_corr_bank_account CHARACTER VARYING;
ALTER TABLE sht.payout
  ADD COLUMN int_corr_bank_name CHARACTER VARYING;
ALTER TABLE sht.payout
  ADD COLUMN int_corr_bank_address CHARACTER VARYING;
ALTER TABLE sht.payout
  ADD COLUMN int_corr_bank_bic CHARACTER VARYING;
ALTER TABLE sht.payout
  ADD COLUMN int_corr_bank_iban CHARACTER VARYING;
ALTER TABLE sht.payout
  ADD COLUMN int_corr_bank_aba_rtn CHARACTER VARYING;
ALTER TABLE sht.payout
  ADD COLUMN int_corr_bank_country_code CHARACTER VARYING;

ALTER TABLE sht.payout_event
  ADD COLUMN payout_account_bank_aba_rtn CHARACTER VARYING;
ALTER TABLE sht.payout_event
  ADD COLUMN payout_account_bank_country_code CHARACTER VARYING;

ALTER TABLE sht.payout_event
  ADD COLUMN payout_international_correspondent_account_bank_name CHARACTER VARYING;
ALTER TABLE sht.payout_event
  ADD COLUMN payout_international_correspondent_account_bank_address CHARACTER VARYING;
ALTER TABLE sht.payout_event
  ADD COLUMN payout_international_correspondent_account_bank_bic CHARACTER VARYING;
ALTER TABLE sht.payout_event
  ADD COLUMN payout_international_correspondent_account_bank_iban CHARACTER VARYING;
ALTER TABLE sht.payout_event
  ADD COLUMN payout_international_correspondent_account_bank_aba_rtn CHARACTER VARYING;
ALTER TABLE sht.payout_event
  ADD COLUMN payout_international_correspondent_account_bank_country_code CHARACTER VARYING;
