import { z } from 'zod';
import { SpaceMarineInput } from '~/api/generated/space-marines/models';
import type { Marine } from '~/lib/marine-display';

const fields = {
  name: z.string(),
  x: z.string(),
  y: z.string(),
  health: z.string(),
  achievements: z.string(),
  weaponType: z.string(),
  meleeWeapon: z.string(),
  chapterName: z.string(),
  parentLegion: z.string(),
  world: z.string(),
};

export type MarineFormValues = z.infer<typeof marineFormSchema>;

export function marineValues(marine?: Marine): MarineFormValues {
  return {
    name: marine?.name ?? '',
    x: String(marine?.coordinates.x ?? ''),
    y: String(marine?.coordinates.y ?? ''),
    health: String(marine?.health ?? ''),
    achievements: marine?.achievements ?? '',
    weaponType: marine?.weaponType ?? 'COMBI_FLAMER',
    meleeWeapon: marine?.meleeWeapon ?? '',
    chapterName: marine?.chapter.name ?? '',
    parentLegion: marine?.chapter.parentLegion ?? '',
    world: marine?.chapter.world ?? '',
  };
}

export function marineInput(values: z.infer<z.ZodObject<typeof fields>>) {
  return {
    name: values.name.trim(),
    coordinates: {
      x: values.x === '' ? undefined : Number(values.x),
      y: values.y === '' ? undefined : Number(values.y),
    },
    health: values.health === '' ? undefined : Number(values.health),
    achievements: values.achievements || null,
    weaponType: values.weaponType,
    meleeWeapon: values.meleeWeapon || null,
    chapter: {
      name: values.chapterName.trim(),
      parentLegion: values.parentLegion || null,
      world: values.world.trim(),
    },
  };
}

const inputPaths: Record<string, keyof typeof fields> = {
  'coordinates.x': 'x',
  'coordinates.y': 'y',
  'chapter.name': 'chapterName',
  'chapter.parentLegion': 'parentLegion',
  'chapter.world': 'world',
};

function marineFieldFromPath(path: string): keyof typeof fields | undefined {
  return (
    inputPaths[path] ??
    (path in fields ? (path as keyof typeof fields) : undefined)
  );
}

export const marineFormSchema = z
  .object(fields)
  .superRefine((values, context) => {
    const result = SpaceMarineInput.safeParse(marineInput(values));
    if (result.success) return;
    for (const issue of result.error.issues) {
      const field = marineFieldFromPath(issue.path.join('.'));
      if (field)
        context.addIssue({
          code: 'custom',
          path: [field],
          message: issue.message,
        });
    }
  });
