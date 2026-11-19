import { Button } from '~/components/ui/button';

export function Pagination({
  page,
  totalPages,
  totalElements,
  size,
  onPageChange,
}: {
  page: number;
  totalPages: number;
  totalElements: number;
  size: number;
  onPageChange: (page: number) => void;
}) {
  const from = totalElements === 0 ? 0 : page * size + 1;
  const to = Math.min((page + 1) * size, totalElements);
  return (
    <div className="flex flex-wrap items-center justify-between gap-3 text-sm text-muted-foreground">
      <span>
        {from}–{to} из {totalElements}
      </span>
      <div className="flex items-center gap-3">
        <Button
          variant="outline"
          disabled={page <= 0}
          onClick={() => onPageChange(page - 1)}
        >
          Назад
        </Button>
        <span>
          Страница {page + 1} из {Math.max(totalPages, 1)}
        </span>
        <Button
          variant="outline"
          disabled={page + 1 >= totalPages}
          onClick={() => onPageChange(page + 1)}
        >
          Далее
        </Button>
      </div>
    </div>
  );
}
