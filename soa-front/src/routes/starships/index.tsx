import { useQuery } from '@tanstack/react-query';
import { createFileRoute, useNavigate } from '@tanstack/react-router';
import { z } from 'zod';
import { listStarships } from '~/api/server/starships';
import { PageShell } from '~/components/page-shell';
import { Pagination } from '~/components/pagination';
import { ServiceErrorView } from '~/components/service-error';
import { StarshipTable } from '~/components/starship-table';
import { Label } from '~/components/ui/label';
import {
  NativeSelect,
  NativeSelectOption,
} from '~/components/ui/native-select';

const searchSchema = z.object({
  page: z.coerce.number().int().min(0).catch(0),
  size: z.coerce.number().int().min(1).max(100).catch(10),
});

export const Route = createFileRoute('/starships/')({
  validateSearch: (search) => searchSchema.parse(search),
  component: StarshipsPage,
});

function StarshipsPage() {
  const search = Route.useSearch();
  const navigate = useNavigate({ from: Route.fullPath });
  const query = useQuery({
    queryKey: ['starships', search],
    queryFn: () => listStarships({ data: search }),
  });
  const result = query.data;
  const page = result?.ok ? result.data : null;

  return (
    <PageShell title="Корабли">
      <div className="flex items-center gap-2 text-sm">
        <Label htmlFor="starships-page-size">На странице</Label>
        <NativeSelect
          id="starships-page-size"
          value={search.size}
          onChange={(event) =>
            navigate({ search: { page: 0, size: Number(event.target.value) } })
          }
        >
          {[10, 20, 50, 100].map((size) => (
            <NativeSelectOption key={size} value={size}>
              {size}
            </NativeSelectOption>
          ))}
        </NativeSelect>
      </div>
      {query.isPending && (
        <p className="text-muted-foreground">Загрузка кораблей...</p>
      )}
      {query.isError && (
        <ServiceErrorView
          error={{ status: 0, message: 'Не удалось получить ответ приложения' }}
        />
      )}
      {result && !result.ok && <ServiceErrorView error={result.error} />}
      {page && (
        <div className="space-y-4">
          <StarshipTable starships={page.content} />
          <Pagination
            page={page.page}
            size={page.size}
            totalPages={page.totalPages}
            totalElements={page.totalElements}
            onPageChange={(number) =>
              navigate({ search: { ...search, page: number } })
            }
          />
        </div>
      )}
    </PageShell>
  );
}
