-- cash flow type
CREATE TYPE sht.CASH_FLOW_TYPE AS ENUM ('payment', 'fee', 'refund', 'adjustment', 'guarantee');

-- cash_flow_description table
CREATE TABLE sht.cash_flow_description (
  id                BIGSERIAL                   NOT NULL,
  payout_id         BIGINT                      NOT NULL,
  cash_flow_type    sht.CASH_FLOW_TYPE          NOT NULL,
  count             INT                         NOT NULL,
  amount            BIGINT                      NOT NULL,
  currency_code     CHARACTER VARYING           NOT NULL,
  description       CHARACTER VARYING,
  CONSTRAINT cash_flow_description_pkey PRIMARY KEY (id)
);
