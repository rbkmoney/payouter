CREATE INDEX CONCURRENTLY payment_invoice_id_idx
  ON sht.payment USING BTREE (invoice_id);