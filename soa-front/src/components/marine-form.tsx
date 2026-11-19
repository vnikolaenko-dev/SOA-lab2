import { logError } from '~/lib/logger';
import { useState } from "react";
import {
  SpaceMarineInput,
  type SpaceMarineInputOutput,
} from "~/api/generated/space-marines/models";
import type { ApiResult, ServiceError } from "~/api/server/http";
import type { Marine } from "~/lib/marine-display";
import {
  meleeWeapons,
  rangedWeapons,
  useAppForm,
} from "./marine-form-controls";
import {
  marineFormSchema,
  marineInput,
  marineValues,
} from "./marine-form-model";
import { ServiceErrorView } from "./service-error";
import { Button } from "./ui/button";

export function MarineForm({
  initial,
  submitLabel,
  onSubmit,
}: {
  initial?: Marine;
  submitLabel: string;
  onSubmit: (input: SpaceMarineInputOutput) => Promise<ApiResult<Marine>>;
}) {
  const [serviceError, setServiceError] = useState<ServiceError | null>(null);
  const form = useAppForm({
    defaultValues: marineValues(initial),
    validators: { onSubmit: marineFormSchema },
    onSubmit: async ({ value }) => {
      setServiceError(null);
      try {
        const result = await onSubmit(
          SpaceMarineInput.parse(marineInput(value)),
        );
        if (!result.ok) setServiceError(result.error);
      } catch (error) {
        logError('ui.action', error, { component: 'marine-form' });
        setServiceError({ status: 0, message: "Не удалось отправить форму" });
      }
    },
  });

  return (
    <form
      onSubmit={(event) => {
        event.preventDefault();
        void form.handleSubmit();
      }}
      className="max-w-3xl space-y-6 rounded-xl border bg-card p-6"
      noValidate
    >
      <div className="grid gap-4 sm:grid-cols-2">
        <form.AppField name="name">
          {(field) => <field.TextField label="Имя" />}
        </form.AppField>
        <form.AppField name="health">
          {(field) => (
            <field.TextField label="Здоровье" type="number" min="1" step="1" />
          )}
        </form.AppField>
        <form.AppField name="x">
          {(field) => (
            <field.TextField label="Координата X" type="number" step="any" />
          )}
        </form.AppField>
        <form.AppField name="y">
          {(field) => (
            <field.TextField label="Координата Y" type="number" step="1" />
          )}
        </form.AppField>
        <form.AppField name="weaponType">
          {(field) => (
            <field.SelectField
              label="Дальнобойное оружие"
              options={rangedWeapons}
            />
          )}
        </form.AppField>
        <form.AppField name="meleeWeapon">
          {(field) => (
            <field.SelectField
              label="Ближнее оружие"
              options={[["", "Нет"], ...meleeWeapons]}
            />
          )}
        </form.AppField>
        <form.AppField name="chapterName">
          {(field) => <field.TextField label="Название ордена" />}
        </form.AppField>
        <form.AppField name="parentLegion">
          {(field) => <field.TextField label="Родительский легион" />}
        </form.AppField>
        <form.AppField name="world">
          {(field) => <field.TextField label="Мир" />}
        </form.AppField>
      </div>
      <form.AppField name="achievements">
        {(field) => <field.TextareaField label="Достижения" />}
      </form.AppField>
      <ServiceErrorView error={serviceError} />
      <form.Subscribe selector={(state) => state.isSubmitting}>
        {(pending) => (
          <Button type="submit" disabled={pending}>
            {pending ? "Сохранение..." : submitLabel}
          </Button>
        )}
      </form.Subscribe>
    </form>
  );
}
