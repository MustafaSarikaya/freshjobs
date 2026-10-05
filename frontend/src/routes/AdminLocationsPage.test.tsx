import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

import { useAdminLocations, useUpdateLocationTag } from "@/api/admin";

import { AdminLocationsPage } from "./AdminLocationsPage";

vi.mock("@/api/admin", () => ({
  useAdminLocations: vi.fn(),
  useUpdateLocationTag: vi.fn(),
}));

const TAGS = [
  { id: 1, name: "Toronto, ON", countryCode: "CA", regionCode: "CA-ON", city: "Toronto" },
  { id: 2, name: "Remote", countryCode: null, regionCode: null, city: null },
];

const update = vi.fn();

function mockTags(state: { data?: typeof TAGS; isLoading?: boolean; isError?: boolean }) {
  vi.mocked(useAdminLocations).mockReturnValue({
    data: state.data,
    isLoading: state.isLoading ?? false,
    isError: state.isError ?? false,
  } as ReturnType<typeof useAdminLocations>);
}

beforeEach(() => {
  vi.clearAllMocks();
  update.mockResolvedValue(TAGS[0]);
  vi.mocked(useUpdateLocationTag).mockReturnValue(
    { mutateAsync: update } as unknown as ReturnType<typeof useUpdateLocationTag>,
  );
});

describe("AdminLocationsPage", () => {
  it("shows loading, error, and empty states", () => {
    mockTags({ isLoading: true });
    const { rerender } = render(<AdminLocationsPage />);
    expect(screen.getByText(/loading location tags/i)).toBeInTheDocument();

    mockTags({ isError: true });
    rerender(<AdminLocationsPage />);
    expect(screen.getByRole("alert")).toHaveTextContent(/could not load location tags/i);

    mockTags({ data: [] });
    rerender(<AdminLocationsPage />);
    expect(screen.getByText(/no location tags/i)).toBeInTheDocument();
  });

  it("filters tags by name and by missing geography", async () => {
    mockTags({ data: TAGS });
    render(<AdminLocationsPage />);
    expect(screen.getAllByRole("row")).toHaveLength(3);

    await userEvent.type(screen.getByLabelText(/search/i), "toron");
    expect(screen.getByRole("row", { name: /toronto/i })).toBeInTheDocument();
    expect(screen.queryByRole("row", { name: /remote/i })).toBeNull();

    await userEvent.clear(screen.getByLabelText(/search/i));
    await userEvent.click(screen.getByLabelText(/only tags without a country/i));
    expect(screen.getByRole("row", { name: /remote/i })).toBeInTheDocument();
    expect(screen.queryByRole("row", { name: /toronto/i })).toBeNull();
  });

  it("saves edited geography for a row", async () => {
    mockTags({ data: TAGS });
    render(<AdminLocationsPage />);

    const row = screen.getByRole("row", { name: /remote/i });
    await userEvent.type(within(row).getByLabelText(/country for remote/i), "US");
    await userEvent.click(within(row).getByRole("button", { name: /save/i }));

    await waitFor(() =>
      expect(update).toHaveBeenCalledWith({
        id: 2,
        update: { countryCode: "US", regionCode: null, city: null },
      }),
    );
    expect(await within(row).findByText(/saved/i)).toBeInTheDocument();
  });

  it("shows a save error for the row", async () => {
    update.mockRejectedValue(new Error("boom"));
    mockTags({ data: TAGS });
    render(<AdminLocationsPage />);

    const row = screen.getByRole("row", { name: /toronto/i });
    await userEvent.click(within(row).getByRole("button", { name: /save/i }));

    expect(await within(row).findByText(/failed/i)).toBeInTheDocument();
  });

  it("sends region and city edits", async () => {
    mockTags({ data: TAGS });
    render(<AdminLocationsPage />);

    const row = screen.getByRole("row", { name: /remote/i });
    await userEvent.type(within(row).getByLabelText(/region for remote/i), "US-NY");
    await userEvent.type(within(row).getByLabelText(/city for remote/i), "New York");
    await userEvent.click(within(row).getByRole("button", { name: /save/i }));

    await waitFor(() =>
      expect(update).toHaveBeenCalledWith({
        id: 2,
        update: { countryCode: null, regionCode: "US-NY", city: "New York" },
      }),
    );
  });
});
