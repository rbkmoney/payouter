drop index if exists sht.payment_party_shop_contract;

create unique index if not exists payment_ukey ON sht.payment USING btree (invoice_id, payment_id);
create unique index if not exists adjustment_ukey ON sht.adjustment USING btree (invoice_id, payment_id, adjustment_id);
create unique index if not exists refund_ukey ON sht.refund USING btree (invoice_id, payment_id, refund_id);
