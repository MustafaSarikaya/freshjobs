import { Link, Outlet } from "react-router-dom";

import { logout } from "@/lib/auth";

type AdminShellProps = {
  onSignOut?: () => void;
};

export function AdminShell({ onSignOut = () => void logout() }: AdminShellProps) {
  return (
    <div className="min-h-screen flex flex-col bg-background text-foreground">
      <header className="border-b border-border">
        <nav className="mx-auto flex w-full max-w-screen-2xl flex-wrap items-center gap-x-5 gap-y-2 px-4 py-3 text-sm sm:px-6 lg:px-8">
          <a href="/" className="font-semibold">
            Fresh Jobs
          </a>
          <span className="text-muted-foreground">Admin</span>
          <Link
            to="/admin/runs"
            className="text-muted-foreground hover:text-foreground"
          >
            Runs
          </Link>
          <Link
            to="/admin/sources"
            className="text-muted-foreground hover:text-foreground"
          >
            Sources
          </Link>
          <Link
            to="/admin/locations"
            className="text-muted-foreground hover:text-foreground"
          >
            Locations
          </Link>
          <button
            type="button"
            onClick={onSignOut}
            className="ml-auto text-muted-foreground hover:text-foreground"
          >
            Sign out
          </button>
        </nav>
      </header>
      <main className="mx-auto w-full max-w-screen-2xl flex-1 px-4 py-4 sm:px-6 sm:py-6 lg:px-8">
        <Outlet />
      </main>
    </div>
  );
}
