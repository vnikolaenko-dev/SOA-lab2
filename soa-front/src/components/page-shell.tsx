import { Link } from "@tanstack/react-router";
import type { ReactNode } from "react";

export function PageShell({
  title,
  description,
  children,
  actions,
}: {
  title: string;
  description?: string;
  children: ReactNode;
  actions?: ReactNode;
}) {
  return (
    <main className="mx-auto max-w-7xl space-y-8 px-4 py-8 sm:px-6 lg:px-8">
      <nav className="flex items-center gap-5 border-b pb-4 text-sm">
        <Link
          to="/space-marines"
          search={{ page: 0, size: 10, filter: [], sort: [] }}
          activeProps={{
            className: "font-semibold",
          }}
        >
          Space Marines
        </Link>
        <Link
          to="/space-marines/bulk"
          activeProps={{
            className: "font-semibold",
          }}
        >
          Генератор Space Marines
        </Link>
        <Link
          to="/starships"
          search={{ page: 0, size: 10 }}
          activeProps={{
            className: "font-semibold",
          }}
        >
          Корабли
        </Link>
      </nav>
      <header className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-3xl font-semibold tracking-tight">{title}</h1>
          {description && (
            <p className="mt-2 text-muted-foreground">{description}</p>
          )}
        </div>
        {actions}
      </header>
      {children}
    </main>
  );
}
