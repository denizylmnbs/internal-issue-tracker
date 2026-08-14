"use client";

import { useCallback, useSyncExternalStore } from "react";

/**
 * Whether a navigation rail is collapsed, remembered across reloads under its
 * own key so the two rails collapse independently.
 *
 * `localStorage` is read through {@link useSyncExternalStore} rather than
 * mirrored into state by an effect: it *is* an external store, it does not
 * exist while the page renders on the server, and the hook's server snapshot
 * (expanded) is what React hydrates against before swapping in the real answer.
 * Doing it with `useState` + `useEffect` would mean a setState in an effect on
 * every mount, which is the cascading render the lint rule is there to catch.
 */
const listeners = new Set<() => void>();

function subscribe(onStoreChange: () => void) {
  listeners.add(onStoreChange);
  // a second tab flipping the same preference
  window.addEventListener("storage", onStoreChange);
  return () => {
    listeners.delete(onStoreChange);
    window.removeEventListener("storage", onStoreChange);
  };
}

export function useRailCollapsed(key: string): [boolean, () => void] {
  const storageKey = `ist.rail.${key}.collapsed`;

  const collapsed = useSyncExternalStore(
    subscribe,
    () => window.localStorage.getItem(storageKey) === "1",
    () => false,
  );

  const toggle = useCallback(() => {
    window.localStorage.setItem(storageKey, collapsed ? "0" : "1");
    // `storage` does not fire in the tab that wrote, so tell this one directly
    listeners.forEach((notify) => notify());
  }, [collapsed, storageKey]);

  return [collapsed, toggle];
}
