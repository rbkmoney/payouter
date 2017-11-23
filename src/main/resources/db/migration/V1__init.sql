CREATE SCHEMA IF NOT EXISTS sht;

CREATE TABLE sht.invoice (
  id       CHARACTER VARYING NOT NULL,
  party_id CHARACTER VARYING NOT NULL,
  shop_id  CHARACTER VARYING NOT NULL,
  CONSTRAINT invoice_pkey PRIMARY KEY (id)
);

CREATE TABLE sht.payment (
  id                    BIGSERIAL                   NOT NULL,
  event_id              BIGINT                      NOT NULL,
  invoice_id            CHARACTER VARYING           NOT NULL,
  payment_id            CHARACTER VARYING           NOT NULL,
  party_id              CHARACTER VARYING           NOT NULL,
  shop_id               CHARACTER VARYING           NOT NULL,
  provider_id           CHARACTER VARYING           NOT NULL,
  status                CHARACTER VARYING           NOT NULL  DEFAULT 'NEW',
  payment_status        CHARACTER VARYING           NOT NULL,
  payout_id             BIGINT,
  amount                BIGINT                      NOT NULL,
  provider_commission   BIGINT                      NOT NULL,
  rbk_commission        BIGINT                      NOT NULL,
  external_commission   BIGINT,
  currency_code         CHARACTER VARYING           NOT NULL,
  captured_at           TIMESTAMP WITHOUT TIME ZONE,
  masked_pan            CHARACTER VARYING,
  test                  BOOLEAN                               DEFAULT FALSE,
  wtime                 TIMESTAMP WITHOUT TIME ZONE NOT NULL  DEFAULT now(),
  adjusted_by           CHARACTER VARYING,
  is_adjustment_payment BOOLEAN                               DEFAULT FALSE,
CONSTRAINT payment_pkey PRIMARY KEY (id)
);

CREATE TYPE sht.PAYOUTSTATUS AS ENUM ('CREATED', 'READY', 'ACCEPTED', 'CANCELED');

CREATE TABLE sht.payout (
  id               BIGSERIAL                   NOT NULL,
  status           sht.PAYOUTSTATUS            NOT NULL DEFAULT 'CREATED',
  from_time        TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  to_time          TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  ones_report      CHARACTER VARYING,
  wtime            TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
  accepted_by_ones BOOL                        NOT NULL DEFAULT FALSE,
  CONSTRAINT payout_pkey PRIMARY KEY (id)
);

CREATE TYPE sht.ADJUSTMENT_STATUS AS ENUM ('PENDING', 'CAPTURED', 'CANCELLED');

CREATE TABLE sht.adjustment (
  id                               BIGSERIAL                   NOT NULL,
  event_id                         BIGINT                      NOT NULL,
  shop_id                          CHARACTER VARYING           NOT NULL,
  party_id                         CHARACTER VARYING           NOT NULL,
  invoice_id                       CHARACTER VARYING           NOT NULL,
  payment_id                       CHARACTER VARYING           NOT NULL,
  adjustment_id                    CHARACTER VARYING           NOT NULL,
  adjustment_status                sht.ADJUSTMENT_STATUS       NOT NULL DEFAULT 'PENDING',
  created_at                       TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
  domain_revision                  BIGINT                      NOT NULL,
  reason                           CHARACTER VARYING           NOT NULL,
  test_shop                        BOOLEAN                              DEFAULT FALSE,

  new_amount                       BIGINT                      NOT NULL,
  new_provider_commission          BIGINT                      NOT NULL,
  new_rbk_commission               BIGINT                      NOT NULL,
  new_external_commission          BIGINT                      NOT NULL,

  inversed_old_amount              BIGINT                      NOT NULL,
  inversed_old_provider_commission BIGINT                      NOT NULL,
  inversed_old_rbk_commission      BIGINT                      NOT NULL,
  inversed_old_external_commission BIGINT                      NOT NULL,

  update_at                        TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
  CONSTRAINT adjustment_pkey PRIMARY KEY (id)
);

