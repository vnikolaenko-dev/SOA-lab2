import { createServerFn } from '@tanstack/react-start';
import { z } from 'zod';
import {
  getAddSpaceMarineUrl,
  getDeleteByHealthUrl,
  getDeleteOneByWeaponUrl,
  getDeleteSpaceMarineUrl,
  getGetMaxNameUrl,
  getGetSpaceMarineByIdUrl,
  getGetSpaceMarinesUrl,
  getPatchSpaceMarineUrl,
  getUpdateSpaceMarineUrl,
} from '~/api/generated/space-marines/client';
import {
  DeleteByHealth200,
  GetSpaceMarinesParams,
  PageOfSpaceMarine,
  SpaceMarine,
  SpaceMarineInput,
  SpaceMarinePatchInput,
} from '~/api/generated/space-marines/models';
import { jsonBody, request } from './http';

const idSchema = z.object({ id: SpaceMarine.shape.id });
const healthSchema = z.object({ health: SpaceMarineInput.shape.health });
const weaponSchema = z.object({
  weaponType: SpaceMarineInput.shape.weaponType,
});

export const listMarines = createServerFn()
  .validator(GetSpaceMarinesParams)
  .handler(({ data }) =>
    request('marines', getGetSpaceMarinesUrl(data), PageOfSpaceMarine),
  );

export const getMarine = createServerFn()
  .validator(idSchema)
  .handler(({ data }) =>
    request('marines', getGetSpaceMarineByIdUrl(data.id), SpaceMarine),
  );

export const getMaxNameMarine = createServerFn().handler(() =>
  request('marines', getGetMaxNameUrl(), SpaceMarine),
);

export const createMarine = createServerFn({ method: 'POST' })
  .validator(SpaceMarineInput)
  .handler(({ data }) =>
    request('marines', getAddSpaceMarineUrl(), SpaceMarine, {
      method: 'POST',
      ...jsonBody(data),
    }),
  );

export const updateMarine = createServerFn({ method: 'POST' })
  .validator(z.object({ id: SpaceMarine.shape.id, marine: SpaceMarineInput }))
  .handler(({ data }) =>
    request('marines', getUpdateSpaceMarineUrl(data.id), SpaceMarine, {
      method: 'PUT',
      ...jsonBody(data.marine),
    }),
  );

export const patchMarine = createServerFn({ method: 'POST' })
  .validator(
    z.object({ id: SpaceMarine.shape.id, patch: SpaceMarinePatchInput }),
  )
  .handler(({ data }) =>
    request('marines', getPatchSpaceMarineUrl(data.id), SpaceMarine, {
      method: 'PATCH',
      ...jsonBody(data.patch),
    }),
  );

export const deleteMarine = createServerFn({ method: 'POST' })
  .validator(idSchema)
  .handler(({ data }) =>
    request('marines', getDeleteSpaceMarineUrl(data.id), z.undefined(), {
      method: 'DELETE',
    }),
  );

export const deleteOneByWeapon = createServerFn({ method: 'POST' })
  .validator(weaponSchema)
  .handler(({ data }) =>
    request(
      'marines',
      getDeleteOneByWeaponUrl(data.weaponType),
      z.undefined(),
      { method: 'DELETE' },
    ),
  );

export const deleteByHealth = createServerFn({ method: 'POST' })
  .validator(healthSchema)
  .handler(({ data }) =>
    request('marines', getDeleteByHealthUrl(data.health), DeleteByHealth200, {
      method: 'DELETE',
    }),
  );
