-- account type
DROP TYPE sht.ACCOUNT_TYPE;
CREATE TYPE sht.ACCOUNT_TYPE AS ENUM ('MERCHANT_SETTLEMENT', 'MERCHANT_GUARANTEE', 'PROVIDER_SETTLEMENT', 'SYSTEM_SETTLEMENT', 'EXTERNAL_INCOME', 'EXTERNAL_OUTCOME');

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