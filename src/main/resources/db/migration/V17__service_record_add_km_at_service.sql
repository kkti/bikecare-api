-- V17__service_record_add_km_at_service.sql
ALTER TABLE service_record
    ADD COLUMN km_at_service DOUBLE PRECISION;
