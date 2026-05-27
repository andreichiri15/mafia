import type { ReactNode } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { useAuthStore } from "../store/authStore";

interface RequireAuthProps {
  children: ReactNode;
}

/**
 * Route guard. Redirects to /signin (with a redirect param back to the
 * requested page) when the user is not logged in. Auth state is resolved
 * synchronously at module-load time, so there is no flicker on refresh.
 */
export function RequireAuth({ children }: RequireAuthProps) {
  const isLoggedIn = useAuthStore((s) => s.isLoggedIn);
  const location = useLocation();

  if (!isLoggedIn) {
    const target = location.pathname + location.search;
    return <Navigate to={`/signin?redirect=${encodeURIComponent(target)}`} replace />;
  }

  return <>{children}</>;
}
