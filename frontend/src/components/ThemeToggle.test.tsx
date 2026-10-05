import { beforeEach, describe, expect, it } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";

import { THEME_STORAGE_KEY } from "@/lib/theme";

import { ThemeToggle } from "./ThemeToggle";

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

describe("ThemeToggle", () => {
  beforeEach(() => {
    installLocalStorage();
    document.documentElement.classList.remove("dark");
  });

  it("switches from light to dark and back", () => {
    render(<ThemeToggle />);
    fireEvent.click(screen.getByRole("button", { name: /switch to dark mode/i }));
    expect(document.documentElement).toHaveClass("dark");
    expect(localStorage.getItem(THEME_STORAGE_KEY)).toBe("dark");

    fireEvent.click(screen.getByRole("button", { name: /switch to light mode/i }));
    expect(document.documentElement).not.toHaveClass("dark");
    expect(localStorage.getItem(THEME_STORAGE_KEY)).toBe("light");
  });

  it("starts from the stored theme", () => {
    localStorage.setItem(THEME_STORAGE_KEY, "dark");
    render(<ThemeToggle />);
    expect(
      screen.getByRole("button", { name: /switch to light mode/i }),
    ).toBeInTheDocument();
  });
});
