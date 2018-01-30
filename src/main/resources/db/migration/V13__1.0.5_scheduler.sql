-- scheduler
CREATE TABLE sht.job_meta (
  party_id               CHARACTER VARYING           NOT NULL,
  contract_id            CHARACTER VARYING           NOT NULL,
  payout_tool_id         CHARACTER VARYING           NOT NULL,
  calendar_id            INT                         NOT NULL,
  scheduler_id           INT,
  wtime                  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  CONSTRAINT job_meta_pkey PRIMARY KEY (party_id, contract_id, payout_tool_id)
);