import { createFileRoute, redirect } from '@tanstack/react-router';

export const Route = createFileRoute('/')({
  beforeLoad: () => {
    throw redirect({
      to: '/space-marines',
      search: { page: 0, size: 10, filter: [], sort: [] },
    });
  },
});
