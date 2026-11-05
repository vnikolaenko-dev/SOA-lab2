import { createServerFn } from '@tanstack/react-start';
import { z } from 'zod';
import {
  getGetStarshipSpaceMarinesUrl,
  getGetStarshipsUrl,
  getLoadSpaceMarineUrl,
  getUnloadAllUrl,
} from '~/api/generated/starships/client';
import {
  GetStarshipSpaceMarinesParams,
  GetStarshipsParams,
  PageOfSpaceMarine,
  PageOfStarship,
  SpaceMarine,
  Starship,
  UnloadAll200,
} from '~/api/generated/starships/models';
import { type ApiResult, request } from './http';

const starshipId = Starship.shape.id;
const marineId = SpaceMarine.shape.id;

async function allStarshipIds(): Promise<ApiResult<number[]>> {
  const ids: number[] = [];
  let page = 0;
  let totalPages: number;
  do {
    const result = await request(
      'starships',
      getGetStarshipsUrl({ page, size: 100 }),
      PageOfStarship,
    );
    if (!result.ok) return result;
    ids.push(...result.data.content.map((ship) => ship.id));
    totalPages = result.data.totalPages;
    page++;
  } while (page < totalPages);
  return { ok: true, data: ids.sort((a, b) => a - b) };
}

export const getAllStarshipIds = createServerFn().handler(allStarshipIds);

export const listStarships = createServerFn()
  .validator(GetStarshipsParams)
  .handler(({ data }) =>
    request('starships', getGetStarshipsUrl(data), PageOfStarship),
  );

export const getCrew = createServerFn()
  .validator(z.object({ starshipId, params: GetStarshipSpaceMarinesParams }))
  .handler(({ data }) =>
    request(
      'starships',
      getGetStarshipSpaceMarinesUrl(data.starshipId, data.params),
      PageOfSpaceMarine,
    ),
  );

export const loadMarine = createServerFn({ method: 'POST' })
  .validator(z.object({ starshipId, marineId }))
  .handler(({ data }) =>
    request(
      'starships',
      getLoadSpaceMarineUrl(data.starshipId, data.marineId),
      SpaceMarine,
      { method: 'POST' },
    ),
  );

export const loadMarineInNewStarship = createServerFn({ method: 'POST' })
  .validator(z.object({ marineId }))
  .handler(async ({ data }) => {
    const { randomInt } = await import('node:crypto');
    const existing = await allStarshipIds();
    if (!existing.ok) return existing;

    const usedIds = new Set(existing.data);
    let newId: number;
    do {
      newId = randomInt(1, 2_147_483_648);
    } while (usedIds.has(newId));

    const result = await request(
      'starships',
      getLoadSpaceMarineUrl(newId, data.marineId),
      SpaceMarine,
      { method: 'POST' },
    );
    if (!result.ok) return result;
    return {
      ok: true as const,
      data: { starshipId: newId, marine: result.data },
    };
  });

export const unloadCrew = createServerFn({ method: 'POST' })
  .validator(z.object({ starshipId }))
  .handler(({ data }) =>
    request('starships', getUnloadAllUrl(data.starshipId), UnloadAll200, {
      method: 'POST',
    }),
  );
