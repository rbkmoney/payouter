ALTER TABLE sht.report RENAME payoutIds TO payout_ids;

ALTER TABLE sht.report ADD COLUMN subject CHARACTER VARYING;
UPDATE sht.report SET subject = 'Выплаты для резидентов, сгенерированные ' || to_char(created_at, 'dd.MM.yyyy');
ALTER TABLE sht.report ALTER COLUMN subject SET NOT NULL;

ALTER TABLE sht.report ADD COLUMN encoding CHARACTER VARYING;
UPDATE sht.report SET encoding = 'Windows-1251';
ALTER TABLE sht.report ALTER COLUMN encoding SET NOT NULL;
