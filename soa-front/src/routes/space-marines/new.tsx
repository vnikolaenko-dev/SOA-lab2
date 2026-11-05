import { useQueryClient } from '@tanstack/react-query';
import { createFileRoute, useNavigate } from '@tanstack/react-router';
import { createMarine } from '~/api/server/marines';
import { MarineForm } from '~/components/marine-form';
import { PageShell } from '~/components/page-shell';

export const Route = createFileRoute('/space-marines/new')({
  component: NewMarinePage,
});

function NewMarinePage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  return (
    <PageShell
      title="Новый десантник"
      description="Заполните данные нового элемента коллекции."
    >
      <MarineForm
        submitLabel="Создать"
        onSubmit={async (marine) => {
          const result = await createMarine({ data: marine });
          if (result.ok) {
            await queryClient.invalidateQueries({ queryKey: ['marines'] });
            await navigate({
              to: '/space-marines/$id',
              params: { id: String(result.data.id) },
            });
          }
          return result;
        }}
      />
    </PageShell>
  );
}
