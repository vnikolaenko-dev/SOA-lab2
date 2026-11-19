import type { SpaceMarineOutput } from '~/api/generated/space-marines/models';

export type Marine = SpaceMarineOutput;

export const weaponLabels: Record<Marine['weaponType'], string> = {
  COMBI_FLAMER: 'Комби-огнемёт',
  GRENADE_LAUNCHER: 'Гранатомёт',
  HEAVY_FLAMER: 'Тяжёлый огнемёт',
};

export const meleeLabels: Record<NonNullable<Marine['meleeWeapon']>, string> = {
  CHAIN_SWORD: 'Цепной меч',
  CHAIN_AXE: 'Цепной топор',
  POWER_BLADE: 'Силовой клинок',
  POWER_FIST: 'Силовой кулак',
};

export const formatDate = (value: string) => new Intl.DateTimeFormat('ru-RU', {
  dateStyle: 'medium', timeStyle: 'short',
}).format(new Date(value));
