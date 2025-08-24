CREATE TABLE IF NOT EXISTS strava_connection (
  id           BIGSERIAL PRIMARY KEY,
  user_id      BIGINT NOT NULL UNIQUE REFERENCES app_user(id),
  athlete_id   BIGINT NOT NULL,
  access_token TEXT   NOT NULL,
  refresh_token TEXT  NOT NULL,
  expires_at   BIGINT NOT NULL,
  created_at   TIMESTAMP NOT NULL DEFAULT now(),
  updated_at   TIMESTAMP NOT NULL DEFAULT now()
);

CREATE OR REPLACE FUNCTION trg_set_updated_at()
RETURNS TRIGGER AS $$
BEGIN NEW.updated_at = now(); RETURN NEW; END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS strava_connection_set_updated_at ON strava_connection;
CREATE TRIGGER strava_connection_set_updated_at
BEFORE UPDATE ON strava_connection
FOR EACH ROW EXECUTE PROCEDURE trg_set_updated_at();
