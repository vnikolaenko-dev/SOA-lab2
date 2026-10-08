CREATE TABLE boarding (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    marine_id   BIGINT CHECK (marine_id > 0),
    starship_id BIGINT NOT NULL CHECK (starship_id > 0),
    loaded_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX boarding_starship_idx ON boarding(starship_id);
ALTER TABLE boarding ADD CONSTRAINT boarding_marine_unique UNIQUE (marine_id);
