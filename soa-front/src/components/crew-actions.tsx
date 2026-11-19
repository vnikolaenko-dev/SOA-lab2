import { logError } from '~/lib/logger';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import type { ServiceError } from '~/api/server/http';
import { unloadCrew } from '~/api/server/starships';
import { ServiceErrorView } from './service-error';
import { Button } from './ui/button';

export function CrewActions({ starshipId }: { starshipId: number }) {
  const queryClient = useQueryClient();
  const [error, setError] = useState<ServiceError | null>(null);
  const [message, setMessage] = useState('');
  const unload = useMutation({
    mutationFn: () => unloadCrew({ data: { starshipId } }),
  });

  async function refresh() {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['crew', starshipId] }),
      queryClient.invalidateQueries({ queryKey: ['starships'] }),
      queryClient.invalidateQueries({ queryKey: ['starship-options'] }),
      queryClient.invalidateQueries({ queryKey: ['marines'] }),
    ]);
  }

  async function unloadAll() {
    if (!window.confirm('Высадить весь экипаж корабля?')) return;
    setError(null);
    try {
      const result = await unload.mutateAsync();
      if (result.ok) {
        setMessage(`Высажено десантников: ${result.data}`);
        await refresh();
      } else setError(result.error);
    } catch (error) {
      logError('ui.action', error, { component: 'crew-actions' });
      setError({ status: 0, message: 'Не удалось высадить экипаж' });
    }
  }

  return (
    <section className="space-y-4 rounded-xl border bg-card p-5">
      <h2 className="font-semibold">Управление экипажем</h2>
      <Button
        variant="destructive"
        disabled={unload.isPending}
        onClick={unloadAll}
      >
        Высадить весь экипаж
      </Button>
      {message && (
        <p role="status" className="text-sm text-muted-foreground">
          {message}
        </p>
      )}
      <ServiceErrorView error={error} />
    </section>
  );
}
