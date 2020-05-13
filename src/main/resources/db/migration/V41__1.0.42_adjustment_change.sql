alter table sht.adjustment add column amount BIGINT DEFAULT 0;

update sht.adjustment
set amount = payment_fee - new_fee + payment_guarantee_deposit - new_guarantee_deposit;

alter table sht.adjustment
  drop column new_amount,
  drop column new_provider_fee,
  drop column new_fee,
  drop column new_external_fee,
  drop column payment_amount,
  drop column payment_fee,
  drop column payment_guarantee_deposit,
  drop column new_guarantee_deposit;
