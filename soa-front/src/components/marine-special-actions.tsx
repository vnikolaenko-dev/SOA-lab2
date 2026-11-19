import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import type { ServiceError } from "~/api/server/http";
import {
  deleteByHealth,
  deleteOneByWeapon,
  getMaxNameMarine,
} from "~/api/server/marines";
import { Button } from "~/components/ui/button";
import { Input } from "~/components/ui/input";
import {
  NativeSelect,
  NativeSelectOption,
} from "~/components/ui/native-select";
import { ServiceErrorView } from "./service-error";

export function MarineSpecialActions() {
  const queryClient = useQueryClient();
  const [health, setHealth] = useState("");
  const [weaponType, setWeaponType] = useState<
    "COMBI_FLAMER" | "GRENADE_LAUNCHER" | "HEAVY_FLAMER"
  >("COMBI_FLAMER");
  const [message, setMessage] = useState("");
  const [error, setError] = useState<ServiceError | null>(null);

  const refresh = () =>
    queryClient.invalidateQueries({ queryKey: ["marines"] });
  const maxName = useMutation({ mutationFn: () => getMaxNameMarine() });
  const removeWeapon = useMutation({
    mutationFn: () => deleteOneByWeapon({ data: { weaponType } }),
  });
  const removeHealth = useMutation({
    mutationFn: () => deleteByHealth({ data: { health: Number(health) } }),
  });

  async function showMax() {
    setError(null);
    const result = await maxName.mutateAsync();
    if (result.ok)
      setMessage(`Наибольшее имя: ${result.data.name} (ID ${result.data.id})`);
    else setError(result.error);
  }

  async function deleteWeapon() {
    if (!window.confirm("Удалить одного десантника с выбранным оружием?"))
      return;
    setError(null);
    const result = await removeWeapon.mutateAsync();
    if (result.ok) {
      setMessage("Десантник удалён");
      await refresh();
    } else setError(result.error);
  }

  async function deleteHealth() {
    const value = Number(health);
    if (!Number.isInteger(value) || value <= 0) {
      setError({
        status: 0,
        message: "Введите положительное целое значение здоровья",
      });
      return;
    }
    if (!window.confirm(`Удалить всех десантников со здоровьем ${value}?`))
      return;
    setError(null);
    const result = await removeHealth.mutateAsync();
    if (result.ok) {
      setMessage(`Удалено: ${result.data}`);
      await refresh();
    } else setError(result.error);
  }

  return (
    <section className="space-y-4 rounded-xl border bg-card p-4">
      <h2 className="font-semibold">Дополнительные операции</h2>
      <div className="flex flex-col gap-3">
        <div>
          <Button
            variant="outline"
            disabled={maxName.isPending}
            onClick={showMax}
          >
            Найти максимальное имя
          </Button>
        </div>

        <div className="flex gap-3">
          <NativeSelect
            aria-label="Тип оружия"
            className="flex-1"
            value={weaponType}
            onChange={(event) =>
              setWeaponType(event.target.value as typeof weaponType)
            }
          >
            <NativeSelectOption value="COMBI_FLAMER">
              Комби-огнемёт
            </NativeSelectOption>
            <NativeSelectOption value="GRENADE_LAUNCHER">
              Гранатомёт
            </NativeSelectOption>
            <NativeSelectOption value="HEAVY_FLAMER">
              Тяжёлый огнемёт
            </NativeSelectOption>
          </NativeSelect>
          <Button
            variant="destructive"
            disabled={removeWeapon.isPending}
            onClick={deleteWeapon}
          >
            Удалить одного по оружию
          </Button>
        </div>

        <div className="flex gap-3">
          <Input
            aria-label="Здоровье"
            type="number"
            min="1"
            step="1"
            placeholder="Здоровье"
            className="min-w-44"
            value={health}
            onChange={(event) => setHealth(event.target.value)}
          />
          <Button
            variant="destructive"
            disabled={removeHealth.isPending}
            onClick={deleteHealth}
          >
            Удалить всех с этим здоровьем
          </Button>
        </div>
      </div>
      {message && (
        <p role="status" className="text-sm text-muted-foreground">
          {message}
        </p>
      )}
      <ServiceErrorView error={error} />
    </section>
  );
}
