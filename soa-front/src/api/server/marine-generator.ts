import { faker } from '@faker-js/faker';
import {
  SpaceMarineInput,
  type SpaceMarineInputOutput,
} from '~/api/generated/space-marines/models';

const meleeWeapons = [
  'CHAIN_SWORD',
  'CHAIN_AXE',
  'POWER_BLADE',
  'POWER_FIST',
  null,
] as const;

export function generateMarine(): SpaceMarineInputOutput {
  return SpaceMarineInput.parse({
    name: faker.person.fullName(),
    coordinates: {
      x: faker.number.float({ min: -1000, max: 1000, fractionDigits: 2 }),
      y: faker.number.int({ min: -629, max: 1000 }),
    },
    health: faker.number.int({ min: 1, max: 1000 }),
    achievements: faker.lorem.sentence(),
    weaponType: faker.helpers.arrayElement(
      SpaceMarineInput.shape.weaponType.options,
    ),
    meleeWeapon: faker.helpers.arrayElement(meleeWeapons),
    chapter: {
      name: faker.company.name(),
      parentLegion: faker.company.name(),
      world: faker.location.city(),
    },
  });
}
