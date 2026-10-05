import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

import { useTriggerRun } from "@/api/admin";

import { useRuns } from "@/api/runs";
import type { Run } from "@/types/api";

import { RunsPage } from "./RunsPage";

vi.mock("@/api/runs", () => ({
  useRuns: vi.fn(),
}));
vi.mock("@/api/admin", () => ({
  useTriggerRun: vi.fn(),
}));

const trigger = vi.fn();

beforeEach(() => {
  trigger.mockReset().mockResolvedValue(undefined);
  vi.mocked(useTriggerRun).mockReturnValue(
    { mutateAsync: trigger, isPending: false } as unknown as ReturnType<typeof useTriggerRun>,
  );
});

const mockUseRuns = vi.mocked(useRuns);

function mockRunsState(state: {
  data?: Run[];
  isLoading?: boolean;
  isError?: boolean;
}) {
  mockUseRuns.mockReturnValue({
    data: state.data,
    isLoading: state.isLoading ?? false,
    isError: state.isError ?? false,
  } as ReturnType<typeof useRuns>);
}

describe("RunsPage", () => {
  it("triggers an ingestion run", async () => {
    mockRunsState({ data: [] });
    render(<RunsPage />);

    await userEvent.click(screen.getByRole("button", { name: /run ingestion now/i }));

    await waitFor(() => expect(trigger).toHaveBeenCalledOnce());
    expect(await screen.findByText(/ingestion started/i)).toBeInTheDocument();
  });

  it("reports a failed trigger", async () => {
    trigger.mockRejectedValue(new Error("boom"));
    mockRunsState({ data: [] });
    render(<RunsPage />);

    await userEvent.click(screen.getByRole("button", { name: /run ingestion now/i }));

    expect(await screen.findByText(/could not start ingestion/i)).toBeInTheDocument();
  });

  it("disables the trigger while a run is being started", () => {
    vi.mocked(useTriggerRun).mockReturnValue(
      { mutateAsync: trigger, isPending: true } as unknown as ReturnType<typeof useTriggerRun>,
    );
    mockRunsState({ data: [] });
    render(<RunsPage />);

    expect(screen.getByRole("button", { name: /starting/i })).toBeDisabled();
  });

  it("shows a loading state", () => {
    mockRunsState({ isLoading: true });
    render(<RunsPage />);
    expect(screen.getByRole("heading", { name: /ingestion runs/i })).toBeInTheDocument();
    expect(screen.getByText(/loading runs/i)).toBeInTheDocument();
  });

  it("shows an error state", () => {
    mockRunsState({ isError: true });
    render(<RunsPage />);
    expect(screen.getByRole("alert")).toHaveTextContent(/could not load/i);
  });

  it("shows an empty state", () => {
    mockRunsState({ data: [] });
    render(<RunsPage />);
    expect(screen.getByText(/no ingestion runs yet/i)).toBeInTheDocument();
  });

  it("renders runs in newest-first table shape", () => {
    mockRunsState({
      data: [
        {
          id: 10,
          status: "completed",
          started_at: "2025-01-01T00:00:00Z",
          finished_at: "2025-01-01T00:02:00Z",
          sources_processed: 3,
          listings_created: 8,
          listings_updated: 5,
          listings_expired: 2,
          error_message: null,
          created_at: "2025-01-01T00:00:00Z",
        },
        {
          id: 9,
          status: "failed",
          started_at: null,
          finished_at: null,
          sources_processed: 0,
          listings_created: 0,
          listings_updated: 0,
          listings_expired: 0,
          error_message: "Connection refused",
          created_at: "2024-12-31T23:00:00Z",
        },
      ],
    });

    render(<RunsPage />);

    const rows = screen.getAllByRole("row");
    expect(rows).toHaveLength(3);
    expect(within(rows[1]).getByText("#10")).toBeInTheDocument();
    expect(within(rows[1]).getByText("completed")).toBeInTheDocument();
    expect(within(rows[1]).getByText("Jan 01, 2025 00:00")).toBeInTheDocument();
    expect(within(rows[1]).getByText("3")).toBeInTheDocument();
    expect(within(rows[1]).getByText("8")).toBeInTheDocument();
    expect(within(rows[1]).getByText("5")).toBeInTheDocument();
    expect(within(rows[1]).getByText("2")).toBeInTheDocument();
    expect(within(rows[1]).getByText("—")).toBeInTheDocument();

    expect(within(rows[2]).getByText("#9")).toBeInTheDocument();
    expect(within(rows[2]).getByText("failed")).toBeInTheDocument();
    expect(within(rows[2]).getAllByText("—")).toHaveLength(2);
    expect(within(rows[2]).getByText("Connection refused")).toBeInTheDocument();
  });
});
