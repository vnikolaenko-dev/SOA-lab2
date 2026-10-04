-- Перенос ранее сохранённых десантников из JSONB в столбцы JPA с сохранением данных.
ALTER TABLE space_marine
    ADD COLUMN name TEXT COLLATE "C",
    ADD COLUMN coordinate_x DOUBLE PRECISION,
    ADD COLUMN coordinate_y INTEGER,
    ADD COLUMN health INTEGER,
    ADD COLUMN achievements TEXT,
    ADD COLUMN weapon_type TEXT,
    ADD COLUMN melee_weapon TEXT,
    ADD COLUMN chapter_name TEXT,
    ADD COLUMN chapter_parent_legion TEXT,
    ADD COLUMN chapter_world TEXT;

UPDATE space_marine SET
    name = data->>'name',
    coordinate_x = (data->'coordinates'->>'x')::double precision,
    coordinate_y = (data->'coordinates'->>'y')::integer,
    health = (data->>'health')::integer,
    achievements = data->>'achievements',
    weapon_type = data->>'weaponType',
    melee_weapon = data->>'meleeWeapon',
    chapter_name = data->'chapter'->>'name',
    chapter_parent_legion = data->'chapter'->>'parentLegion',
    chapter_world = data->'chapter'->>'world';

ALTER TABLE space_marine
    ALTER COLUMN name SET NOT NULL,
    ALTER COLUMN coordinate_x SET NOT NULL,
    ALTER COLUMN coordinate_y SET NOT NULL,
    ALTER COLUMN health SET NOT NULL,
    ALTER COLUMN weapon_type SET NOT NULL,
    ALTER COLUMN chapter_name SET NOT NULL,
    ALTER COLUMN chapter_world SET NOT NULL,
    DROP COLUMN data,
    ADD CHECK (length(name) > 0),
    ADD CHECK (health > 0),
    ADD CHECK (coordinate_y > -630),
    ADD CHECK (length(chapter_name) > 0),
    ADD CHECK (weapon_type IN ('COMBI_FLAMER', 'GRENADE_LAUNCHER', 'HEAVY_FLAMER')),
    ADD CHECK (melee_weapon IN ('CHAIN_SWORD', 'CHAIN_AXE', 'POWER_BLADE', 'POWER_FIST'));
CREATE INDEX space_marine_health_idx ON space_marine(health);
CREATE INDEX space_marine_weapon_idx ON space_marine(weapon_type);
