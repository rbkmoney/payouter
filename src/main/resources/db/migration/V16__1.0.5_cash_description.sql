-- cash flow type
CREATE TYPE sht.CASH_FLOW_TYPE AS ENUM ('payment', 'refund', 'adjustment');

-- cash_flow_description table
CREATE TABLE sht.cash_flow_description (
  id                BIGSERIAL                   NOT NULL,
  payout_id         CHARACTER VARYING           NOT NULL,
  cash_flow_type    sht.CASH_FLOW_TYPE          NOT NULL,
  count             INT                         NOT NULL,
  amount            BIGINT                      NOT NULL,
  fee               BIGINT                      NOT NULL,
  currency_code     CHARACTER VARYING           NOT NULL,
  from_time         TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  to_time           TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  CONSTRAINT cash_flow_description_pkey PRIMARY KEY (id)
);