import { useParams, Link } from "react-router-dom";
import { useState } from "react";
import {
  ArrowLeft, FileText, Wrench, Shield,
  CheckCircle2, XCircle, Clock, Play, TestTube
} from "lucide-react";
import { Header } from "@/components/layout/Header";
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/Card";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import { RoleGuard } from "@/components/RoleGuard";
import { api, extractError } from "@/lib/api";
import { useFinding } from "@/lib/queries";
import { useAuth } from "@/contexts/AuthContext";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { formatDate } from "@/lib/utils";
import { toast } from "sonner";

type Tab = "overview" | "evidence" | "remediation";

export function FindingDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { user } = useAuth();
  const qc = useQueryClient();
  const [tab, setTab] = useState<Tab>("overview");
  const { data: finding, isLoading } = useFinding(id);

  const isViewer = user?.role === "VIEWER";
  const isDeveloper = user?.role === "DEVELOPER";
  const isManager = user?.role === "ADMIN" || user?.role === "SECURITY_MANAGER";
  const isPentester = user?.role === "PENTESTER";
  const isAnalyst = user?.role === "ANALYST";

  // VIEWER: no evidence, no remediation tabs
  // DEVELOPER: can see remediation (their queue), but no evidence? Actually developers need evidence to fix.
  const canSeeEvidence = !isViewer;
  const canSeeRemediation = !isViewer;

  const evidenceQ = useQuery({
    queryKey: ["evidence", id],
    enabled: !!id && tab === "evidence" && canSeeEvidence,
    queryFn: async () => (await api.get(`/findings/${id}/evidence`)).data as any[],
  });

  const remediationQ = useQuery({
    queryKey: ["remediation", id],
    enabled: !!id && tab === "remediation" && canSeeRemediation,
    queryFn: async () => {
      try { return (await api.get(`/findings/${id}/remediation`)).data; }
      catch { return null; }
    },
  });

  const retestsQ = useQuery({
    queryKey: ["retests", id],
    enabled: !!id && tab === "remediation" && canSeeRemediation,
    queryFn: async () => (await api.get(`/findings/${id}/retests`)).data as any[],
  });

  async function startRemediation() {
    if (!id || !user) return;
    try {
      await api.post(`/findings/${id}/remediation/start`, { developerId: user.id });
      toast.success("Remediation started");
      qc.invalidateQueries({ queryKey: ["remediation", id] });
      qc.invalidateQueries({ queryKey: ["finding", id] });
      setTab("remediation");
    } catch (e) { toast.error(extractError(e)); }
  }

  async function markFixed() {
    if (!id) return;
    try {
      await api.post(`/findings/${id}/remediation/ready`, {
        fixNotes: "Fixed via prepared statements.",
        commitRef: "auto-" + Date.now(),
      });
      toast.success("Marked as ready for retest");
      qc.invalidateQueries({ queryKey: ["remediation", id] });
    } catch (e) { toast.error(extractError(e)); }
  }

  async function retest(result: "PASS" | "FAIL") {
    if (!id) return;
    try {
      await api.post(`/findings/${id}/retest`, {
        result,
        notes: result === "PASS" ? "Retest passed" : "Issue still present",
      });
      toast.success(`Retest recorded: ${result}`);
      qc.invalidateQueries({ queryKey: ["remediation", id] });
      qc.invalidateQueries({ queryKey: ["retests", id] });
      qc.invalidateQueries({ queryKey: ["finding", id] });
    } catch (e) { toast.error(extractError(e)); }
  }

  if (isLoading) {
    return (
      <>
        <Header title="Finding" description="Loading…" />
        <div className="p-8 text-slate-500">Loading…</div>
      </>
    );
  }

  if (!finding) {
    return (
      <>
        <Header title="Finding not found" />
        <div className="p-8">
          <Link to="/findings" className="text-blue-400 hover:underline flex items-center gap-1">
            <ArrowLeft className="h-4 w-4" /> Back to findings
          </Link>
        </div>
      </>
    );
  }

  const tabs: { key: Tab; label: string; icon: any; show: boolean }[] = [
    { key: "overview",    label: "Overview",    icon: FileText, show: true },
    { key: "evidence",    label: "Evidence",    icon: Shield,   show: canSeeEvidence },
    { key: "remediation", label: "Remediation", icon: Wrench,   show: canSeeRemediation },
  ];

  return (
    <>
      <Header
        title={`${finding.reference} — ${finding.title}`}
        description={`Detected ${formatDate(finding.createdAt)}`}
        actions={
          <>
            <RoleGuard roles={["ADMIN", "SECURITY_MANAGER", "DEVELOPER"]}>
              {finding.status !== "VERIFIED" && finding.status !== "CLOSED" && (
                <Button size="sm" variant="outline" onClick={startRemediation}>
                  <Play className="h-3.5 w-3.5" /> Start Remediation
                </Button>
              )}
            </RoleGuard>
            <RoleGuard roles={["ADMIN", "SECURITY_MANAGER", "DEVELOPER"]}>
              {(finding.status === "IN_PROGRESS" || finding.status === "REOPENED") && (
                <Button size="sm" onClick={markFixed}>
                  <CheckCircle2 className="h-3.5 w-3.5" /> Mark Fixed
                </Button>
              )}
            </RoleGuard>
            <RoleGuard roles={["ADMIN", "SECURITY_MANAGER", "PENTESTER"]}>
              {finding.status === "READY_FOR_RETEST" && (
                <>
                  <Button size="sm" variant="outline" onClick={() => retest("FAIL")}>
                    <XCircle className="h-3.5 w-3.5" /> Retest FAIL
                  </Button>
                  <Button size="sm" onClick={() => retest("PASS")}>
                    <TestTube className="h-3.5 w-3.5" /> Retest PASS
                  </Button>
                </>
              )}
            </RoleGuard>
          </>
        }
      />
      <div className="flex-1 overflow-y-auto p-8 space-y-4">
        <Link to="/findings" className="text-xs text-blue-400 hover:underline inline-flex items-center gap-1">
          <ArrowLeft className="h-3 w-3" /> Back to findings
        </Link>

        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <Card className="p-5">
            <div className="text-xs text-slate-500 uppercase tracking-wider">Severity</div>
            <div className="mt-2">
              <Badge kind="severity" value={finding.severity} className="text-base py-1 px-3">
                {finding.severity}
              </Badge>
            </div>
          </Card>
          <Card className="p-5">
            <div className="text-xs text-slate-500 uppercase tracking-wider">Risk Score</div>
            <div className="mt-2 flex items-baseline gap-2">
              <span className="text-2xl font-bold text-white">{finding.riskScore?.toFixed(1) ?? "—"}</span>
              <Badge kind="risk" value={finding.riskBand ?? "MEDIUM"}>{finding.riskBand ?? "—"}</Badge>
            </div>
          </Card>
          <Card className="p-5">
            <div className="text-xs text-slate-500 uppercase tracking-wider">Status</div>
            <div className="mt-2">
              <Badge kind="status" value={finding.status}>{finding.status}</Badge>
            </div>
          </Card>
          <Card className="p-5">
            <div className="text-xs text-slate-500 uppercase tracking-wider">Source</div>
            <div className="mt-2 text-white">{finding.source ?? "—"}</div>
          </Card>
        </div>

        <div className="border-b border-slate-800 flex gap-1">
          {tabs.filter(t => t.show).map(({ key, label, icon: Icon }) => (
            <button key={key} onClick={() => setTab(key)}
              className={`flex items-center gap-2 px-4 py-2 text-sm font-medium border-b-2 transition-colors ${
                tab === key ? "border-blue-500 text-blue-400"
                            : "border-transparent text-slate-400 hover:text-white"
              }`}>
              <Icon className="h-4 w-4" />
              {label}
            </button>
          ))}
        </div>

        {tab === "overview" && (
          <Card>
            <CardHeader><CardTitle>Details</CardTitle></CardHeader>
            <CardContent>
              <dl className="grid grid-cols-1 md:grid-cols-2 gap-5 text-sm">
                <div className="md:col-span-2">
                  <dt className="text-xs text-slate-500 uppercase tracking-wider">Description</dt>
                  <dd className="text-slate-200 mt-1">{finding.description ?? "—"}</dd>
                </div>
                <div>
                  <dt className="text-xs text-slate-500 uppercase tracking-wider">Endpoint</dt>
                  <dd className="text-slate-200 font-mono mt-1">{finding.endpoint ?? "—"}</dd>
                </div>
                <div>
                  <dt className="text-xs text-slate-500 uppercase tracking-wider">Parameter</dt>
                  <dd className="text-slate-200 font-mono mt-1">{finding.parameter ?? "—"}</dd>
                </div>
                <div>
                  <dt className="text-xs text-slate-500 uppercase tracking-wider">Confidence</dt>
                  <dd className="text-slate-200 mt-1">{finding.confidence?.toFixed(3) ?? "—"}</dd>
                </div>
                <div>
                  <dt className="text-xs text-slate-500 uppercase tracking-wider">Finding ID</dt>
                  <dd className="text-slate-200 font-mono text-xs mt-1">{finding.id}</dd>
                </div>
              </dl>
            </CardContent>
          </Card>
        )}

        {tab === "evidence" && canSeeEvidence && (
          <Card>
            <CardHeader><CardTitle>Evidence Artifacts</CardTitle></CardHeader>
            <CardContent>
              {evidenceQ.isLoading ? (
                <div className="text-sm text-slate-500 py-8 text-center">Loading…</div>
              ) : !evidenceQ.data || evidenceQ.data.length === 0 ? (
                <EmptyState icon={Shield} title="No evidence attached"
                  description="Upload HTTP requests, responses, or screenshots to cryptographically prove this finding." />
              ) : (
                <div className="space-y-3">
                  {evidenceQ.data.map((e: any) => (
                    <div key={e.id} className="rounded-md border border-slate-800 bg-slate-800/40 p-4">
                      <div className="flex items-center justify-between mb-2">
                        <div className="flex items-center gap-2">
                          <FileText className="h-4 w-4 text-blue-400" />
                          <span className="text-sm font-medium text-white">{e.filename}</span>
                        </div>
                        <span className="text-xs text-slate-500">{formatDate(e.createdAt)}</span>
                      </div>
                      <div className="text-xs text-slate-500">
                        <span className="font-medium text-slate-400">Size:</span> {e.sizeBytes} bytes
                      </div>
                      <div className="text-xs mt-1 break-all">
                        <span className="font-medium text-slate-400">SHA-256:</span>{" "}
                        <span className="font-mono text-emerald-400">{e.sha256}</span>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>
        )}

        {tab === "remediation" && canSeeRemediation && (
          <div className="space-y-4">
            <Card>
              <CardHeader><CardTitle>Remediation Status</CardTitle></CardHeader>
              <CardContent>
                {remediationQ.isLoading ? (
                  <div className="text-sm text-slate-500 py-4">Loading…</div>
                ) : !remediationQ.data ? (
                  <EmptyState icon={Wrench} title="No remediation started"
                    description="Once a developer starts work on this finding, the remediation state will appear here." />
                ) : (
                  <div className="space-y-4 text-sm">
                    <div className="flex items-center gap-2">
                      <Badge kind="remediation" value={remediationQ.data.status}>
                        {remediationQ.data.status}
                      </Badge>
                      {remediationQ.data.reopenedCount > 0 && (
                        <span className="text-xs text-red-400">
                          Reopened {remediationQ.data.reopenedCount}×
                        </span>
                      )}
                    </div>
                    {remediationQ.data.fixNotes && (
                      <div>
                        <div className="text-xs text-slate-500 uppercase tracking-wider mb-1">Fix Notes</div>
                        <div className="text-slate-200">{remediationQ.data.fixNotes}</div>
                      </div>
                    )}
                    {remediationQ.data.commitRef && (
                      <div>
                        <div className="text-xs text-slate-500 uppercase tracking-wider mb-1">Commit</div>
                        <div className="font-mono text-xs text-blue-300">{remediationQ.data.commitRef}</div>
                      </div>
                    )}
                  </div>
                )}
              </CardContent>
            </Card>

            {retestsQ.data && retestsQ.data.length > 0 && (
              <Card>
                <CardHeader><CardTitle>Retest History</CardTitle></CardHeader>
                <CardContent>
                  <div className="space-y-3">
                    {retestsQ.data.map((r: any) => (
                      <div key={r.id} className="flex items-start gap-3 rounded-md border border-slate-800 bg-slate-800/40 p-4">
                        {r.result === "PASS" ? (
                          <CheckCircle2 className="h-5 w-5 text-emerald-400 shrink-0 mt-0.5" />
                        ) : r.result === "FAIL" ? (
                          <XCircle className="h-5 w-5 text-red-400 shrink-0 mt-0.5" />
                        ) : (
                          <Clock className="h-5 w-5 text-amber-400 shrink-0 mt-0.5" />
                        )}
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center gap-2 mb-1">
                            <Badge kind="severity" value={
                              r.result === "PASS" ? "LOW" : r.result === "FAIL" ? "HIGH" : "MEDIUM"
                            }>{r.result}</Badge>
                            <span className="text-xs text-slate-500">{formatDate(r.createdAt)}</span>
                          </div>
                          {r.notes && <div className="text-sm text-slate-300">{r.notes}</div>}
                        </div>
                      </div>
                    ))}
                  </div>
                </CardContent>
              </Card>
            )}
          </div>
        )}
      </div>
    </>
  );
}
