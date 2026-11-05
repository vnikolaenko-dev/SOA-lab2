import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import {
  createRootRoute,
  HeadContent,
  Outlet,
  Scripts,
} from '@tanstack/react-router';
import { type ReactNode, useState } from 'react';
import '~/lib/zod-locale';
import '~/styles/app.css';

export const Route = createRootRoute({
  head: () => ({
    meta: [
      {
        charSet: 'utf-8',
      },
      {
        name: 'viewport',
        content: 'width=device-width, initial-scale=1',
      },
      {
        title: 'SOA Lab2',
      },
    ],
  }),
  component: RootComponent,
});

function RootComponent() {
  const [queryClient] = useState(() => new QueryClient());

  return (
    <RootDocument>
      <QueryClientProvider client={queryClient}>
        <Outlet />
      </QueryClientProvider>
    </RootDocument>
  );
}

function RootDocument({ children }: Readonly<{ children: ReactNode }>) {
  return (
    <html lang="ru">
      <head>
        <HeadContent />
      </head>
      <body>
        {children}
        <Scripts />
      </body>
    </html>
  );
}
