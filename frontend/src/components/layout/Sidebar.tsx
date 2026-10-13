import { NavLink, useNavigate } from "react-router-dom";
import {
  LayoutDashboard, AlertTriangle, Swords, Search, FileCheck,
  Upload, Smartphone, Shield, Target, LogOut, Users as UsersIcon
} from "lucide-react";
import { cn } from "@/lib/utils";
import { useAuth } from "@/contexts/AuthContext";
import type { UserRole } from "@/lib/types";

interface LinkDef {
  to: string;
  label: string;
  icon: typeof LayoutDashboard;
  end?: boolean;
  roles: (UserRole | "*")[];
}

const links: LinkDef[] = [
  { to: "/", label: "Dashboard", icon: LayoutDashboard, end: true, roles: ["*"] },
  { to: "/findings", label: "Findings", icon: AlertTriangle, roles: ["*"] },
  { to: "/campaigns", label: "Attack Campaigns", icon: Swords,
    roles: ["ADMIN", "SECURITY_MANAGER", "ANALYST", "PENTESTER"] },
  { to: "/investigations", label: "Investigations", icon: Search,
    roles: ["ADMIN", "SECURITY_MANAGER", "ANALYST"] },
  // Remediation is workflow-only — VIEWER excluded
  { to: "/remediations", label: "Remediation", icon: FileCheck,
    roles: ["ADMIN", "SECURITY_MANAGER", "PENTESTER", "ANALYST", "DEVELOPER"] },
  { to: "/imports", label: "Imports", icon: Upload,
    roles: ["ADMIN", "SECURITY_MANAGER", "PENTESTER", "ANALYST"] },
  { to: "/mobile", label: "Mobile Apps", icon: Smartphone,
    roles: ["ADMIN", "SECURITY_MANAGER", "PENTESTER", "ANALYST"] },
  { to: "/iocs", label: "Threat Intel", icon: Target,
    roles: ["ADMIN", "SECURITY_MANAGER", "ANALYST"] },
  { to: "/users", label: "Users & Roles", icon: UsersIcon, roles: ["ADMIN"] },
  { to: "/audit", label: "Audit Log", icon: Shield,
    roles: ["ADMIN", "SECURITY_MANAGER"] },
];

export function Sidebar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const visibleLinks = links.filter((l) =>
    l.roles.includes("*") || (user && l.roles.includes(user.role))
  );

  return (
    <aside className="w-64 shrink-0 border-r border-slate-800 bg-slate-950/60 backdrop-blur flex flex-col">
      <div className="h-16 flex items-center gap-2 px-6 border-b border-slate-800">
        <div className="h-8 w-8 rounded-md bg-gradient-to-br from-blue-500 to-blue-700 flex items-center justify-center">
          <Shield className="h-4 w-4 text-white" />
        </div>
        <div>
          <div className="text-sm font-bold text-white leading-tight">ETRA-CTI</div>
          <div className="text-[10px] uppercase tracking-wider text-slate-500">Security Platform</div>
        </div>
      </div>

      <nav className="flex-1 p-3 space-y-1 overflow-y-auto">
        {visibleLinks.map(({ to, label, icon: Icon, end }) => (
          <NavLink
            key={to}
            to={to}
            end={end}
            className={({ isActive }) =>
              cn(
                "flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors",
                isActive
                  ? "bg-blue-500/15 text-blue-300 border border-blue-500/30"
                  : "text-slate-400 hover:text-white hover:bg-slate-800/60"
              )
            }
          >
            <Icon className="h-4 w-4" />
            {label}
          </NavLink>
        ))}
      </nav>

      <div className="p-3 border-t border-slate-800 space-y-2">
        <div className="flex items-center gap-2 px-3 py-2">
          <div className="h-8 w-8 rounded-full bg-slate-800 flex items-center justify-center text-xs font-semibold text-slate-300">
            {user?.fullName?.split(" ").map(s => s[0]).slice(0, 2).join("") ?? "?"}
          </div>
          <div className="flex-1 min-w-0">
            <div className="text-xs font-medium text-white truncate">{user?.fullName ?? "Unknown"}</div>
            <div className="text-[10px] text-slate-500 truncate">{user?.role ?? ""}</div>
          </div>
        </div>
        <button
          onClick={() => { logout(); navigate("/login"); }}
          className="w-full flex items-center gap-2 rounded-md px-3 py-2 text-xs text-slate-400 hover:text-red-300 hover:bg-red-500/10 transition-colors"
        >
          <LogOut className="h-3.5 w-3.5" />
          Sign out
        </button>
      </div>
    </aside>
  );
}
