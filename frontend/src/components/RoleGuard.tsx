import { useAuth } from "@/contexts/AuthContext";
import type { UserRole } from "@/lib/types";

interface Props {
  roles: UserRole[];
  children: React.ReactNode;
  fallback?: React.ReactNode;
}

/**
 * Renders children only if the logged-in user has one of the given roles.
 */
export function RoleGuard({ roles, children, fallback = null }: Props) {
  const { user } = useAuth();
  if (!user || !roles.includes(user.role)) return <>{fallback}</>;
  return <>{children}</>;
}
