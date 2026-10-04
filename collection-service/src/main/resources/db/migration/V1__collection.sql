CREATE TABLE space_marine (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 creation_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 data JSONB NOT NULL,
 CHECK ((data->>'health')::integer > 0),
 CHECK ((data->'coordinates'->>'y')::integer > -630)
);
