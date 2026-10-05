import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { applyTheme, getInitialTheme, setTheme, THEME_STORAGE_KEY } from "./theme";

function installLocalStorage() {
  let state: Record<string, string> = {};
  Object.defineProperty(window, "localStorage", {
    configurable: true,
    value: {
      getItem: (key: string) => state[key] ?? null,
      setItem: (key: string, value: string) => {
        state[key] = value;
      },
      removeItem: (key: string) => {
        delete state[key];
      },
      clear: () => {
        state = {};
      },
    },
  });
}

function mockMatchMedia(matches: boolean) {
  vi.stubGlobal(
    "matchMedia",
    vi.fn().mockReturnValue({ matches } as MediaQueryList),
  );
}

describe("theme", () => {
  beforeEach(() => {
    installLocalStorage();
    document.documentElement.classList.remove("dark");
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("prefers the stored theme", () => {
    mockMatchMedia(false);
    localStorage.setItem(THEME_STORAGE_KEY, "dark");
    expect(getInitialTheme()).toBe("dark");
  });

  it("falls back to the system preference", () => {
    mockMatchMedia(true);
    expect(getInitialTheme()).toBe("dark");
    mockMatchMedia(false);
    expect(getInitialTheme()).toBe("light");
  });

  it("ignores invalid stored values", () => {
    mockMatchMedia(false);
    localStorage.setItem(THEME_STORAGE_KEY, "purple");
    expect(getInitialTheme()).toBe("light");
  });

  it("defaults to light when matchMedia is unavailable", () => {
    vi.stubGlobal("matchMedia", undefined);
    expect(getInitialTheme()).toBe("light");
  });

  it("defaults to light when storage throws", () => {
    mockMatchMedia(false);
    vi.spyOn(window.localStorage, "getItem").mockImplementationOnce(() => {
      throw new Error("blocked");
    });
    expect(getInitialTheme()).toBe("light");
  });

  it("applies the theme to the document root", () => {
    applyTheme("dark");
    expect(document.documentElement).toHaveClass("dark");
    expect(document.documentElement.style.colorScheme).toBe("dark");
    applyTheme("light");
    expect(document.documentElement).not.toHaveClass("dark");
    expect(document.documentElement.style.colorScheme).toBe("light");
  });

  it("persists and applies the chosen theme", () => {
    setTheme("dark");
    expect(localStorage.getItem(THEME_STORAGE_KEY)).toBe("dark");
    expect(document.documentElement).toHaveClass("dark");
  });

  it("still applies the theme when storage throws", () => {
    vi.spyOn(window.localStorage, "setItem").mockImplementationOnce(() => {
      throw new Error("blocked");
    });
    setTheme("dark");
    expect(document.documentElement).toHaveClass("dark");
  });
});
