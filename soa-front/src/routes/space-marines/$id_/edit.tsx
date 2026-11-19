import { useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { getMarine, updateMarine } from "~/api/server/marines";
import { MarineForm } from "~/components/marine-form";
import { PageShell } from "~/components/page-shell";
import { ServiceErrorView } from "~/components/service-error";
import { buttonVariants } from "~/components/ui/button";

export const Route = createFileRoute("/space-marines/$id_/edit")({
  component: EditMarinePage,
});

function EditMarinePage() {
  const { id } = Route.useParams();
  const marineId = Number(id);
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const query = useQuery({
    queryKey: ["marine", marineId],
    queryFn: () => getMarine({ data: { id: marineId } }),
    enabled: Number.isInteger(marineId) && marineId > 0,
  });
  const result = query.data;
  const marine = result?.ok ? result.data : null;

  return (
    <PageShell
      title={`Редактирование десантника #${id}`}
      actions={
        <Link
          to="/space-marines/$id"
          params={{ id }}
          className={buttonVariants({ variant: "outline" })}
        >
          Вернуться
        </Link>
      }
    >
      {(!Number.isInteger(marineId) || marineId <= 0) && (
        <ServiceErrorView
          error={{
            status: 0,
            message: "ID должен быть положительным целым числом",
          }}
        />
      )}
      {query.isPending && <p className="text-muted-foreground">Загрузка...</p>}
      {query.isError && (
        <ServiceErrorView
          error={{ status: 0, message: "Не удалось получить ответ приложения" }}
        />
      )}
      {result && !result.ok && <ServiceErrorView error={result.error} />}
      {marine && (
        <MarineForm
          key={marine.id}
          initial={marine}
          submitLabel="Сохранить"
          onSubmit={async (input) => {
            const response = await updateMarine({
              data: { id: marineId, marine: input },
            });
            if (response.ok) {
              await Promise.all([
                queryClient.invalidateQueries({
                  queryKey: ["marine", marineId],
                }),
                queryClient.invalidateQueries({ queryKey: ["marines"] }),
                queryClient.invalidateQueries({ queryKey: ["crew"] }),
              ]);
              await navigate({ to: "/space-marines/$id", params: { id } });
            }
            return response;
          }}
        />
      )}
    </PageShell>
  );
}
