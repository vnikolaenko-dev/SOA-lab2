import { createServerFn } from '@tanstack/react-start';
import { z } from 'zod';
import { getAddSpaceMarineUrl } from '~/api/generated/space-marines/client';
import { SpaceMarine } from '~/api/generated/space-marines/models';
import { jsonBody, request, type ServiceError } from './http';

export const MAX_BULK_MARINES = 500;

export type BulkMarineResult = {
  requested: number;
  created: number;
  error?: ServiceError;
};

export const createManyMarines = createServerFn({ method: 'POST' })
  .validator(z.object({ count: z.number().int().min(1).max(MAX_BULK_MARINES) }))
  .handler(async ({ data }): Promise<BulkMarineResult> => {
    const { generateMarine } = await import('./marine-generator');
    let created = 0;

    for (let index = 0; index < data.count; index++) {
      const result = await request(
        'marines',
        getAddSpaceMarineUrl(),
        SpaceMarine,
        { method: 'POST', ...jsonBody(generateMarine()) },
      );
      if (!result.ok) {
        return { requested: data.count, created, error: result.error };
      }
      created++;
    }

    return { requested: data.count, created };
  });
