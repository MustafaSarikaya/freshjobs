import { getCsrfToken } from "./csrf";

export class LoginError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "LoginError";
  }
}

export type LoginInput = {
  username: string;
  password: string;
  next?: string;
};

/* v8 ignore start -- unit tests inject redirect callbacks; browser navigation is covered by Playwright. */
function browserRedirect(url: string) {
  window.location.assign(url);
}
/* v8 ignore stop */

async function ensureCsrfCookie() {
  if (getCsrfToken()) {
    return;
  }
  await fetch("/api/v1/health", {
    method: "GET",
    credentials: "same-origin",
  });
}

export async function login({ username, password, next }: LoginInput) {
  await ensureCsrfCookie();

  const headers = new Headers({
    "Content-Type": "application/json",
  });
  const token = getCsrfToken();
  if (token) {
    headers.set("X-CSRFToken", token);
  }

  const response = await fetch("/api/v1/auth/login", {
    method: "POST",
    headers,
    body: JSON.stringify({ username, password }),
    credentials: "same-origin",
  });

  if (response.status === 403) {
    throw new LoginError("Your sign-in session expired. Reload and try again.");
  }
  if (response.status === 401) {
    throw new LoginError("Please enter a correct username and password.");
  }
  if (!response.ok) {
    throw new LoginError(`Sign in failed with HTTP ${response.status}.`);
  }

  return next ?? "/";
}

export async function logout(
  redirect: (url: string) => void = browserRedirect,
) {
  const headers = new Headers();
  const token = getCsrfToken();
  if (token) {
    headers.set("X-CSRFToken", token);
  }
  await fetch("/api/v1/auth/logout", {
    method: "POST",
    headers,
    credentials: "same-origin",
  });
  redirect("/admin/login/");
}
