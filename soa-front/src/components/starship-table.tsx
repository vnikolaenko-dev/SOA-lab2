import { Link } from "@tanstack/react-router";
import type { StarshipOutput } from "~/api/generated/starships/models";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "~/components/ui/table";

export function StarshipTable({ starships }: { starships: StarshipOutput[] }) {
  return (
    <div className="rounded-xl border bg-card">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead className="px-4">ID корабля</TableHead>
            <TableHead className="px-4"></TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {starships.map((starship) => (
            <TableRow key={starship.id}>
              <TableCell className="px-4 font-medium">#{starship.id}</TableCell>
              <TableCell className="px-4 text-right">
                <Link
                  to="/starships/$starshipId"
                  params={{ starshipId: String(starship.id) }}
                  search={{ page: 0, size: 10, filter: [] }}
                  className="text-primary underline-offset-4 hover:underline"
                >
                  Открыть экипаж
                </Link>
              </TableCell>
            </TableRow>
          ))}
          {starships.length === 0 && (
            <TableRow>
              <TableCell
                colSpan={2}
                className="px-4 py-10 text-center text-muted-foreground"
              >
                Кораблей пока нет
              </TableCell>
            </TableRow>
          )}
        </TableBody>
      </Table>
    </div>
  );
}
