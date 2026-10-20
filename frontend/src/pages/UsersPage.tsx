import { useState } from "react";
import { Users as UsersIcon, Plus, Loader2 } from "lucide-react";
import { Header } from "@/components/layout/Header";
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/Card";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { EmptyState } from "@/components/ui/EmptyState";
import { api, extractError } from "@/lib/api";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import type { UserDto, UserRole } from "@/lib/types";

const ROLES: UserRole[] = ["ADMIN", "SECURITY_MANAGER", "PENTESTER", "ANALYST", "DEVELOPER", "VIEWER"];

export function UsersPage() {
  const qc = useQueryClient();
  const [showForm, setShowForm] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [form, setForm] = useState({
    email: "",
    password: "",
    fullName: "",
    role: "VIEWER" as UserRole,
  });

  const usersQ = useQuery({
    queryKey: ["users"],
    queryFn: async () => (await api.get<UserDto[]>("/users")).data,
  });

  async function create() {
    if (!form.email || !form.password || !form.fullName) return;
    setSubmitting(true);
    try {
      await api.post("/users", form);
      toast.success("User created");
      setForm({ email: "", password: "", fullName: "", role: "VIEWER" });
      setShowForm(false);
      qc.invalidateQueries({ queryKey: ["users"] });
    } catch (e) {
      toast.error(extractError(e));
    } finally {
      setSubmitting(false);
    }
  }

  async function updateRole(id: string, role: UserRole) {
    try {
      await api.patch(`/users/${id}/role`, { role });
      toast.success("Role updated");
      qc.invalidateQueries({ queryKey: ["users"] });
    } catch (e) {
      toast.error(extractError(e));
    }
  }

  async function toggleStatus(u: UserDto) {
    const newStatus = u.enabled === false;
    try {
      await api.patch(`/users/${u.id}/status`, { enabled: newStatus });
      toast.success(`User ${newStatus ? "enabled" : "disabled"}`);
      qc.invalidateQueries({ queryKey: ["users"] });
    } catch (e) {
      toast.error(extractError(e));
    }
  }

  return (
    <>
      <Header
        title="Users & Roles"
        description="Manage team members, access roles, and account statuses"
        actions={
          <Button size="sm" onClick={() => setShowForm((v) => !v)}>
            <Plus className="h-4 w-4" /> Add User
          </Button>
        }
      />
      <div className="flex-1 overflow-y-auto p-8 space-y-4">

        {showForm && (
          <Card className="border-blue-500/40 bg-blue-500/5">
            <CardHeader><CardTitle>Create User</CardTitle></CardHeader>
            <CardContent>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                <div>
                  <label className="text-xs text-slate-400 mb-1 block">Full Name</label>
                  <Input value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} />
                </div>
                <div>
                  <label className="text-xs text-slate-400 mb-1 block">Email</label>
                  <Input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
                </div>
                <div>
                  <label className="text-xs text-slate-400 mb-1 block">Password</label>
                  <Input type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} />
                </div>
                <div>
                  <label className="text-xs text-slate-400 mb-1 block">Role</label>
                  <select value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value as UserRole })}
                    className="h-10 w-full rounded-md border border-slate-700 bg-slate-900 px-3 text-sm text-slate-100">
                    {ROLES.map(r => <option key={r} value={r}>{r}</option>)}
                  </select>
                </div>
              </div>
              <div className="flex justify-end gap-2 mt-4">
                <Button variant="ghost" onClick={() => setShowForm(false)}>Cancel</Button>
                <Button onClick={create} disabled={submitting}>
                  {submitting && <Loader2 className="h-4 w-4 animate-spin" />}
                  Create
                </Button>
              </div>
            </CardContent>
          </Card>
        )}

        <Card className="overflow-hidden">
          {usersQ.isLoading ? (
            <div className="p-12 text-center text-sm text-slate-500">Loading…</div>
          ) : !usersQ.data || usersQ.data.length === 0 ? (
            <EmptyState icon={UsersIcon} title="No users" />
          ) : (
            <table className="w-full text-sm">
              <thead className="bg-slate-900/80 text-slate-400 text-xs uppercase tracking-wider">
                <tr>
                  <th className="text-left px-4 py-3">Name</th>
                  <th className="text-left px-4 py-3">Email</th>
                  <th className="text-left px-4 py-3">Role</th>
                  <th className="text-left px-4 py-3">Status</th>
                  <th className="text-right px-4 py-3">Actions</th>
                </tr>
              </thead>
              <tbody>
                {usersQ.data.map((u) => {
                  const isEnabled = u.enabled !== false;
                  return (
                    <tr key={u.id} className="border-t border-slate-800 hover:bg-slate-800/40">
                      <td className="px-4 py-3 text-white font-medium">{u.fullName}</td>
                      <td className="px-4 py-3 text-slate-400 font-mono text-xs">{u.email}</td>
                      <td className="px-4 py-3">
                        <Badge kind="neutral">{u.role}</Badge>
                      </td>
                      <td className="px-4 py-3">
                        <Badge kind={isEnabled ? "info" : "danger"}>
                          {isEnabled ? "ACTIVE" : "DISABLED"}
                        </Badge>
                      </td>
                      <td className="px-4 py-3 text-right">
                        <div className="flex items-center justify-end gap-2">
                          <select
                            value={u.role}
                            onChange={(e) => updateRole(u.id, e.target.value as UserRole)}
                            className="h-8 rounded-md border border-slate-700 bg-slate-900 px-2 text-xs text-slate-100"
                          >
                            {ROLES.map(r => <option key={r} value={r}>{r}</option>)}
                          </select>
                          <Button
                            size="sm"
                            variant={isEnabled ? "ghost" : "primary"}
                            className={isEnabled ? "text-rose-400 hover:text-rose-300 hover:bg-rose-500/10 text-xs h-8" : "text-xs h-8"}
                            onClick={() => toggleStatus(u)}
                          >
                            {isEnabled ? "Disable" : "Enable"}
                          </Button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}
        </Card>
      </div>
    </>
  );
}
