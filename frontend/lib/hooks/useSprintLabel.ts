import { useQuery } from "@tanstack/react-query";
import { getSprint } from "@/lib/api/endpoints/sprints";
import { isApiClientError } from "@/lib/api/errors";

/**
 * docs/API.md §5 note 4 / §4.8: sprint deletion doesn't cascade, so an
 * issue's `sprintId` (or a `SPRINT_UPDATED` activity value) can point at a
 * sprint that's gone. Degrade to a "deleted" label instead of erroring the
 * row it appears in.
 */
export function useSprintLabel(projectId: number, sprintId: number | null | undefined) {
  const { data, isError, error, isLoading } = useQuery({
    queryKey: ["sprints", projectId, sprintId],
    queryFn: () => getSprint(projectId, sprintId as number),
    enabled: sprintId != null,
    staleTime: 60_000,
    retry: false,
  });

  const deleted = isError && isApiClientError(error) && error.status === 404;

  return { name: data?.name, deleted, isLoading };
}
