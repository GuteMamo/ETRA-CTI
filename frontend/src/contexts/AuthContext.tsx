import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import { api, tokenStore, extractError } from "@/lib/api";
import type { UserDto, TokenPair, UserRole } from "@/lib/types";

interface AuthCtx {
  user: UserDto | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
  hasRole: (roles: UserRole[]) => boolean;
}

const Ctx = createContext<AuthCtx | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserDto | null>(null);
  const [loading, setLoading] = useState(true);

  async function refreshUser() {
    try {
      const res = await api.get<UserDto>("/auth/me");
      setUser(res.data);
    } catch {
      setUser(null);
      tokenStore.clear();
    }
  }

  async function login(email: string, password: string) {
    try {
      const res = await api.post<TokenPair>("/auth/login", { email, password });
      tokenStore.set(res.data);
      await refreshUser();
    } catch (e) {
      throw new Error(extractError(e));
    }
  }

  function logout() {
    tokenStore.clear();
    setUser(null);
  }

  function hasRole(roles: UserRole[]) {
    if (!user) return false;
    return roles.includes(user.role);
  }

  useEffect(() => {
    (async () => {
      if (tokenStore.getAccess()) await refreshUser();
      setLoading(false);
    })();
  }, []);

  return (
    <Ctx.Provider value={{ user, loading, login, logout, hasRole }}>
      {children}
    </Ctx.Provider>
  );
}

export function useAuth() {
  const c = useContext(Ctx);
  if (!c) throw new Error("useAuth must be used inside AuthProvider");
  return c;
}
