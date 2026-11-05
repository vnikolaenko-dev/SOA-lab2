import { useQueryClient } from '@tanstack/react-query';
import { Check, Pencil, X } from 'lucide-react';
import { useState } from 'react';
import { SpaceMarinePatchInput } from '~/api/generated/space-marines/models';
import type { ServiceError } from '~/api/server/http';
import { patchMarine } from '~/api/server/marines';
import type { Marine } from '~/lib/marine-display';
import { meleeLabels, weaponLabels } from '~/lib/marine-display';
import { ServiceErrorView } from './service-error';
import { Button } from './ui/button';
import { NativeSelect, NativeSelectOption } from './ui/native-select';

type WeaponKind = 'ranged' | 'melee';

export function MarineWeaponField({
  marine,
  kind,
}: {
  marine: Marine;
  kind: WeaponKind;
}) {
  const queryClient = useQueryClient();
  const label = kind === 'ranged' ? 'Дальнобойное оружие' : 'Ближнее оружие';
  const current =
    kind === 'ranged' ? marine.weaponType : (marine.meleeWeapon ?? '');
  const display =
    kind === 'ranged'
      ? weaponLabels[marine.weaponType]
      : marine.meleeWeapon
        ? meleeLabels[marine.meleeWeapon]
        : 'Нет';
  const options =
    kind === 'ranged'
      ? Object.entries(weaponLabels)
      : [['', 'Нет'], ...Object.entries(meleeLabels)];
  const [editing, setEditing] = useState(false);
  const [value, setValue] = useState(current);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<ServiceError | null>(null);

  function cancel() {
    setValue(current);
    setError(null);
    setEditing(false);
  }

  async function save() {
    if (value === current) {
      setEditing(false);
      return;
    }
    const parsed = SpaceMarinePatchInput.safeParse(
      kind === 'ranged'
        ? { weaponType: value }
        : { meleeWeapon: value || null },
    );
    if (!parsed.success) {
      setError({
        status: 0,
        message:
          parsed.error.issues[0]?.message ?? 'Некорректное значение оружия',
      });
      return;
    }
    setError(null);
    setPending(true);
    try {
      const result = await patchMarine({
        data: { id: marine.id, patch: parsed.data },
      });
      if (!result.ok) {
        setError(result.error);
        return;
      }
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['marine', marine.id] }),
        queryClient.invalidateQueries({ queryKey: ['marines'] }),
        queryClient.invalidateQueries({ queryKey: ['crew'] }),
      ]);
      setEditing(false);
    } catch {
      setError({ status: 0, message: 'Не удалось сохранить оружие' });
    } finally {
      setPending(false);
    }
  }

  return (
    <div className="border-b py-3 last:border-b-0">
      <dt className="text-sm text-muted-foreground">{label}</dt>
      <dd className="mt-1 flex min-h-8 flex-wrap items-center gap-2">
        {editing ? (
          <>
            <NativeSelect
              className="min-w-40"
              aria-label={label}
              value={value}
              disabled={pending}
              onChange={(event) => setValue(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === 'Escape') cancel();
              }}
            >
              {options.map(([option, text]) => (
                <NativeSelectOption key={option} value={option}>
                  {text}
                </NativeSelectOption>
              ))}
            </NativeSelect>
            <Button
              type="button"
              variant="ghost"
              size="icon-sm"
              aria-label={`Сохранить: ${label}`}
              title="Сохранить"
              disabled={pending}
              onClick={save}
            >
              <Check />
            </Button>
            <Button
              type="button"
              variant="ghost"
              size="icon-sm"
              aria-label={`Отменить: ${label}`}
              title="Отменить"
              disabled={pending}
              onClick={cancel}
            >
              <X />
            </Button>
          </>
        ) : (
          <>
            <span>{display}</span>
            <Button
              type="button"
              variant="ghost"
              size="icon-sm"
              aria-label={`Редактировать: ${label}`}
              title="Редактировать"
              onClick={() => {
                setValue(current);
                setError(null);
                setEditing(true);
              }}
            >
              <Pencil />
            </Button>
          </>
        )}
      </dd>
      <ServiceErrorView error={error} />
    </div>
  );
}
