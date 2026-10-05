import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { apiFetch } from "@/lib/fetcher";
import type { Source, SourcePlatform } from "@/types/api";
import type { components } from "@/types/api.generated";

import { RUNS_QUERY_KEY } from "./runs";
import { SOURCES_QUERY_KEY } from "./sources";

export const LOCATIONS_QUERY_KEY = ["locations"] as const;

export type AdminLocationTag = components["schemas"]["LocationTag"];
export type LocationTagUpdate = components["schemas"]["LocationTagUpdate"];
type BackendSource = components["schemas"]["Source"];

export type SourceInput = {
  name: string;
  platform: SourcePlatform;
  board_id: string;
  is_active: boolean;
};

export function useAdminLocations() {
  return useQuery<AdminLocationTag[]>({
    queryKey: LOCATIONS_QUERY_KEY,
    queryFn: () => apiFetch<AdminLocationTag[]>("/api/v1/locations"),
  });
}

export function useCreateSource() {
  const queryClient = useQueryClient();
  return useMutation<Source, Error, SourceInput>({
    mutationFn: (input) =>
      apiFetch<BackendSource>("/api/v1/admin/sources", {
        method: "POST",
        body: JSON.stringify(toBackendSource(input)),
      }).then(fromBackendSource),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: SOURCES_QUERY_KEY }),
  });
}

export function useUpdateSource() {
  const queryClient = useQueryClient();
  return useMutation<Source, Error, { id: number; input: SourceInput }>({
    mutationFn: ({ id, input }) =>
      apiFetch<BackendSource>(`/api/v1/admin/sources/${id}`, {
        method: "PUT",
        body: JSON.stringify(toBackendSource(input)),
      }).then(fromBackendSource),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: SOURCES_QUERY_KEY }),
  });
}

export function useDeleteSource() {
  const queryClient = useQueryClient();
  return useMutation<void, Error, number>({
    mutationFn: (id) =>
      apiFetch<void>(`/api/v1/admin/sources/${id}`, { method: "DELETE" }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: SOURCES_QUERY_KEY }),
  });
}

export function useUpdateLocationTag() {
  const queryClient = useQueryClient();
  return useMutation<AdminLocationTag, Error, { id: number; update: LocationTagUpdate }>({
    mutationFn: ({ id, update }) =>
      apiFetch<AdminLocationTag>(`/api/v1/admin/locations/${id}`, {
        method: "PUT",
        body: JSON.stringify(update),
      }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: LOCATIONS_QUERY_KEY }),
  });
}

export function useTriggerRun() {
  const queryClient = useQueryClient();
  return useMutation<unknown, Error, void>({
    mutationFn: () =>
      apiFetch("/api/v1/runs", { method: "POST", body: JSON.stringify({}) }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: RUNS_QUERY_KEY }),
  });
}

function toBackendSource(input: SourceInput) {
  return {
    name: input.name,
    platform: input.platform,
    boardId: input.board_id,
    isActive: input.is_active,
  };
}

function fromBackendSource(source: BackendSource): Source {
  return {
    id: source.id,
    name: source.name,
    platform: source.platform,
    board_id: source.boardId,
    is_active: source.isActive,
  };
}
