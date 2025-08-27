ALTER TABLE service_record
ALTER COLUMN km_at_service
  TYPE numeric(38,2)
  USING km_at_service::numeric(38,2);
