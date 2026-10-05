import { afterEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";

import { LoginError, login } from "@/lib/auth";

import { LoginPage } from "./LoginPage";

vi.mock("@/lib/auth", async () => {
  const actual = await vi.importActual<typeof import("@/lib/auth")>("@/lib/auth");
  return {
    ...actual,
    login: vi.fn(),
  };
});

const mockLogin = vi.mocked(login);

afterEach(() => {
  vi.clearAllMocks();
});

function renderLoginPage(initialPath = "/admin/login/?next=/admin/runs") {
  const onRedirect = vi.fn();
  render(
    <MemoryRouter
      initialEntries={[initialPath]}
      future={{ v7_relativeSplatPath: true, v7_startTransition: true }}
    >
      <Routes>
        <Route
          path="/admin/login/"
          element={<LoginPage onRedirect={onRedirect} />}
        />
      </Routes>
    </MemoryRouter>,
  );
  return onRedirect;
}

describe("LoginPage", () => {
  it("renders username and password fields with Django-compatible names", () => {
    renderLoginPage();
    expect(screen.getByText("Fresh Jobs")).toBeInTheDocument();
    expect(screen.getByLabelText("Username")).toHaveAttribute("name", "username");
    expect(screen.getByLabelText("Password")).toHaveAttribute("name", "password");
  });

  it("submits credentials and redirects to next", async () => {
    mockLogin.mockResolvedValue("/admin/runs");
    const onRedirect = renderLoginPage();

    await userEvent.type(screen.getByLabelText("Username"), "e2e");
    await userEvent.type(screen.getByLabelText("Password"), "e2e-pass-123");
    await userEvent.click(screen.getByRole("button", { name: "Sign in" }));

    await waitFor(() =>
      expect(mockLogin).toHaveBeenCalledWith({
        username: "e2e",
        password: "e2e-pass-123",
        next: "/admin/runs",
      }),
    );
    expect(onRedirect).toHaveBeenCalledWith("/admin/runs");
  });

  it("defaults next to /admin/", async () => {
    mockLogin.mockResolvedValue("/admin/");
    renderLoginPage("/admin/login/");

    await userEvent.type(screen.getByLabelText("Username"), "e2e");
    await userEvent.type(screen.getByLabelText("Password"), "e2e-pass-123");
    await userEvent.click(screen.getByRole("button", { name: "Sign in" }));

    await waitFor(() =>
      expect(mockLogin).toHaveBeenCalledWith({
        username: "e2e",
        password: "e2e-pass-123",
        next: "/admin/",
      }),
    );
  });

  it("shows validation errors before submit", async () => {
    renderLoginPage();

    await userEvent.click(screen.getByRole("button", { name: "Sign in" }));

    expect(await screen.findByText("Username is required")).toBeInTheDocument();
    expect(screen.getByText("Password is required")).toBeInTheDocument();
    expect(mockLogin).not.toHaveBeenCalled();
  });

  it("shows login errors returned by the auth helper", async () => {
    mockLogin.mockRejectedValue(
      new LoginError("Please enter a correct username and password."),
    );
    renderLoginPage();

    await userEvent.type(screen.getByLabelText("Username"), "e2e");
    await userEvent.type(screen.getByLabelText("Password"), "wrong");
    await userEvent.click(screen.getByRole("button", { name: "Sign in" }));

    expect(
      await screen.findByText("Please enter a correct username and password."),
    ).toBeInTheDocument();
  });

  it("shows a generic error for unexpected failures", async () => {
    mockLogin.mockRejectedValue(new Error("network down"));
    renderLoginPage();

    await userEvent.type(screen.getByLabelText("Username"), "e2e");
    await userEvent.type(screen.getByLabelText("Password"), "secret");
    await userEvent.click(screen.getByRole("button", { name: "Sign in" }));

    expect(await screen.findByText("Sign in failed.")).toBeInTheDocument();
  });
});
