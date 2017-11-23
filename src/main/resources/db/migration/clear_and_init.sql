TRUNCATE TABLE public.schema_version CONTINUE IDENTITY RESTRICT;
 DROP TABLE sht.data;
 DROP TABLE sht.payment;
 DROP TABLE sht.payout;
 DROP TABLE sht.invoice;

truncate sht.payment;
truncate sht.data;

INSERT INTO sht.payment (
  id, event_id, invoice_id, merchant_id,
  shop_id, customer_id, amount, provider_commission, rbk_commission,
  currency_code)
VALUES (125, 236, '19', '11',
        '7', '24', 10000, 300, 500,
        '810');

INSERT INTO sht.payment (
  id, event_id, invoice_id, merchant_id,
  shop_id, customer_id, amount, provider_commission, rbk_commission,
  currency_code)
VALUES (126, 237, '20', '11',
        '7', '25', 5000, 150, 250,
        '810');



