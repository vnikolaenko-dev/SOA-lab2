import { Card, CardContent, CardHeader, CardTitle } from '~/components/ui/card';
import type { Marine } from '~/lib/marine-display';
import { formatDate } from '~/lib/marine-display';
import { MarineWeaponField } from './marine-weapon-field';

const itemClass = 'border-b py-3 last:border-b-0';

export function MarineCard({ marine }: { marine: Marine }) {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-xl font-semibold">
          {marine.name}{' '}
          <span className="text-sm font-normal text-muted-foreground">
            #{marine.id}
          </span>
        </CardTitle>
      </CardHeader>
      <CardContent>
        <dl className="mt-4 grid gap-x-8 sm:grid-cols-2">
          <div className={itemClass}>
            <dt className="text-sm text-muted-foreground">Здоровье</dt>
            <dd>{marine.health}</dd>
          </div>
          <div className={itemClass}>
            <dt className="text-sm text-muted-foreground">Создан</dt>
            <dd>{formatDate(marine.creationDate)}</dd>
          </div>
          <div className={itemClass}>
            <dt className="text-sm text-muted-foreground">Координаты</dt>
            <dd>
              {marine.coordinates.x}, {marine.coordinates.y}
            </dd>
          </div>
          <MarineWeaponField marine={marine} kind="ranged" />
          <MarineWeaponField marine={marine} kind="melee" />
          <div className={itemClass}>
            <dt className="text-sm text-muted-foreground">Орден</dt>
            <dd>{marine.chapter.name}</dd>
          </div>
          <div className={itemClass}>
            <dt className="text-sm text-muted-foreground">
              Родительский легион
            </dt>
            <dd>{marine.chapter.parentLegion || 'Не указан'}</dd>
          </div>
          <div className={itemClass}>
            <dt className="text-sm text-muted-foreground">Мир</dt>
            <dd>{marine.chapter.world}</dd>
          </div>
        </dl>
        <div className="pt-4">
          <h3 className="text-sm text-muted-foreground">Достижения</h3>
          <p className="mt-1 whitespace-pre-wrap">
            {marine.achievements || 'Не указаны'}
          </p>
        </div>
      </CardContent>
    </Card>
  );
}
