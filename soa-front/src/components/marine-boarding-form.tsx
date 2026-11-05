import { logError } from '~/lib/logger';
import { useQueryClient } from "@tanstack/react-query";
import { Link } from "@tanstack/react-router";
import { useState } from "react";
import { z } from "zod";
import type { ServiceError } from "~/api/server/http";
import { loadMarine, loadMarineInNewStarship } from "~/api/server/starships";
import { useStarshipOptions } from "~/lib/use-starship-options";
import { useAppForm } from "./marine-form-controls";
import { ServiceErrorView } from "./service-error";
import { Button } from "./ui/button";

const schema = z.object({
  starshipId: z.string().min(1, "Выберите корабль"),
});

export function MarineBoardingForm({ marineId }: { marineId: number }) {
  const queryClient = useQueryClient();
  const ships = useStarshipOptions();
  const [error, setError] = useState<ServiceError | null>(null);
  const [boardedStarshipId, setBoardedStarshipId] = useState<number | null>(
    null,
  );
  const [creatingShip, setCreatingShip] = useState(false);

  async function finishBoarding(starshipId: number) {
    setBoardedStarshipId(starshipId);
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ["crew", starshipId] }),
      queryClient.invalidateQueries({ queryKey: ["starships"] }),
      queryClient.invalidateQueries({ queryKey: ["starship-options"] }),
      queryClient.invalidateQueries({ queryKey: ["marines"] }),
    ]);
  }

  const form = useAppForm({
    defaultValues: { starshipId: "" },
    validators: { onSubmit: schema },
    onSubmit: async ({ value }) => {
      setError(null);
      setBoardedStarshipId(null);
      try {
        const starshipId = Number(value.starshipId);
        const result = await loadMarine({ data: { marineId, starshipId } });
        if (result.ok) await finishBoarding(starshipId);
        else setError(result.error);
      } catch (error) {
        logError('ui.action', error, { component: 'marine-boarding-form' });
        setError({
          status: 0,
          message: "Не удалось посадить десантника на корабль",
        });
      }
    },
  });

  async function boardNewShip() {
    setError(null);
    setBoardedStarshipId(null);
    setCreatingShip(true);
    try {
      const result = await loadMarineInNewStarship({ data: { marineId } });
      if (result.ok) await finishBoarding(result.data.starshipId);
      else setError(result.error);
    } catch (error) {
      logError('ui.action', error, { component: 'marine-boarding-form' });
      setError({
        status: 0,
        message: "Не удалось посадить десантника в новый корабль",
      });
    } finally {
      setCreatingShip(false);
    }
  }

  const ids = ships.data?.ok ? ships.data.data : [];
  return (
    <section
      id="boarding"
      className="max-w-xl space-y-4 rounded-xl border bg-card p-5"
    >
      <h2 className="font-semibold">Посадить на корабль</h2>
      <form
        className="flex flex-wrap items-end gap-3"
        noValidate
        onSubmit={(event) => {
          event.preventDefault();
          void form.handleSubmit();
        }}
      >
        <div className="min-w-44 flex-1">
          <form.AppField name="starshipId">
            {(field) => (
              <field.SelectField
                label="Корабль"
                options={[
                  [
                    "",
                    ships.isPending
                      ? "Загрузка кораблей..."
                      : "Выберите корабль",
                  ],
                  ...ids.map((id) => [String(id), `Корабль #${id}`] as const),
                ]}
              />
            )}
          </form.AppField>
        </div>
        <form.Subscribe selector={(state) => state.isSubmitting}>
          {(pending) => (
            <>
              <Button
                type="submit"
                disabled={pending || creatingShip || ids.length === 0}
              >
                {pending ? "Посадка..." : "Посадить"}
              </Button>
              <Button
                type="button"
                variant="outline"
                disabled={pending || creatingShip}
                onClick={boardNewShip}
              >
                {creatingShip ? "Создание..." : "Посадить в новый корабль"}
              </Button>
            </>
          )}
        </form.Subscribe>
      </form>
      {ships.isError && (
        <ServiceErrorView
          error={{ status: 0, message: "Не удалось загрузить список кораблей" }}
        />
      )}
      {ships.data && !ships.data.ok && (
        <ServiceErrorView error={ships.data.error} />
      )}
      {boardedStarshipId !== null && (
        <p role="status" className="text-sm">
          Десантник посажен на корабль #{boardedStarshipId}.{" "}
          <Link
            to="/starships/$starshipId"
            params={{ starshipId: String(boardedStarshipId) }}
            search={{ page: 0, size: 10, filter: [] }}
            className="text-primary underline-offset-4 hover:underline"
          >
            Открыть экипаж
          </Link>
        </p>
      )}
      <ServiceErrorView error={error} />
    </section>
  );
}
