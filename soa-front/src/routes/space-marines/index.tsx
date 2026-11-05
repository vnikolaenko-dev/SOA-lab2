import { useQuery } from "@tanstack/react-query";
import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { z } from "zod";
import { listMarines } from "~/api/server/marines";
import { CollectionControls } from "~/components/collection-controls";
import { MarineSpecialActions } from "~/components/marine-special-actions";
import { MarineTable } from "~/components/marine-table";
import { PageShell } from "~/components/page-shell";
import { Pagination } from "~/components/pagination";
import { ServiceErrorView } from "~/components/service-error";
import { Button } from "~/components/ui/button";

const searchSchema = z.object({
  page: z.coerce.number().int().min(0).catch(0),
  size: z.coerce.number().int().min(1).max(100).catch(10),
  filter: z.array(z.string()).catch([]),
  sort: z.array(z.string()).catch([]),
});

export const Route = createFileRoute("/space-marines/")({
  validateSearch: (search) => searchSchema.parse(search),
  component: MarinesPage,
});

function MarinesPage() {
  const search = Route.useSearch();
  const navigate = useNavigate({ from: Route.fullPath });
  const query = useQuery({
    queryKey: ["marines", search],
    queryFn: () => listMarines({ data: search }),
  });
  const result = query.data;
  const page = result?.ok ? result.data : null;

  return (
    <PageShell
      title="Десантники"
      actions={
        <div className="flex flex-wrap gap-2">
          <Link to="/space-marines/new">
            <Button>Добавить десантника</Button>
          </Link>
        </div>
      }
    >
      <CollectionControls
        key={JSON.stringify([search.filter, search.sort, search.size])}
        filters={search.filter}
        sorts={search.sort}
        allowSort
        size={search.size}
        onApply={(next) => navigate({ search: { ...next, page: 0 } })}
      />
      {query.isPending && (
        <p className="text-muted-foreground">Загрузка коллекции...</p>
      )}
      {query.isError && (
        <ServiceErrorView
          error={{ status: 0, message: "Не удалось получить ответ приложения" }}
        />
      )}
      {result && !result.ok && <ServiceErrorView error={result.error} />}
      {page && (
        <div className="space-y-4">
          <MarineTable marines={page.content} />
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
      <MarineSpecialActions />
    </PageShell>
  );
}
