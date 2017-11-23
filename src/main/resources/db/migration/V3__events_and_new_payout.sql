-- <Cleaning>

ALTER TABLE sht.payout
  RENAME TO payout_old;

ALTER TYPE sht.PAYOUTSTATUS
RENAME TO PAYOUTSTATUS_OLD;

-- </Cleaning>

CREATE TYPE sht.PAYOUT_STATUS AS ENUM ('UNPAID', 'PAID', 'CONFIRMED', 'CANCELED');

CREATE TYPE sht.PAYOUT_TYPE AS ENUM ('CardPayout', 'AccountPayout');

CREATE SEQUENCE payout_id_sequence START WITH 1000;

CREATE TABLE sht.payout (
  id                BIGINT PRIMARY KEY  DEFAULT nextval('payout_id_sequence')               NOT NULL UNIQUE,
  party_id          CHARACTER VARYING                                                       NOT NULL,
  shop_id           CHARACTER VARYING                                                       NOT NULL,
  created_at        TIMESTAMP WITHOUT TIME ZONE                                             NOT NULL,
  from_time         TIMESTAMP WITHOUT TIME ZONE,
  to_time           TIMESTAMP WITHOUT TIME ZONE,
  status            sht.PAYOUT_STATUS                                                       NOT NULL,
  payout_type       sht.PAYOUT_TYPE                                                         NOT NULL,
  -- fields for cash flow
  -- сумма которая будет выведена. amount + rbk_fee = то что будет списано со счета магазина.
  amount            BIGINT,
  -- сумма которая была выведена на карты, до этой выплаты
  shop_acc          BIGINT,
  shop_payout_acc   BIGINT,
  currency_code     CHARACTER VARYING,

  --bank account fields
  bank_account      CHARACTER VARYING,
  cor_account       CHARACTER VARYING,
  bank_bik          CHARACTER VARYING,
  bank_name         CHARACTER VARYING,
  bank_post_account CHARACTER VARYING,
  inn               CHARACTER VARYING,
  purpose           CHARACTER VARYING,
  description       CHARACTER VARYING
);






