import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

import { useCreateSource, useDeleteSource, useUpdateSource } from "@/api/admin";
import { useSources } from "@/api/sources";
import { ApiError } from "@/lib/fetcher";
import type { Source } from "@/types/api";

import { AdminSourcesPage } from "./AdminSourcesPage";

vi.mock("@/api/sources", () => ({ useSources: vi.fn() }));
vi.mock("@/api/admin", () => ({
  useCreateSource: vi.fn(),
  useUpdateSource: vi.fn(),
  useDeleteSource: vi.fn(),
}));

const SOURCE: Source = {
  id: 3,
  name: "Acme",
  platform: "GREENHOUSE",
  board_id: "acme",
  is_active: true,
};

const create = vi.fn();
const update = vi.fn();
const remove = vi.fn();

function mockSources(state: { data?: Source[]; isLoading?: boolean; isError?: boolean }) {
  vi.mocked(useSources).mockReturnValue({
    data: state.data,
    isLoading: state.isLoading ?? false,
    isError: state.isError ?? false,
  } as ReturnType<typeof useSources>);
}

beforeEach(() => {
  vi.clearAllMocks();
  create.mockResolvedValue(SOURCE);
  update.mockResolvedValue(SOURCE);
  remove.mockResolvedValue(undefined);
  vi.mocked(useCreateSource).mockReturnValue(
    { mutateAsync: create } as unknown as ReturnType<typeof useCreateSource>,
  );
  vi.mocked(useUpdateSource).mockReturnValue(
    { mutateAsync: update } as unknown as ReturnType<typeof useUpdateSource>,
  );
  vi.mocked(useDeleteSource).mockReturnValue(
    { mutateAsync: remove } as unknown as ReturnType<typeof useDeleteSource>,
  );
});

describe("AdminSourcesPage", () => {
  it("shows loading, error, and empty states", () => {
    mockSources({ isLoading: true });
    const { rerender } = render(<AdminSourcesPage />);
    expect(screen.getByText(/loading sources/i)).toBeInTheDocument();

    mockSources({ isError: true });
    rerender(<AdminSourcesPage />);
    expect(screen.getByRole("alert")).toHaveTextContent(/could not load sources/i);

    mockSources({ data: [] });
    rerender(<AdminSourcesPage />);
    expect(screen.getByText(/no sources yet/i)).toBeInTheDocument();
  });

  it("lists sources with their platform label and status", () => {
    mockSources({ data: [SOURCE, { ...SOURCE, id: 4, name: "Beta", platform: "PHENOM", board_id: "beta", is_active: false }] });
    render(<AdminSourcesPage />);
    const acme = screen.getByRole("row", { name: /acme/i });
    expect(within(acme).getByText("Greenhouse")).toBeInTheDocument();
    expect(within(acme).getByText("Active")).toBeInTheDocument();
    const beta = screen.getByRole("row", { name: /beta/i });
    expect(within(beta).getByText("Phenom People")).toBeInTheDocument();
    expect(within(beta).getByText("Inactive")).toBeInTheDocument();
  });

  it("adds a source", async () => {
    mockSources({ data: [] });
    render(<AdminSourcesPage />);

    await userEvent.click(screen.getByRole("button", { name: /add source/i }));
    const dialog = screen.getByRole("dialog");
    await userEvent.type(within(dialog).getByLabelText("Name"), "Gamma");
    await userEvent.selectOptions(within(dialog).getByLabelText("Platform"), "LEVER");
    await userEvent.type(within(dialog).getByLabelText("Board ID"), "gamma");
    await userEvent.click(within(dialog).getByRole("button", { name: /save/i }));

    await waitFor(() =>
      expect(create).toHaveBeenCalledWith({
        name: "Gamma",
        platform: "LEVER",
        board_id: "gamma",
        is_active: true,
      }),
    );
    await waitFor(() => expect(screen.queryByRole("dialog")).toBeNull());
  });

  it("requires name and board id", async () => {
    mockSources({ data: [] });
    render(<AdminSourcesPage />);

    await userEvent.click(screen.getByRole("button", { name: /add source/i }));
    await userEvent.click(screen.getByRole("button", { name: /save/i }));

    expect(await screen.findByText(/name and board id are required/i)).toBeInTheDocument();
    expect(create).not.toHaveBeenCalled();
  });

  it("edits a source", async () => {
    mockSources({ data: [SOURCE] });
    render(<AdminSourcesPage />);

    await userEvent.click(screen.getByRole("button", { name: /edit acme/i }));
    const dialog = screen.getByRole("dialog");
    const name = within(dialog).getByLabelText("Name");
    await userEvent.clear(name);
    await userEvent.type(name, "Acme Inc");
    await userEvent.click(within(dialog).getByLabelText(/active/i));
    await userEvent.click(within(dialog).getByRole("button", { name: /save/i }));

    await waitFor(() =>
      expect(update).toHaveBeenCalledWith({
        id: 3,
        input: { name: "Acme Inc", platform: "GREENHOUSE", board_id: "acme", is_active: false },
      }),
    );
  });

  it("shows a conflict message from the server", async () => {
    update.mockRejectedValue(new ApiError(409, "HTTP 409"));
    mockSources({ data: [SOURCE] });
    render(<AdminSourcesPage />);

    await userEvent.click(screen.getByRole("button", { name: /edit acme/i }));
    await userEvent.click(screen.getByRole("button", { name: /save/i }));

    expect(await screen.findByText(/another source already uses this board/i)).toBeInTheDocument();
  });

  it("shows a generic save error", async () => {
    update.mockRejectedValue(new Error("boom"));
    mockSources({ data: [SOURCE] });
    render(<AdminSourcesPage />);

    await userEvent.click(screen.getByRole("button", { name: /edit acme/i }));
    await userEvent.click(screen.getByRole("button", { name: /save/i }));

    expect(await screen.findByText(/could not save the source/i)).toBeInTheDocument();
  });

  it("cancels editing", async () => {
    mockSources({ data: [SOURCE] });
    render(<AdminSourcesPage />);

    await userEvent.click(screen.getByRole("button", { name: /edit acme/i }));
    await userEvent.click(screen.getByRole("button", { name: /cancel/i }));

    await waitFor(() => expect(screen.queryByRole("dialog")).toBeNull());
    expect(update).not.toHaveBeenCalled();
  });

  it("deletes a source after confirmation", async () => {
    mockSources({ data: [SOURCE] });
    render(<AdminSourcesPage />);

    await userEvent.click(screen.getByRole("button", { name: /delete acme/i }));
    const dialog = screen.getByRole("dialog");
    expect(dialog).toHaveTextContent(/all of its job listings/i);
    await userEvent.click(within(dialog).getByRole("button", { name: /^delete$/i }));

    await waitFor(() => expect(remove).toHaveBeenCalledWith(3));
    await waitFor(() => expect(screen.queryByRole("dialog")).toBeNull());
  });

  it("shows a delete error", async () => {
    remove.mockRejectedValue(new Error("boom"));
    mockSources({ data: [SOURCE] });
    render(<AdminSourcesPage />);

    await userEvent.click(screen.getByRole("button", { name: /delete acme/i }));
    await userEvent.click(screen.getByRole("button", { name: /^delete$/i }));

    expect(await screen.findByText(/could not delete the source/i)).toBeInTheDocument();
  });

  it("closes the delete confirmation without deleting", async () => {
    mockSources({ data: [SOURCE] });
    render(<AdminSourcesPage />);

    await userEvent.click(screen.getByRole("button", { name: /delete acme/i }));
    await userEvent.click(screen.getByRole("button", { name: /cancel/i }));

    await waitFor(() => expect(screen.queryByRole("dialog")).toBeNull());
    expect(remove).not.toHaveBeenCalled();
  });

  it("closes both dialogs with Escape", async () => {
    mockSources({ data: [SOURCE] });
    render(<AdminSourcesPage />);

    await userEvent.click(screen.getByRole("button", { name: /edit acme/i }));
    await userEvent.keyboard("{Escape}");
    await waitFor(() => expect(screen.queryByRole("dialog")).toBeNull());

    await userEvent.click(screen.getByRole("button", { name: /delete acme/i }));
    await userEvent.keyboard("{Escape}");
    await waitFor(() => expect(screen.queryByRole("dialog")).toBeNull());
  });
});
