import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { AlertTriangle, Search, Filter, Plus, Loader2, X, CheckSquare, Clock } from "lucide-react";
import { Header } from "@/components/layout/Header";
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/Card";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { EmptyState } from "@/components/ui/EmptyState";
import { useFindings } from "@/lib/queries";
import { api, extractError } from "@/lib/api";
import { formatDate } from "@/lib/utils";
import { useAuth } from "@/contexts/AuthContext";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import type { Severity, FindingStatus, ApplicationDto } from "@/lib/types";

const SEVERITIES: (Severity | "ALL")[] = ["ALL", "CRITICAL", "HIGH", "MEDIUM", "LOW"];
const STATUSES: (FindingStatus | "ALL")[] = [
  "ALL", "OPEN", "CONFIRMED", "ASSIGNED", "IN_PROGRESS",
  "READY_FOR_RETEST", "VERIFIED", "CLOSED", "REOPENED", "FALSE_POSITIVE"
];

export function FindingsPage() {
  const qc = useQueryClient();
  const { user } = useAuth();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [severity, setSeverity] = useState<Severity | "ALL">("ALL");
  const [status, setStatus] = useState<FindingStatus | "ALL">("ALL");
  const [assignedToMeOnly, setAssignedToMeOnly] = useState(false);
  const [overdueOnly, setOverdueOnly] = useState(false);

  // New Finding Modal State
  const [showSubmitModal, setShowSubmitModal] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [newFinding, setNewFinding] = useState({
    applicationId: "",
    title: "",
    description: "",
    endpoint: "",
    parameter: "",
    severity: "HIGH" as Severity,
    confidence: "0.90",
    source: "MANUAL",
  });

  const { data, isLoading } = useFindings(page, 50);
  const all = data?.content ?? [];

  const appsQ = useQuery({
    queryKey: ["applications-all"],
    queryFn: async () => (await api.get<ApplicationDto[]>("/applications")).data,
  });

  const canSubmit = user && ["ADMIN", "SECURITY_MANAGER", "PENTESTER", "ANALYST"].includes(user.role);

  async function handleCreateFinding() {
    if (!newFinding.applicationId) {
      toast.error("Please select an application.");
      return;
    }
    if (!newFinding.title.trim()) {
      toast.error("Finding title is required.");
      return;
    }
    setSubmitting(true);
    try {
      await api.post("/findings", {
        applicationId: newFinding.applicationId,
        title: newFinding.title.trim(),
        description: newFinding.description.trim() || undefined,
        endpoint: newFinding.endpoint.trim() || undefined,
        parameter: newFinding.parameter.trim() || undefined,
        severity: newFinding.severity,
        confidence: newFinding.confidence ? parseFloat(newFinding.confidence) : undefined,
        source: newFinding.source || "MANUAL",
      });
      toast.success("Security finding submitted successfully");
      qc.invalidateQueries({ queryKey: ["findings"] });
      setShowSubmitModal(false);
      setNewFinding({
        applicationId: "",
        title: "",
        description: "",
        endpoint: "",
        parameter: "",
        severity: "HIGH",
        confidence: "0.90",
        source: "MANUAL",
      });
    } catch (e) {
      toast.error(extractError(e));
    } finally {
      setSubmitting(false);
    }
  }

  const filtered = useMemo(() => {
    return all.filter(f => {
      if (severity !== "ALL" && f.severity !== severity) return false;
      if (status !== "ALL" && f.status !== status) return false;
      if (assignedToMeOnly && user && f.assignedTo !== user.id) return false;
      if (overdueOnly) {
        if (!f.dueDate) return false;
        const isPast = new Date(f.dueDate) < new Date();
        const isOpen = f.status !== "VERIFIED" && f.status !== "CLOSED";
        if (!isPast || !isOpen) return false;
      }
      if (search) {
        const s = search.toLowerCase();
        return (
          f.title.toLowerCase().includes(s) ||
          f.reference.toLowerCase().includes(s) ||
          (f.endpoint ?? "").toLowerCase().includes(s)
        );
      }
      return true;
    });
  }, [all, severity, status, assignedToMeOnly, overdueOnly, user, search]);

  return (
    <>
      <Header
        title="Findings"
        description="All security findings across applications, sorted by risk score"
        actions={
          canSubmit ? (
            <Button size="sm" onClick={() => setShowSubmitModal(true)}>
              <Plus className="h-4 w-4 mr-1" /> Submit Finding
            </Button>
          ) : undefined
        }
      />
      <div className="flex-1 overflow-y-auto p-8 space-y-4">

        {/* Filters */}
        <Card className="p-4">
          <div className="flex flex-wrap items-center gap-3">
            <div className="relative flex-1 min-w-[240px]">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-500" />
              <Input
                placeholder="Search by reference, title, endpoint…"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="pl-9"
              />
            </div>
            <div className="flex items-center gap-2 flex-wrap">
              <Filter className="h-4 w-4 text-slate-500" />
              <select
                value={severity}
                onChange={(e) => setSeverity(e.target.value as Severity | "ALL")}
                className="h-10 rounded-md border border-slate-700 bg-slate-900 px-3 text-sm text-slate-100 focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                {SEVERITIES.map(s => <option key={s} value={s}>Severity: {s}</option>)}
              </select>
              <select
                value={status}
                onChange={(e) => setStatus(e.target.value as FindingStatus | "ALL")}
                className="h-10 rounded-md border border-slate-700 bg-slate-900 px-3 text-sm text-slate-100 focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                {STATUSES.map(s => <option key={s} value={s}>Status: {s}</option>)}
              </select>

              <label className="flex items-center gap-1.5 text-xs text-slate-300 ml-2 cursor-pointer select-none">
                <input
                  type="checkbox"
                  checked={assignedToMeOnly}
                  onChange={(e) => setAssignedToMeOnly(e.target.checked)}
                  className="rounded border-slate-700 bg-slate-900 text-blue-500 focus:ring-0"
                />
                Assigned to Me
              </label>

              <label className="flex items-center gap-1.5 text-xs text-rose-300 ml-2 cursor-pointer select-none">
                <input
                  type="checkbox"
                  checked={overdueOnly}
                  onChange={(e) => setOverdueOnly(e.target.checked)}
                  className="rounded border-slate-700 bg-slate-900 text-rose-500 focus:ring-0"
                />
                Overdue Only
              </label>
            </div>
            <div className="text-xs text-slate-500 ml-auto">
              {filtered.length} of {all.length} shown
            </div>
          </div>
        </Card>

        {/* Table */}
        <Card className="overflow-hidden">
          {isLoading ? (
            <div className="p-12 text-center text-sm text-slate-500">Loading findings…</div>
          ) : filtered.length === 0 ? (
            <EmptyState icon={AlertTriangle} title="No findings match your filters"
              description="Adjust the filters above or submit a new security finding." />
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="bg-slate-900/80 text-slate-400 text-xs uppercase tracking-wider">
                  <tr>
                    <th className="text-left px-4 py-3 font-medium">Reference</th>
                    <th className="text-left px-4 py-3 font-medium">Title</th>
                    <th className="text-left px-4 py-3 font-medium">Endpoint</th>
                    <th className="text-left px-4 py-3 font-medium">Severity</th>
                    <th className="text-left px-4 py-3 font-medium">Risk</th>
                    <th className="text-left px-4 py-3 font-medium">Status</th>
                    <th className="text-left px-4 py-3 font-medium">SLA Due</th>
                    <th className="text-left px-4 py-3 font-medium">Source</th>
                    <th className="text-left px-4 py-3 font-medium">Created</th>
                  </tr>
                </thead>
                <tbody>
                  {filtered.map(f => {
                    const isOverdue = f.dueDate && new Date(f.dueDate) < new Date() &&
                      f.status !== "VERIFIED" && f.status !== "CLOSED";
                    return (
                      <tr key={f.id} className="border-t border-slate-800 hover:bg-slate-800/40 transition-colors">
                        <td className="px-4 py-3 font-mono text-xs text-blue-300">
                          <Link to={`/findings/${f.id}`} className="hover:underline">{f.reference}</Link>
                        </td>
                        <td className="px-4 py-3 text-slate-100 max-w-md truncate font-medium">{f.title}</td>
                        <td className="px-4 py-3 text-slate-400 font-mono text-xs max-w-[200px] truncate">
                          {f.endpoint ?? "—"}{f.parameter ? `?${f.parameter}` : ""}
                        </td>
                        <td className="px-4 py-3">
                          <Badge kind="severity" value={f.severity}>{f.severity}</Badge>
                        </td>
                        <td className="px-4 py-3">
                          <span className="text-slate-300 font-medium">
                            {f.riskScore?.toFixed(1) ?? "—"}
                          </span>
                          <span className="text-xs text-slate-500 ml-1.5">({f.riskBand ?? "—"})</span>
                        </td>
                        <td className="px-4 py-3">
                          <Badge kind="status" value={f.status}>{f.status}</Badge>
                        </td>
                        <td className="px-4 py-3 text-xs whitespace-nowrap">
                          {f.dueDate ? (
                            <span className={isOverdue ? "text-rose-400 font-semibold" : "text-slate-400"}>
                              {formatDate(f.dueDate)}
                              {isOverdue && <span className="ml-1 text-[10px] bg-rose-500/20 px-1 py-0.5 rounded">OVERDUE</span>}
                            </span>
                          ) : (
                            <span className="text-slate-600">—</span>
                          )}
                        </td>
                        <td className="px-4 py-3 text-xs text-slate-500">{f.source ?? "—"}</td>
                        <td className="px-4 py-3 text-xs text-slate-500 whitespace-nowrap">
                          {formatDate(f.createdAt)}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </Card>

        {/* Pagination */}
        {data && data.totalPages > 1 && (
          <div className="flex items-center justify-between text-sm text-slate-400">
            <div>Page {data.number + 1} of {data.totalPages}</div>
            <div className="flex gap-2">
              <button
                disabled={data.first}
                onClick={() => setPage(p => Math.max(0, p - 1))}
                className="px-3 py-1.5 rounded-md border border-slate-700 hover:bg-slate-800 disabled:opacity-40 disabled:pointer-events-none"
              >
                Previous
              </button>
              <button
                disabled={data.last}
                onClick={() => setPage(p => p + 1)}
                className="px-3 py-1.5 rounded-md border border-slate-700 hover:bg-slate-800 disabled:opacity-40 disabled:pointer-events-none"
              >
                Next
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Submit Finding Modal */}
      {showSubmitModal && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
          <Card className="w-full max-w-xl border-slate-700 bg-slate-900 shadow-xl max-h-[90vh] flex flex-col">
            <CardHeader className="flex flex-row items-center justify-between pb-3 border-b border-slate-800">
              <CardTitle>Submit Security Finding</CardTitle>
              <Button size="sm" variant="ghost" onClick={() => setShowSubmitModal(false)}>
                <X className="h-4 w-4" />
              </Button>
            </CardHeader>
            <CardContent className="p-4 overflow-y-auto space-y-4 flex-1">
              <div>
                <label className="text-xs text-slate-400 font-medium mb-1 block">
                  Target Application <span className="text-rose-400">*</span>
                </label>
                <select
                  value={newFinding.applicationId}
                  onChange={(e) => setNewFinding({ ...newFinding, applicationId: e.target.value })}
                  className="w-full h-10 rounded-md border border-slate-700 bg-slate-950 px-3 text-sm text-slate-100"
                >
                  <option value="">-- Select Application --</option>
                  {appsQ.data?.map(app => (
                    <option key={app.id} value={app.id}>{app.name} ({app.type} - {app.environment})</option>
                  ))}
                </select>
              </div>

              <div>
                <label className="text-xs text-slate-400 font-medium mb-1 block">
                  Finding Title <span className="text-rose-400">*</span>
                </label>
                <Input
                  placeholder="e.g. SQL Injection in Search Endpoint"
                  value={newFinding.title}
                  onChange={(e) => setNewFinding({ ...newFinding, title: e.target.value })}
                />
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                <div>
                  <label className="text-xs text-slate-400 font-medium mb-1 block">Severity</label>
                  <select
                    value={newFinding.severity}
                    onChange={(e) => setNewFinding({ ...newFinding, severity: e.target.value as Severity })}
                    className="w-full h-10 rounded-md border border-slate-700 bg-slate-950 px-3 text-sm text-slate-100"
                  >
                    {SEVERITIES.filter(s => s !== "ALL").map(s => <option key={s} value={s}>{s}</option>)}
                  </select>
                </div>
                <div>
                  <label className="text-xs text-slate-400 font-medium mb-1 block">Confidence (0.0 to 1.0)</label>
                  <Input
                    type="number"
                    step="0.05"
                    min="0"
                    max="1"
                    value={newFinding.confidence}
                    onChange={(e) => setNewFinding({ ...newFinding, confidence: e.target.value })}
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                <div>
                  <label className="text-xs text-slate-400 font-medium mb-1 block">Endpoint URL / Path</label>
                  <Input
                    placeholder="/api/v1/users"
                    value={newFinding.endpoint}
                    onChange={(e) => setNewFinding({ ...newFinding, endpoint: e.target.value })}
                  />
                </div>
                <div>
                  <label className="text-xs text-slate-400 font-medium mb-1 block">Vulnerable Parameter</label>
                  <Input
                    placeholder="id or username"
                    value={newFinding.parameter}
                    onChange={(e) => setNewFinding({ ...newFinding, parameter: e.target.value })}
                  />
                </div>
              </div>

              <div>
                <label className="text-xs text-slate-400 font-medium mb-1 block">Technical Description & Proof</label>
                <textarea
                  rows={4}
                  placeholder="Describe the vulnerability, impact, and reproduction steps..."
                  value={newFinding.description}
                  onChange={(e) => setNewFinding({ ...newFinding, description: e.target.value })}
                  className="w-full rounded-md border border-slate-700 bg-slate-950 p-3 text-sm text-slate-100 placeholder:text-slate-600 focus:outline-none focus:ring-1 focus:ring-blue-500"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2 border-t border-slate-800">
                <Button variant="ghost" onClick={() => setShowSubmitModal(false)} disabled={submitting}>
                  Cancel
                </Button>
                <Button onClick={handleCreateFinding} disabled={submitting}>
                  {submitting && <Loader2 className="h-4 w-4 animate-spin mr-1" />}
                  Submit Finding
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>
      )}
    </>
  );
}
