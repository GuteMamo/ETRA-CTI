import { useState } from "react";
import { Link } from "react-router-dom";
import { Search, ExternalLink, User, Activity, Plus, Loader2, X } from "lucide-react";
import { Header } from "@/components/layout/Header";
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/Card";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { EmptyState } from "@/components/ui/EmptyState";
import { useInvestigations } from "@/lib/queries";
import { api, extractError } from "@/lib/api";
import { formatRelative } from "@/lib/utils";
import { useAuth } from "@/contexts/AuthContext";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import type { InvestigationPriority, ApplicationDto } from "@/lib/types";

const PRIORITIES: InvestigationPriority[] = ["LOW", "MEDIUM", "HIGH", "CRITICAL"];

export function InvestigationsPage() {
  const qc = useQueryClient();
  const { user } = useAuth();
  const { data: investigations = [], isLoading } = useInvestigations();

  const [showCreateModal, setShowCreateModal] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [newInv, setNewInv] = useState({
    applicationId: "",
    title: "",
    description: "",
    priority: "HIGH" as InvestigationPriority,
  });

  const appsQ = useQuery({
    queryKey: ["applications-all"],
    queryFn: async () => (await api.get<ApplicationDto[]>("/applications")).data,
  });

  const canCreate = user && ["ADMIN", "SECURITY_MANAGER", "ANALYST"].includes(user.role);

  async function handleCreateInvestigation() {
    if (!newInv.applicationId) {
      toast.error("Please select an application.");
      return;
    }
    if (!newInv.title.trim()) {
      toast.error("Investigation title is required.");
      return;
    }
    setSubmitting(true);
    try {
      await api.post("/investigations", {
        applicationId: newInv.applicationId,
        title: newInv.title.trim(),
        description: newInv.description.trim() || undefined,
        priority: newInv.priority,
      });
      toast.success("Security investigation initiated");
      qc.invalidateQueries({ queryKey: ["investigations"] });
      setShowCreateModal(false);
      setNewInv({ applicationId: "", title: "", description: "", priority: "HIGH" });
    } catch (e) {
      toast.error(extractError(e));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <>
      <Header
        title="Investigations"
        description="Analyst workspace tracking threat investigations and case evidence"
        actions={
          canCreate ? (
            <Button size="sm" onClick={() => setShowCreateModal(true)}>
              <Plus className="h-4 w-4 mr-1" /> New Investigation
            </Button>
          ) : undefined
        }
      />
      <div className="flex-1 overflow-y-auto p-8 space-y-4">

        {isLoading ? (
          <Card className="p-12 text-center text-sm text-slate-500">Loading investigations…</Card>
        ) : investigations.length === 0 ? (
          <Card>
            <EmptyState
              icon={Search}
              title="No investigations yet"
              description="Start a new investigation or open one from an attack campaign or alert."
            />
          </Card>
        ) : (
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
            {investigations.map((inv) => (
              <Card key={inv.id} className="p-5 hover:border-slate-700 transition-colors">
                <div className="flex items-start justify-between mb-3">
                  <div className="flex items-center gap-3">
                    <div className="h-10 w-10 rounded-lg bg-blue-500/15 border border-blue-500/30 flex items-center justify-center">
                      <Search className="h-5 w-5 text-blue-400" />
                    </div>
                    <div>
                      <Link to={`/investigations/${inv.id}`}
                        className="text-sm font-semibold text-white hover:underline flex items-center gap-2">
                        {inv.reference}
                        <ExternalLink className="h-3 w-3 text-slate-500" />
                      </Link>
                      <div className="text-xs text-slate-400 mt-0.5 truncate max-w-xs">
                        {inv.title}
                      </div>
                    </div>
                  </div>
                  <Badge kind="status" value={inv.status}>{inv.status}</Badge>
                </div>

                <div className="grid grid-cols-2 gap-2 text-xs mt-3">
                  <div className="rounded bg-slate-800/50 p-2 flex items-center gap-2">
                    <Activity className="h-3 w-3 text-slate-500" />
                    <div>
                      <div className="text-slate-500">Priority</div>
                      <div className="text-white mt-0.5 font-medium">{inv.priority}</div>
                    </div>
                  </div>
                  <div className="rounded bg-slate-800/50 p-2 flex items-center gap-2">
                    <User className="h-3 w-3 text-slate-500" />
                    <div>
                      <div className="text-slate-500">Assigned</div>
                      <div className="text-white mt-0.5 font-mono text-xs">
                        {inv.assignedTo ? inv.assignedTo.slice(0, 8) + "…" : "unassigned"}
                      </div>
                    </div>
                  </div>
                </div>

                <div className="mt-3 text-xs text-slate-500">
                  Opened {formatRelative(inv.createdAt)}
                </div>
              </Card>
            ))}
          </div>
        )}
      </div>

      {/* New Investigation Modal */}
      {showCreateModal && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
          <Card className="w-full max-w-lg border-slate-700 bg-slate-900 shadow-xl">
            <CardHeader className="flex flex-row items-center justify-between pb-3 border-b border-slate-800">
              <CardTitle>Initiate Security Investigation</CardTitle>
              <Button size="sm" variant="ghost" onClick={() => setShowCreateModal(false)}>
                <X className="h-4 w-4" />
              </Button>
            </CardHeader>
            <CardContent className="p-4 space-y-4">
              <div>
                <label className="text-xs text-slate-400 font-medium mb-1 block">
                  Application <span className="text-rose-400">*</span>
                </label>
                <select
                  value={newInv.applicationId}
                  onChange={(e) => setNewInv({ ...newInv, applicationId: e.target.value })}
                  className="w-full h-10 rounded-md border border-slate-700 bg-slate-950 px-3 text-sm text-slate-100"
                >
                  <option value="">-- Select Application --</option>
                  {appsQ.data?.map(app => (
                    <option key={app.id} value={app.id}>{app.name} ({app.environment})</option>
                  ))}
                </select>
              </div>

              <div>
                <label className="text-xs text-slate-400 font-medium mb-1 block">
                  Investigation Title <span className="text-rose-400">*</span>
                </label>
                <Input
                  placeholder="e.g. Investigation into Credential Stuffing Surge"
                  value={newInv.title}
                  onChange={(e) => setNewInv({ ...newInv, title: e.target.value })}
                />
              </div>

              <div>
                <label className="text-xs text-slate-400 font-medium mb-1 block">Priority</label>
                <select
                  value={newInv.priority}
                  onChange={(e) => setNewInv({ ...newInv, priority: e.target.value as InvestigationPriority })}
                  className="w-full h-10 rounded-md border border-slate-700 bg-slate-950 px-3 text-sm text-slate-100"
                >
                  {PRIORITIES.map(p => <option key={p} value={p}>{p}</option>)}
                </select>
              </div>

              <div>
                <label className="text-xs text-slate-400 font-medium mb-1 block">Initial Scope & Hypothesis</label>
                <textarea
                  rows={4}
                  placeholder="Describe observed anomalous indicators, affected systems, and triage questions..."
                  value={newInv.description}
                  onChange={(e) => setNewInv({ ...newInv, description: e.target.value })}
                  className="w-full rounded-md border border-slate-700 bg-slate-950 p-3 text-sm text-slate-100 placeholder:text-slate-600 focus:outline-none focus:ring-1 focus:ring-blue-500"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2 border-t border-slate-800">
                <Button variant="ghost" onClick={() => setShowCreateModal(false)} disabled={submitting}>
                  Cancel
                </Button>
                <Button onClick={handleCreateInvestigation} disabled={submitting}>
                  {submitting && <Loader2 className="h-4 w-4 animate-spin mr-1" />}
                  Open Case
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>
      )}
    </>
  );
}
