CREATE TABLE boarding (
 marine_id BIGINT PRIMARY KEY CHECK (marine_id > 0),
 starship_id BIGINT NOT NULL CHECK (starship_id > 0),
 loaded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX boarding_starship_idx ON boarding(starship_id);
