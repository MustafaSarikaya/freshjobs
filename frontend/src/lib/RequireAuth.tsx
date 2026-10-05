import { Navigate, Outlet, useLocation } from "react-router-dom";

import { useCurrentUser } from "@/api/auth";
import { ApiError } from "./fetcher";

function isAuthError(error: unknown) {
  return error instanceof ApiError && (error.status === 401 || error.status === 403);
}

/** Guards the admin area: only staff and superusers get through. */
export function RequireAuth() {
  const location = useLocation();
  const { data: user, error, isLoading } = useCurrentUser();

  if (isLoading) {
    return null;
  }

  const isNonAdmin = user !== undefined && !user.isStaff && !user.isSuperuser;
  if (isAuthError(error) || isNonAdmin) {
    const next = `${location.pathname}${location.search}`;
    return (
      <Navigate
        to={`/admin/login/?next=${encodeURIComponent(next)}`}
        replace
      />
    );
  }

  return <Outlet />;
}
