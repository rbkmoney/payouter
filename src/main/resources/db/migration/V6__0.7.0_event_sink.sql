ALTER TABLE sht.payout
  ADD COLUMN account_legal_agreement_id CHARACTER VARYING;
ALTER TABLE sht.payout
  ADD COLUMN account_legal_agreement_signed_at TIMESTAMP WITHOUT TIME ZONE;

CREATE SEQUENCE payout_event_id_sequence START WITH 1000;

CREATE TABLE sht.payout_event (
  event_id                                 BIGINT DEFAULT nextval(
      'payout_event_id_sequence')                                            NOT NULL,
  event_created_at                         TIMESTAMP WITHOUT TIME ZONE       NOT NULL,
  event_type                               CHARACTER VARYING                 NOT NULL,
  payout_id                                CHARACTER VARYING                 NOT NULL,
  payout_party_id                          CHARACTER VARYING,
  payout_shop_id                           CHARACTER VARYING,
  payout_created_at                        TIMESTAMP WITHOUT TIME ZONE,
  payout_status                            CHARACTER VARYING,
  payout_status_cancel_details             CHARACTER VARYING,
  payout_type                              CHARACTER VARYING,
  payout_cash_flow                         CHARACTER VARYING,
  payout_paid_details_type                 CHARACTER VARYING,
  payout_card_token                        CHARACTER VARYING,
  payout_card_payment_system               CHARACTER VARYING,
  payout_card_bin                          CHARACTER VARYING,
  payout_card_masked_pan                   CHARACTER VARYING,
  payout_card_provider_name                CHARACTER VARYING,
  payout_card_provider_transaction_id      CHARACTER VARYING,
  payout_account_id                        CHARACTER VARYING,
  payout_account_bank_name                 CHARACTER VARYING,
  payout_account_bank_post_id              CHARACTER VARYING,
  payout_account_bank_bik                  CHARACTER VARYING,
  payout_account_inn                       CHARACTER VARYING,
  payout_account_purpose                   CHARACTER VARYING,
  payout_account_legal_agreement_id        CHARACTER VARYING,
  payout_account_legal_agreement_signed_at TIMESTAMP WITHOUT TIME ZONE,
  user_id                                  CHARACTER VARYING,
  user_type                                CHARACTER VARYING,
  CONSTRAINT payout_event_pkey PRIMARY KEY (event_id)
);
