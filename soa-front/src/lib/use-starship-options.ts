import { useQuery } from '@tanstack/react-query';
import { getAllStarshipIds } from '~/api/server/starships';

export function useStarshipOptions() {
  return useQuery({
    queryKey: ['starship-options'],
    queryFn: () => getAllStarshipIds(),
  });
}
