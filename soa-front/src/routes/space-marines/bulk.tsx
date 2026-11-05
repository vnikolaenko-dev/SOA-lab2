import { useQueryClient } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { useState } from "react";
import { z } from "zod";
import {
  type BulkMarineResult,
  createManyMarines,
  MAX_BULK_MARINES,
} from "~/api/server/bulk-marines";
import { useAppForm } from "~/components/marine-form-controls";
import { PageShell } from "~/components/page-shell";
import { ServiceErrorView } from "~/components/service-error";
import { Button } from "~/components/ui/button";

const formSchema = z.object({
  count: z
    .string()
    .refine(
      (value) => /^[1-9]\d*$/.test(value) && Number(value) <= MAX_BULK_MARINES,
      `Укажите целое число от 1 до ${MAX_BULK_MARINES}`,
    ),
});

export const Route = createFileRoute("/space-marines/bulk")({
  component: BulkMarinesPage,
});

function BulkMarinesPage() {
  const queryClient = useQueryClient();
  const [result, setResult] = useState<BulkMarineResult | null>(null);
  const [requestError, setRequestError] = useState(false);
  const form = useAppForm({
    defaultValues: { count: "10" },
    validators: { onSubmit: formSchema },
    onSubmit: async ({ value }) => {
      setResult(null);
      setRequestError(false);
      try {
        const response = await createManyMarines({
          data: { count: Number(value.count) },
        });
        setResult(response);
        if (response.created > 0) {
          await queryClient.invalidateQueries({ queryKey: ["marines"] });
        }
      } catch {
        setRequestError(true);
      }
    },
  });

  return (
    <PageShell title="Добавить десантников">
      <form
        className="max-w-lg space-y-5 rounded-xl border bg-card p-6"
        noValidate
        onSubmit={(event) => {
          event.preventDefault();
          void form.handleSubmit();
        }}
      >
        <form.AppField name="count">
          {(field) => (
            <field.TextField
              label="Количество десантников"
              type="number"
              min="1"
              step="1"
            />
          )}
        </form.AppField>
        <form.Subscribe selector={(state) => state.isSubmitting}>
          {(pending) => (
            <Button type="submit" disabled={pending}>
              {pending ? "Добавление..." : "Добавить десантников"}
            </Button>
          )}
        </form.Subscribe>
      </form>
      {result && (
        <p role="status" className="text-sm">
          Создано десантников: {result.created} из {result.requested}.
        </p>
      )}
      {result?.error && <ServiceErrorView error={result.error} />}
      {requestError && (
        <ServiceErrorView
          error={{
            status: 0,
            message: "Не удалось выполнить запрос на создание десантников",
          }}
        />
      )}
    </PageShell>
  );
}
