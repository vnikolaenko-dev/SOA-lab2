import { useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useState } from "react";
import type { ServiceError } from "~/api/server/http";
import { deleteMarine, getMarine } from "~/api/server/marines";
import { MarineBoardingForm } from "~/components/marine-boarding-form";
import { MarineCard } from "~/components/marine-card";
import { PageShell } from "~/components/page-shell";
import { ServiceErrorView } from "~/components/service-error";
import { Button, buttonVariants } from "~/components/ui/button";

export const Route = createFileRoute("/space-marines/$id")({
  component: MarinePage,
});

function MarinePage() {
  const { id } = Route.useParams();
  const numericId = Number(id);
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [error, setError] = useState<ServiceError | null>(null);
  const query = useQuery({
    queryKey: ["marine", numericId],
    queryFn: () => getMarine({ data: { id: numericId } }),
    enabled: Number.isInteger(numericId) && numericId > 0,
  });
  const result = query.data;
  const marine = result?.ok ? result.data : null;

  async function refresh() {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ["marine", numericId] }),
      queryClient.invalidateQueries({ queryKey: ["marines"] }),
      queryClient.invalidateQueries({ queryKey: ["crew"] }),
    ]);
  }

  async function remove() {
    if (!window.confirm(`Удалить десантника #${id}?`)) return;
    setError(null);
    try {
      const response = await deleteMarine({ data: { id: numericId } });
      if (response.ok) {
        await refresh();
        await navigate({
          to: "/space-marines",
          search: { page: 0, size: 10, filter: [], sort: [] },
        });
      } else setError(response.error);
    } catch {
      setError({ status: 0, message: "Не удалось удалить десантника" });
    }
  }

  return (
    <PageShell
      title={`Десантник #${id}`}
      actions={
        marine && (
          <div className="flex gap-2">
            <Link
              to="/space-marines/$id/edit"
              params={{ id }}
              className={buttonVariants({ variant: "outline" })}
            >
              Редактирование
            </Link>
            <Button variant="destructive" onClick={remove}>
              Удалить
            </Button>
          </div>
        )
      }
    >
      {!Number.isInteger(numericId) || numericId <= 0 ? (
        <ServiceErrorView
          error={{
            status: 0,
            message: "ID должен быть положительным целым числом",
          }}
        />
      ) : null}
      {query.isPending && <p className="text-muted-foreground">Загрузка...</p>}
      {query.isError && (
        <ServiceErrorView
          error={{ status: 0, message: "Не удалось получить ответ приложения" }}
        />
      )}
      {result && !result.ok && <ServiceErrorView error={result.error} />}
      <ServiceErrorView error={error} />
      {marine && (
        <>
          <MarineCard marine={marine} />
          <MarineBoardingForm marineId={marine.id} />
        </>
      )}
    </PageShell>
  );
}
