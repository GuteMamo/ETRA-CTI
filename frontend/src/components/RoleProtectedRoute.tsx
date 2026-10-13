import { Navigate } from "react-router-dom";
import { useAuth } from "@/contexts/AuthContext";
import type { UserRole } from "@/lib/types";

interface Props {
  roles: UserRole[];
  children: React.ReactNode;
}

/**
 * Redirects to / if the logged-in user's role is not in the allowed list.
 */
export function RoleProtectedRoute({ roles, children }: Props) {
  const { user, loading } = useAuth();
  if (loading) return <div className="p-8 text-slate-500">Loading…</div>;
  if (!user) return <Navigate to="/login" replace />;
  if (!roles.includes(user.role)) return <Navigate to="/" replace />;
  return <>{children}</>;
}
