import { Link, Outlet } from "react-router-dom";

import { ThemeToggle } from "@/components/ThemeToggle";

export function AppShell() {
  return (
    <div className="min-h-screen flex flex-col bg-background text-foreground">
      <header className="border-b border-border">
        <nav className="mx-auto flex w-full max-w-screen-2xl flex-wrap items-center gap-x-5 gap-y-2 px-4 py-3 text-sm sm:px-6 lg:px-8">
          <a href="/" className="font-semibold">
            Fresh Jobs
          </a>
          <a href="/" className="text-muted-foreground hover:text-foreground">
            Jobs
          </a>
          <Link
            to="/sources"
            className="text-muted-foreground hover:text-foreground"
          >
            Sources
          </Link>
          <div className="ml-auto">
            <ThemeToggle />
          </div>
        </nav>
      </header>
      <main className="mx-auto w-full max-w-screen-2xl flex-1 px-4 py-4 sm:px-6 sm:py-6 lg:px-8">
        <Outlet />
      </main>
    </div>
  );
}
