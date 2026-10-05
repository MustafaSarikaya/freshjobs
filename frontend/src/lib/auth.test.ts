import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { login, logout } from "./auth";

function setCookie(value: string) {
  Object.defineProperty(document, "cookie", {
    configurable: true,
    get: () => value,
  });
}

function response({
  status = 200,
}: {
  status?: number;
}) {
  const result = new Response(status === 204 ? null : "", { status });
  return result;
}

beforeEach(() => {
  setCookie("csrftoken=login-token");
});

afterEach(() => {
  vi.restoreAllMocks();
  setCookie("");
});

describe("login", () => {
  it("posts JSON credentials with CSRF and returns next", async () => {
    const spy = vi.spyOn(globalThis, "fetch").mockResolvedValue(response({}));

    const redirectTo = await login({
      username: "e2e",
      password: "e2e-pass-123",
      next: "/sources/",
    });

    expect(redirectTo).toBe("/sources/");
    expect(spy.mock.calls[0][0]).toBe("/api/v1/auth/login");
    const init = spy.mock.calls[0][1]!;
    const headers = new Headers(init.headers);
    expect(headers.get("Content-Type")).toBe("application/json");
    expect(headers.get("X-CSRFToken")).toBe("login-token");
    expect(init.credentials).toBe("same-origin");
    expect(JSON.parse(init.body as string)).toEqual({
      username: "e2e",
      password: "e2e-pass-123",
    });
  });

  it("warms the CSRF cookie when it is missing", async () => {
    setCookie("");
    const spy = vi.spyOn(globalThis, "fetch").mockResolvedValue(response({}));

    const redirectTo = await login({ username: "e2e", password: "secret" });

    expect(redirectTo).toBe("/");
    expect(spy.mock.calls[0][0]).toBe("/api/v1/health");
    expect(spy.mock.calls[1][0]).toBe("/api/v1/auth/login");
    const init = spy.mock.calls[1][1]!;
    expect(new Headers(init.headers).has("X-CSRFToken")).toBe(false);
  });

  it("reports invalid credentials", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(response({ status: 401 }));

    await expect(
      login({ username: "e2e", password: "wrong", next: "/" }),
    ).rejects.toThrow("Please enter a correct username and password.");
  });

  it("reports expired CSRF sessions", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(response({ status: 403 }));

    await expect(login({ username: "e2e", password: "secret" })).rejects.toThrow(
      "Your sign-in session expired. Reload and try again.",
    );
  });

  it("reports unexpected failed responses", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(response({ status: 500 }));

    await expect(login({ username: "e2e", password: "secret" })).rejects.toThrow(
      "Sign in failed with HTTP 500.",
    );
  });
});

describe("logout", () => {
  it("posts to the session logout endpoint and redirects to login", async () => {
    const spy = vi.spyOn(globalThis, "fetch").mockResolvedValue(response({ status: 204 }));
    const redirect = vi.fn();

    await logout(redirect);

    expect(spy.mock.calls[0][0]).toBe("/api/v1/auth/logout");
    expect(spy.mock.calls[0][1]).toMatchObject({
      method: "POST",
      credentials: "same-origin",
    });
    expect(new Headers(spy.mock.calls[0][1]?.headers).get("X-CSRFToken")).toBe(
      "login-token",
    );
    expect(redirect).toHaveBeenCalledWith("/admin/login/");
  });
});
