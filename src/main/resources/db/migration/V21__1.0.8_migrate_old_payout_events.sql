UPDATE sht.payout_event
SET payout_account_type = 'russian_payout_account'
WHERE event_type = 'payout_created' AND payout_account_type IS NULL;