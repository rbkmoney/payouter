-- account type
DROP TYPE sht.ACCOUNT_TYPE;
CREATE TYPE sht.ACCOUNT_TYPE AS ENUM ('MERCHANT_SETTLEMENT', 'MERCHANT_GUARANTEE', 'MERCHANT_PAYOUT', 'PROVIDER_SETTLEMENT', 'SYSTEM_SETTLEMENT', 'EXTERNAL_INCOME', 'EXTERNAL_OUTCOME');

-- cash_flow_posting table
CREATE TABLE sht.cash_flow_posting (
  id                BIGSERIAL                   NOT NULL,
  payout_id         CHARACTER VARYING           NOT NULL,
  plan_id           CHARACTER VARYING           NOT NULL,
  batch_id          BIGINT                      NOT NULL,
  from_account_id   BIGINT                      NOT NULL,
  from_account_type sht.ACCOUNT_TYPE            NOT NULL,
  to_account_id     BIGINT                      NOT NULL,
  to_account_type   sht.ACCOUNT_TYPE            NOT NULL,
  amount            BIGINT                      NOT NULL,
  currency_code     CHARACTER VARYING           NOT NULL,
  description       CHARACTER VARYING,
  created_at        TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
  CONSTRAINT posting_pkey PRIMARY KEY (id)
);

-- insert old data in a new format
insert into sht.cash_flow_posting (payout_id, plan_id, batch_id, from_account_id, from_account_type, to_account_id, to_account_type, amount, currency_code, description, created_at)
  select
    payout_id as payout_id,
    'payout_' || payout_id as plan_id,
    1 as batch_id,
    cast(json_extract_path_text(json_array_elements(payout_cash_flow::json), 'source', 'account_id') as bigint) as from_account_id,
    'MERCHANT_SETTLEMENT' as from_account_type,
    cast(json_extract_path_text(json_array_elements(payout_cash_flow::json), 'destination', 'account_id') as bigint) as to_account_id,
    'MERCHANT_PAYOUT' as to_account_type,
    cast(json_extract_path_text(json_array_elements(payout_cash_flow::json), 'volume', 'amount') as bigint) as amount,
    json_extract_path_text(json_array_elements(payout_cash_flow::json), 'volume', 'currency', 'symbolic_code') as currency_code,
    'Payout: ' || payout_id as description,
    event_created_at as created_at
  FROM sht.payout_event
  where event_type = 'payout_created'
  order by event_id asc;
