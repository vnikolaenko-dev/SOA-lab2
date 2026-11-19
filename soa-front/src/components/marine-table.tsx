import { Link } from '@tanstack/react-router';
import { type ColumnDef, tableFeatures, useTable } from '@tanstack/react-table';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '~/components/ui/table';
import type { Marine } from '~/lib/marine-display';
import { formatDate, weaponLabels } from '~/lib/marine-display';

const features = tableFeatures({});
const columns: Array<ColumnDef<typeof features, Marine>> = [
  { accessorKey: 'id', header: 'ID' },
  {
    accessorKey: 'name',
    header: 'Имя',
    cell: ({ row }) => (
      <Link
        to="/space-marines/$id"
        params={{ id: String(row.original.id) }}
        className="font-medium underline-offset-4 hover:underline"
      >
        {row.original.name}
      </Link>
    ),
  },
  { accessorKey: 'health', header: 'Здоровье' },
  {
    accessorKey: 'weaponType',
    header: 'Оружие',
    cell: ({ row }) => weaponLabels[row.original.weaponType],
  },
  { accessorFn: (row) => row.chapter.name, id: 'chapter', header: 'Орден' },
  {
    accessorKey: 'creationDate',
    header: 'Создан',
    cell: ({ row }) => formatDate(row.original.creationDate),
  },
];

export function MarineTable({ marines }: { marines: Marine[] }) {
  const table = useTable({ features, columns, data: marines });
  return (
    <div className="overflow-x-auto rounded-xl border bg-card">
      <Table className="min-w-[700px]">
        <TableHeader className="bg-muted/50 text-muted-foreground">
          {table.getHeaderGroups().map((group) => (
            <TableRow key={group.id}>
              {group.headers.map((header) => (
                <TableHead key={header.id} className="px-4 py-3 font-medium">
                  {header.isPlaceholder ? null : (
                    <table.FlexRender header={header} />
                  )}
                </TableHead>
              ))}
            </TableRow>
          ))}
        </TableHeader>
        <TableBody>
          {table.getRowModel().rows.map((row) => (
            <TableRow key={row.id}>
              {row.getAllCells().map((cell) => (
                <TableCell key={cell.id} className="px-4 py-3">
                  <table.FlexRender cell={cell} />
                </TableCell>
              ))}
            </TableRow>
          ))}
          {marines.length === 0 && (
            <TableRow>
              <TableCell
                colSpan={columns.length}
                className="px-4 py-10 text-center text-muted-foreground"
              >
                Ничего не найдено
              </TableCell>
            </TableRow>
          )}
        </TableBody>
      </Table>
    </div>
  );
}
