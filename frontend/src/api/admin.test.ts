import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { renderHook, waitFor } from "@testing-library/react";

import {
  LOCATIONS_QUERY_KEY,
  useAdminLocations,
  useCreateSource,
  useDeleteSource,
  useTriggerRun,
  useUpdateLocationTag,
  useUpdateSource,
} from "./admin";
import { createQueryWrapper } from "@/test/queryWrapper";

function jsonResponse(body: unknown, status = 200) {
  return new Response(status === 204 ? null : JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

const BACKEND_SOURCE = {
  id: 3,
  name: "Acme",
  platform: "GREENHOUSE",
  boardId: "acme",
  isActive: true,
};

const INPUT = {
  name: "Acme",
  platform: "GREENHOUSE" as const,
  board_id: "acme",
  is_active: true,
};

beforeEach(() => {
  document.cookie = "csrftoken=tok; path=/";
});

afterEach(() => {
  vi.restoreAllMocks();
  document.cookie = "csrftoken=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT";
});

function lastCall(fetchSpy: { mock: { calls: unknown[][] } }) {
  const [url, init] = fetchSpy.mock.calls.at(-1) as [string, RequestInit];
  return { url, init, headers: new Headers(init.headers) };
}

describe("admin api", () => {
  it("exposes a stable locations query key", () => {
    expect(LOCATIONS_QUERY_KEY).toEqual(["locations"]);
  });

  it("loads location tags", async () => {
    const tags = [{ id: 1, name: "Toronto", countryCode: "CA" }];
    vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse(tags));
    const { result } = renderHook(() => useAdminLocations(), {
      wrapper: createQueryWrapper(),
    });
    await waitFor(() => expect(result.current.data).toEqual(tags));
  });

  it("creates a source with a camelCase body", async () => {
    const fetchSpy = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValue(jsonResponse(BACKEND_SOURCE, 201));
    const { result } = renderHook(() => useCreateSource(), {
      wrapper: createQueryWrapper(),
    });

    const created = await result.current.mutateAsync(INPUT);

    const { url, init, headers } = lastCall(fetchSpy);
    expect(url).toBe("/api/v1/admin/sources");
    expect(init.method).toBe("POST");
    expect(headers.get("X-CSRFToken")).toBe("tok");
    expect(JSON.parse(init.body as string)).toEqual({
      name: "Acme",
      platform: "GREENHOUSE",
      boardId: "acme",
      isActive: true,
    });
    expect(created).toEqual({ id: 3, ...INPUT });
  });

  it("updates a source", async () => {
    const fetchSpy = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValue(jsonResponse({ ...BACKEND_SOURCE, isActive: false }));
    const { result } = renderHook(() => useUpdateSource(), {
      wrapper: createQueryWrapper(),
    });

    const updated = await result.current.mutateAsync({
      id: 3,
      input: { ...INPUT, is_active: false },
    });

    const { url, init } = lastCall(fetchSpy);
    expect(url).toBe("/api/v1/admin/sources/3");
    expect(init.method).toBe("PUT");
    expect(updated.is_active).toBe(false);
  });

  it("deletes a source", async () => {
    const fetchSpy = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValue(jsonResponse(null, 204));
    const { result } = renderHook(() => useDeleteSource(), {
      wrapper: createQueryWrapper(),
    });

    await result.current.mutateAsync(3);

    const { url, init } = lastCall(fetchSpy);
    expect(url).toBe("/api/v1/admin/sources/3");
    expect(init.method).toBe("DELETE");
  });

  it("updates a location tag", async () => {
    const fetchSpy = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValue(jsonResponse({ id: 5, name: "Toronto", countryCode: "CA" }));
    const { result } = renderHook(() => useUpdateLocationTag(), {
      wrapper: createQueryWrapper(),
    });

    await result.current.mutateAsync({
      id: 5,
      update: { countryCode: "CA", regionCode: null, city: "Toronto" },
    });

    const { url, init } = lastCall(fetchSpy);
    expect(url).toBe("/api/v1/admin/locations/5");
    expect(init.method).toBe("PUT");
    expect(JSON.parse(init.body as string)).toEqual({
      countryCode: "CA",
      regionCode: null,
      city: "Toronto",
    });
  });

  it("triggers an ingestion run", async () => {
    const fetchSpy = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValue(jsonResponse({ id: 9, status: "PENDING" }, 202));
    const { result } = renderHook(() => useTriggerRun(), {
      wrapper: createQueryWrapper(),
    });

    await result.current.mutateAsync();

    const { url, init, headers } = lastCall(fetchSpy);
    expect(url).toBe("/api/v1/runs");
    expect(init.method).toBe("POST");
    expect(headers.get("X-CSRFToken")).toBe("tok");
  });
});
