import { useQuery } from "@tanstack/react-query";
import { getTeam } from "@/lib/api/endpoints/teams";
import { isApiClientError } from "@/lib/api/errors";

/** Small and cached forever — there's no batch team-lookup either, but the
 * volume is far lower than users, so a per-id fetch is fine. */
export function useTeamName(teamId: number | null | undefined) {
  const { data, isLoading } = useQuery({
    queryKey: ["teams", teamId],
    queryFn: () => getTeam(teamId as number),
    enabled: teamId != null,
    staleTime: Infinity,
    retry: false,
  });
  return { name: data?.name, isLoading };
}
