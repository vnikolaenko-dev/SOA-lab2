import type { ServiceError } from '~/api/server/http';

export function ServiceErrorView({ error }: { error?: ServiceError | null }) {
  if (!error) return null;
  return (
    <div
      role="alert"
      className="rounded-xl border border-destructive/30 bg-destructive/10 p-4 text-sm text-destructive"
    >
      <strong>{error.status ? `Ошибка ${error.status}` : 'Ошибка'}</strong>
      <p>{error.message}</p>
      {error.field && <p>Поле: {error.field}</p>}
    </div>
  );
}
