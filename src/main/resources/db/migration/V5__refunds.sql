CREATE TYPE sht.REFUND_STATUS AS ENUM ('PENDING', 'SUCCEEDED', 'FAILED');

CREATE TABLE sht.refund (
  id         BIGSERIAL                   NOT NULL,
  event_id   BIGINT                      NOT NULL,
  shop_id    CHARACTER VARYING           NOT NULL,
  party_id   CHARACTER VARYING           NOT NULL,
  invoice_id CHARACTER VARYING           NOT NULL,
  payment_id CHARACTER VARYING           NOT NULL,
  refund_id  CHARACTER VARYING           NOT NULL,
  status     sht.REFUND_STATUS           NOT NULL DEFAULT 'PENDING',
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  reason     CHARACTER VARYING,
  amount     BIGINT                      NOT NULL,
  fee        BIGINT                      NOT NULL,
  paid       BOOLEAN                     NOT NULL DEFAULT FALSE,
  payout_id  BIGINT,

  update_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
  CONSTRAINT refund_pkey PRIMARY KEY (id)
);