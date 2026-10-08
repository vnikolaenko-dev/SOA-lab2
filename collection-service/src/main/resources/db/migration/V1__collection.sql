CREATE TABLE space_marine (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    creation_date         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    name                  TEXT COLLATE "C" NOT NULL,
    coordinate_x          DOUBLE PRECISION NOT NULL,
    coordinate_y          INTEGER NOT NULL,
    health                INTEGER NOT NULL,
    achievements          TEXT,
    weapon_type           TEXT NOT NULL,
    melee_weapon          TEXT,
    chapter_name          TEXT NOT NULL,
    chapter_parent_legion TEXT,
    chapter_world         TEXT NOT NULL,
    CHECK (length(name) > 0),
    CHECK (health > 0),
    CHECK (coordinate_y > -630),
    CHECK (length(chapter_name) > 0),
    CHECK (weapon_type IN ('COMBI_FLAMER', 'GRENADE_LAUNCHER', 'HEAVY_FLAMER')),
    CHECK (melee_weapon IN ('CHAIN_SWORD', 'CHAIN_AXE', 'POWER_BLADE', 'POWER_FIST'))
);

CREATE INDEX space_marine_health_idx ON space_marine(health);

CREATE INDEX space_marine_weapon_idx ON space_marine(weapon_type);
