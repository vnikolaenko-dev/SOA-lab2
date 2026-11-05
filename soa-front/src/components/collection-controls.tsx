import { useState } from 'react';
import { Button } from '~/components/ui/button';
import { Input } from '~/components/ui/input';
import { Label } from '~/components/ui/label';
import {
  NativeSelect,
  NativeSelectOption,
} from '~/components/ui/native-select';

type Rule = { id: string; field: string; operator: string; value: string };
type Sort = { id: string; field: string; direction: string };

const fields = [
  ['id', 'ID'],
  ['name', 'Имя'],
  ['health', 'Здоровье'],
  ['creationDate', 'Дата создания'],
  ['achievements', 'Достижения'],
  ['weaponType', 'Тип оружия'],
  ['meleeWeapon', 'Ближнее оружие'],
  ['chapter.name', 'Орден'],
  ['chapter.parentLegion', 'Родительский легион'],
  ['chapter.world', 'Мир'],
  ['coordinates.x', 'Координата X'],
  ['coordinates.y', 'Координата Y'],
] as const;

const inputClass = 'h-9 rounded-lg border bg-background px-3 text-sm';

function parseRule(value: string, index: number): Rule {
  const [field = 'name', operator = 'eq', ...rest] = value.split(',');
  return { id: `filter-${index}`, field, operator, value: rest.join(',') };
}

function parseSort(value: string, index: number): Sort {
  const [field = 'id', direction = 'asc'] = value.split(',');
  return { id: `sort-${index}`, field, direction };
}

export function CollectionControls({
  filters,
  sorts = [],
  allowSort = false,
  size,
  onApply,
}: {
  filters: string[];
  sorts?: string[];
  allowSort?: boolean;
  size: number;
  onApply: (next: { filter: string[]; sort: string[]; size: number }) => void;
}) {
  const [rules, setRules] = useState<Rule[]>(filters.map(parseRule));
  const [order, setOrder] = useState<Sort[]>(sorts.map(parseSort));
  const [pageSize, setPageSize] = useState(size);

  const setRule = (index: number, patch: Partial<Rule>) =>
    setRules((old) =>
      old.map((rule, i) => (i === index ? { ...rule, ...patch } : rule)),
    );
  const setSort = (index: number, patch: Partial<Sort>) =>
    setOrder((old) =>
      old.map((sort, i) => (i === index ? { ...sort, ...patch } : sort)),
    );

  return (
    <form
      className="space-y-4 rounded-xl border bg-card p-4"
      onSubmit={(event) => {
        event.preventDefault();
        onApply({
          filter: rules
            .filter((rule) => rule.value.trim())
            .map(
              ({ field, operator, value }) =>
                `${field},${operator},${value.trim()}`,
            ),
          sort: order.map(({ field, direction }) => `${field},${direction}`),
          size: pageSize,
        });
      }}
    >
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 className="font-semibold">Фильтры и вывод</h2>
        <div className="flex items-center gap-2 text-sm">
          <Label htmlFor="collection-page-size">На странице</Label>
          <NativeSelect
            id="collection-page-size"
            className="w-full"
            value={pageSize}
            onChange={(event) => setPageSize(Number(event.target.value))}
          >
            {[10, 20, 50, 100].map((value) => (
              <NativeSelectOption key={value} value={value}>
                {value}
              </NativeSelectOption>
            ))}
          </NativeSelect>
        </div>
      </div>
      {rules.map((rule, index) => (
        <div key={rule.id} className="flex flex-wrap gap-2">
          <NativeSelect
            aria-label="Поле фильтра"
            className="w-full"
            value={rule.field}
            onChange={(event) => setRule(index, { field: event.target.value })}
          >
            {fields.map(([value, label]) => (
              <NativeSelectOption key={value} value={value}>
                {label}
              </NativeSelectOption>
            ))}
          </NativeSelect>
          <NativeSelect
            aria-label="Условие фильтра"
            className="w-full"
            value={rule.operator}
            onChange={(event) =>
              setRule(index, { operator: event.target.value })
            }
          >
            <NativeSelectOption value="eq">Равно</NativeSelectOption>
            <NativeSelectOption value="gt">Больше</NativeSelectOption>
            <NativeSelectOption value="lt">Меньше</NativeSelectOption>
          </NativeSelect>
          <Input
            aria-label="Значение фильтра"
            className={`${inputClass} min-w-40 flex-1`}
            value={rule.value}
            onChange={(event) => setRule(index, { value: event.target.value })}
            placeholder="Значение"
          />
          <Button
            type="button"
            variant="ghost"
            onClick={() => setRules((old) => old.filter((_, i) => i !== index))}
          >
            Убрать
          </Button>
        </div>
      ))}
      <Button
        type="button"
        variant="outline"
        onClick={() =>
          setRules((old) => [
            ...old,
            {
              id: crypto.randomUUID(),
              field: 'name',
              operator: 'eq',
              value: '',
            },
          ])
        }
      >
        Добавить фильтр
      </Button>
      {allowSort && (
        <div className="space-y-2 border-t pt-4">
          <h3 className="text-sm font-semibold">Сортировка</h3>
          {order.map((sort, index) => (
            <div key={sort.id} className="flex flex-wrap gap-2">
              <NativeSelect
                aria-label="Поле сортировки"
                className="w-full"
                value={sort.field}
                onChange={(event) =>
                  setSort(index, { field: event.target.value })
                }
              >
                {fields.map(([value, label]) => (
                  <NativeSelectOption key={value} value={value}>
                    {label}
                  </NativeSelectOption>
                ))}
              </NativeSelect>
              <NativeSelect
                aria-label="Направление сортировки"
                className="w-full"
                value={sort.direction}
                onChange={(event) =>
                  setSort(index, { direction: event.target.value })
                }
              >
                <NativeSelectOption value="asc">
                  По возрастанию
                </NativeSelectOption>
                <NativeSelectOption value="desc">
                  По убыванию
                </NativeSelectOption>
              </NativeSelect>
              <Button
                type="button"
                variant="ghost"
                onClick={() =>
                  setOrder((old) => old.filter((_, i) => i !== index))
                }
              >
                Убрать
              </Button>
            </div>
          ))}
          <Button
            type="button"
            variant="outline"
            onClick={() =>
              setOrder((old) => [
                ...old,
                { id: crypto.randomUUID(), field: 'id', direction: 'asc' },
              ])
            }
          >
            Добавить сортировку
          </Button>
        </div>
      )}
      <div>
        <Button type="submit">Применить</Button>
      </div>
    </form>
  );
}
