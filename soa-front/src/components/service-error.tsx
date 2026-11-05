import type { ServiceError } from '~/api/server/http';
import { useEffect } from 'react';
import { logError } from '~/lib/logger';

export function ServiceErrorView({ error }: { error?: ServiceError | null }) {
  const status = error?.status;
  const message = error?.message;
  const field = error?.field;
  useEffect(() => {
    if (message !== undefined) {
      logError('api.result', new Error(message), { status, field });
    }
  }, [status, message, field]);
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
