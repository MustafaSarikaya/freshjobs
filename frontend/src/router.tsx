import { Navigate, createBrowserRouter } from "react-router-dom";

import { App } from "@/App";
import { AdminShell } from "@/components/AdminShell";
import { AppShell } from "@/components/AppShell";
import { AdminLocationsPage } from "@/routes/AdminLocationsPage";
import { AdminSourcesPage } from "@/routes/AdminSourcesPage";
import { RequireAuth } from "@/lib/RequireAuth";
import { JobsPage } from "@/routes/JobsPage";
import { LoginPage } from "@/routes/LoginPage";
import { RunsPage } from "@/routes/RunsPage";
import { SourcesPage } from "@/routes/SourcesPage";

export const router = createBrowserRouter([
  {
    element: <AppShell />,
    children: [
      {
        path: "/",
        element: <JobsPage />,
      },
      {
        path: "/sources",
        element: <SourcesPage />,
      },
    ],
  },
  {
    path: "/admin/login",
    element: <LoginPage />,
  },
  {
    element: <RequireAuth />,
    children: [
      {
        element: <AdminShell />,
        children: [
          {
            path: "/admin",
            element: <Navigate to="/admin/runs" replace />,
          },
          {
            path: "/admin/runs",
            element: <RunsPage />,
          },
          {
            path: "/admin/sources",
            element: <AdminSourcesPage />,
          },
          {
            path: "/admin/locations",
            element: <AdminLocationsPage />,
          },
        ],
      },
    ],
  },
  {
    path: "*",
    element: <App />,
  },
]);
