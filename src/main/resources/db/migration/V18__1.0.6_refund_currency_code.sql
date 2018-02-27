ALTER TABLE sht.refund ADD COLUMN currency_code CHARACTER VARYING;
UPDATE sht.refund as s1 SET currency_code =
(select currency_code from sht.payment as s2 where s1.invoice_id = s2.invoice_id AND s1.payment_id = s2.payment_id);
ALTER TABLE sht.refund ALTER COLUMN currency_code SET NOT NULL;