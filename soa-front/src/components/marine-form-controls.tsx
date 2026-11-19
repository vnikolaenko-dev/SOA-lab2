import { createFormHook, createFormHookContexts } from '@tanstack/react-form';
import { type ReactNode, useId } from 'react';
import { Input } from './ui/input';
import { Label } from './ui/label';
import { NativeSelect, NativeSelectOption } from './ui/native-select';
import { Textarea } from './ui/textarea';

const { fieldContext, formContext, useFieldContext } = createFormHookContexts();

function FieldShell({
  label,
  children,
}: {
  label: string;
  children: (id: string) => ReactNode;
}) {
  const field = useFieldContext<string>();
  const id = useId();
  const error = field.state.meta.errors[0];
  const message =
    typeof error === 'string'
      ? error
      : error && typeof error === 'object' && 'message' in error
        ? String(error.message)
        : undefined;
  return (
    <div className="grid gap-1.5 text-sm">
      <Label htmlFor={id}>{label}</Label>
      {children(id)}
      {message && <span className="text-destructive">{message}</span>}
    </div>
  );
}

function TextField({
  label,
  type = 'text',
  min,
  step,
}: {
  label: string;
  type?: string;
  min?: string;
  step?: string;
}) {
  const field = useFieldContext<string>();
  return (
    <FieldShell label={label}>
      {(id) => (
        <Input
          id={id}
          type={type}
          min={min}
          step={step}
          className="h-10"
          value={field.state.value}
          onBlur={field.handleBlur}
          onChange={(event) => field.handleChange(event.target.value)}
        />
      )}
    </FieldShell>
  );
}

function SelectField({
  label,
  options,
}: {
  label: string;
  options: readonly (readonly [string, string])[];
}) {
  const field = useFieldContext<string>();
  return (
    <FieldShell label={label}>
      {(id) => (
        <NativeSelect
          id={id}
          className="w-full"
          value={field.state.value}
          onBlur={field.handleBlur}
          onChange={(event) => field.handleChange(event.target.value)}
        >
          {options.map(([value, text]) => (
            <NativeSelectOption key={value} value={value}>
              {text}
            </NativeSelectOption>
          ))}
        </NativeSelect>
      )}
    </FieldShell>
  );
}

function TextareaField({ label }: { label: string }) {
  const field = useFieldContext<string>();
  return (
    <FieldShell label={label}>
      {(id) => (
        <Textarea
          id={id}
          rows={3}
          value={field.state.value}
          onBlur={field.handleBlur}
          onChange={(event) => field.handleChange(event.target.value)}
        />
      )}
    </FieldShell>
  );
}

export const { useAppForm } = createFormHook({
  fieldContext,
  formContext,
  fieldComponents: { TextField, SelectField, TextareaField },
  formComponents: {},
});

export const rangedWeapons = [
  ['COMBI_FLAMER', 'Комби-огнемёт'],
  ['GRENADE_LAUNCHER', 'Гранатомёт'],
  ['HEAVY_FLAMER', 'Тяжёлый огнемёт'],
] as const;

export const meleeWeapons = [
  ['CHAIN_SWORD', 'Цепной меч'],
  ['CHAIN_AXE', 'Цепной топор'],
  ['POWER_BLADE', 'Силовой клинок'],
  ['POWER_FIST', 'Силовой кулак'],
] as const;
