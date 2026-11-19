import { useQuery } from "@tanstack/react-query";
import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { z } from "zod";
import { getCrew } from "~/api/server/starships";
import { CollectionControls } from "~/components/collection-controls";
import { CrewActions } from "~/components/crew-actions";
import { MarineTable } from "~/components/marine-table";
import { PageShell } from "~/components/page-shell";
import { Pagination } from "~/components/pagination";
import { ServiceErrorView } from "~/components/service-error";

const searchSchema = z.object({
  page: z.coerce.number().int().min(0).catch(0),
  size: z.coerce.number().int().min(1).max(100).catch(10),
  filter: z.array(z.string()).catch([]),
});

export const Route = createFileRoute("/starships/$starshipId")({
  validateSearch: (search) => searchSchema.parse(search),
  component: CrewPage,
});

function CrewPage() {
  const { starshipId } = Route.useParams();
  const id = Number(starshipId);
  const search = Route.useSearch();
  const navigate = useNavigate({ from: Route.fullPath });
  const query = useQuery({
    queryKey: ["crew", id, search],
    queryFn: () => getCrew({ data: { starshipId: id, params: search } }),
    enabled: Number.isInteger(id) && id > 0,
  });
  const result = query.data;
  const page = result?.ok ? result.data : null;

  return (
    <PageShell title={`Корабль #${starshipId}`}>
      {!Number.isInteger(id) || id <= 0 ? (
        <ServiceErrorView
          error={{
            status: 0,
            message: "ID корабля должен быть положительным целым числом",
          }}
        />
      ) : null}
      <CollectionControls
        key={JSON.stringify([search.filter, search.size])}
        filters={search.filter}
        size={search.size}
        onApply={(next) =>
          navigate({
            search: { filter: next.filter, size: next.size, page: 0 },
          })
        }
      />
      {query.isPending && (
        <p className="text-muted-foreground">Загрузка экипажа...</p>
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
      {Number.isInteger(id) && id > 0 && <CrewActions starshipId={id} />}
    </PageShell>
  );
}
