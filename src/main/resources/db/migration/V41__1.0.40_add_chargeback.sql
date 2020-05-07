CREATE TYPE sht.chargeback_status AS ENUM ('PENDING', 'SUCCEEDED', 'REJECTED', 'CANCELLED');

CREATE TYPE sht.chargeback_category AS ENUM ('fraud', 'dispute', 'authorisation', 'processing_error');

CREATE TYPE sht.chargeback_stage AS ENUM ('chargeback', 'pre_arbitration', 'arbitration');

CREATE TABLE sht.chargeback
(
    id                 BIGSERIAL                   NOT NULL,
    event_id           BIGINT                      NOT NULL,
    shop_id            CHARACTER VARYING           NOT NULL,
    party_id           CHARACTER VARYING           NOT NULL,
    invoice_id         CHARACTER VARYING           NOT NULL,
    payment_id         CHARACTER VARYING           NOT NULL,
    chargeback_id      CHARACTER VARYING           NOT NULL,
    payout_id          CHARACTER VARYING,
    status             sht.chargeback_status       NOT NULL DEFAULT 'PENDING',
    created_at         TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    reason             CHARACTER VARYING,
    reason_category    sht.chargeback_category     NOT NULL,
    domain_revision    BIGINT                      NOT NULL,
    party_revision     BIGINT,
    amount             BIGINT                      NOT NULL,
    currency_code      CHARACTER VARYING           NOT NULL,
    levy_amount        BIGINT                      NOT NULL,
    levy_currency_code CHARACTER VARYING           NOT NULL,
    fee                BIGINT                      NOT NULL DEFAULT 0,
    chargeback_stage   sht.chargeback_stage        NOT NULL,
    succeeded_at       TIMESTAMP WITHOUT TIME ZONE,

    CONSTRAINT chargeback_pkey PRIMARY KEY (id)
);

CREATE INDEX chargeback_payout_id_idx ON sht.chargeback USING btree (payout_id);

CREATE UNIQUE INDEX IF NOT EXISTS chargeback_ukey ON sht.chargeback USING btree (invoice_id, payment_id, chargeback_id);
