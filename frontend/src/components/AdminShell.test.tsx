import { describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";

import { AdminShell } from "./AdminShell";

const { mockLogout } = vi.hoisted(() => ({ mockLogout: vi.fn() }));
vi.mock("@/lib/auth", () => ({ logout: mockLogout }));

function renderShell(onSignOut = vi.fn()) {
  renderWithProps({ onSignOut });
  return onSignOut;
}

function renderWithProps(props: { onSignOut?: () => void }) {
  render(
    <MemoryRouter
      initialEntries={["/admin/runs"]}
      future={{ v7_relativeSplatPath: true, v7_startTransition: true }}
    >
      <Routes>
        <Route element={<AdminShell {...props} />}>
          <Route path="/admin/runs" element={<p>admin body</p>} />
        </Route>
      </Routes>
    </MemoryRouter>,
  );
}

describe("AdminShell", () => {
  it("links back to the site and to the runs page", () => {
    renderShell();
    expect(screen.getByRole("link", { name: /fresh jobs/i })).toHaveAttribute(
      "href",
      "/",
    );
    expect(screen.getByRole("link", { name: /^runs$/i })).toHaveAttribute(
      "href",
      "/admin/runs",
    );
    expect(screen.getByRole("link", { name: /^sources$/i })).toHaveAttribute(
      "href",
      "/admin/sources",
    );
    expect(screen.getByRole("link", { name: /^locations$/i })).toHaveAttribute(
      "href",
      "/admin/locations",
    );
  });

  it("renders the child route via Outlet", () => {
    renderShell();
    expect(screen.getByText("admin body")).toBeInTheDocument();
  });

  it("signs out", async () => {
    const onSignOut = renderShell();
    await userEvent.click(screen.getByRole("button", { name: /sign out/i }));
    expect(onSignOut).toHaveBeenCalledOnce();
  });

  it("logs out by default", async () => {
    renderWithProps({});
    await userEvent.click(screen.getByRole("button", { name: /sign out/i }));
    expect(mockLogout).toHaveBeenCalledOnce();
  });
});
