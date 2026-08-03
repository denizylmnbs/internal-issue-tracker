import { useMutation, useQueryClient, type UseMutationOptions } from "@tanstack/react-query";
import { toast } from "sonner";
import { isApiClientError } from "@/lib/api/errors";
import { SESSION_QUERY_KEY } from "@/lib/auth/session";

/**
 * Every mutation in the app goes through this rather than raw `useMutation`.
 * It's the mechanism behind docs/API.md §5 note 8 — "roles are re-read on
 * every request, never trust a cached one" — by turning any 403 into both a
 * toast and a session refetch, so a role that changed elsewhere is reflected
 * immediately rather than after the next full reload. Every other
 * ApiClientError also gets a toast by default so individual call sites don't
 * have to remember to.
 */
export function useApiMutation<TData, TVariables>(
  options: UseMutationOptions<TData, unknown, TVariables> & { silent?: boolean },
) {
  const queryClient = useQueryClient();
  const { silent, onError, ...rest } = options;

  return useMutation<TData, unknown, TVariables>({
    ...rest,
    onError: (error, variables, onMutateResult, context) => {
      if (isApiClientError(error)) {
        if (!silent) toast.error(error.message);
        if (error.status === 403) {
          queryClient.invalidateQueries({ queryKey: SESSION_QUERY_KEY });
        }
      } else if (!silent) {
        toast.error("Something went wrong. Try again.");
      }
      onError?.(error, variables, onMutateResult, context);
    },
  });
}
