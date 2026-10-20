import { useParams, Link } from "react-router-dom";
import { useState, useRef } from "react";
import {
  ArrowLeft, FileText, Wrench, Shield, ShieldCheck, Upload,
  CheckCircle2, XCircle, Clock, Play, TestTube, UserCheck, Calendar,
  AlertTriangle, Loader2, X, SlidersHorizontal
} from "lucide-react";
import { Header } from "@/components/layout/Header";
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/Card";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { EmptyState } from "@/components/ui/EmptyState";
import { RoleGuard } from "@/components/RoleGuard";
import { api, extractError } from "@/lib/api";
import { useFinding } from "@/lib/queries";
import { useAuth } from "@/contexts/AuthContext";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { formatDate } from "@/lib/utils";
import { toast } from "sonner";
import type { Severity, UserDto } from "@/lib/types";

type Tab = "overview" | "evidence" | "remediation" | "governance";

const SEVERITIES: Severity[] = ["CRITICAL", "HIGH", "MEDIUM", "LOW"];

export function FindingDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { user } = useAuth();
  const qc = useQueryClient();
  const [tab, setTab] = useState<Tab>("overview");
  const { data: finding, isLoading } = useFinding(id);

  const isViewer = user?.role === "VIEWER";
  const isManager = user?.role === "ADMIN" || user?.role === "SECURITY_MANAGER";

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

  // Query users for assignment
  const usersQ = useQuery({
    queryKey: ["assignable-users"],
    enabled: isManager,
    queryFn: async () => (await api.get<UserDto[]>("/users")).data,
  });

  // Evidence state
  const [uploadingEvidence, setUploadingEvidence] = useState(false);
  const [verifyingId, setVerifyingId] = useState<string | null>(null);
  const [verifyResults, setVerifyResults] = useState<Record<string, { integrity: string; currentSha256?: string }>>({});
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  // Modals state
  const [showMarkFixedModal, setShowMarkFixedModal] = useState(false);
  const [fixCommitRef, setFixCommitRef] = useState("");
  const [fixNotes, setFixNotes] = useState("");
  const [submittingFix, setSubmittingFix] = useState(false);

  const [showRetestModal, setShowRetestModal] = useState(false);
  const [retestResult, setRetestResult] = useState<"PASS" | "FAIL">("PASS");
  const [retestNotes, setRetestNotes] = useState("");
  const [submittingRetest, setSubmittingRetest] = useState(false);

  const [showOverrideModal, setShowOverrideModal] = useState(false);
  const [overrideSeverity, setOverrideSeverity] = useState<Severity>("HIGH");
  const [overrideReason, setOverrideReason] = useState("");
  const [submittingOverride, setSubmittingOverride] = useState(false);

  // Governance inline state
  const [selectedAssignee, setSelectedAssignee] = useState<string>("");
  const [selectedDueDate, setSelectedDueDate] = useState<string>("");

  async function handleEvidenceUpload(file: File) {
    if (!id) return;
    setUploadingEvidence(true);
    try {
      const form = new FormData();
      form.append("file", file);
      await api.post(`/findings/${id}/evidence`, form);
      toast.success("Evidence uploaded & SHA-256 digest recorded");
      qc.invalidateQueries({ queryKey: ["evidence", id] });
    } catch (e) {
      toast.error(extractError(e));
    } finally {
      setUploadingEvidence(false);
    }
  }

  async function handleVerifyEvidence(evidenceId: string) {
    setVerifyingId(evidenceId);
    try {
      const res = await api.post(`/evidence/${evidenceId}/verify`);
      const data = res.data as { integrity: string; currentSha256?: string; storedSha256: string };
      setVerifyResults(prev => ({ ...prev, [evidenceId]: data }));
      if (data.integrity === "INTACT") {
        toast.success("Integrity Verified: INTACT (hashes match)");
      } else {
        toast.error(`Integrity Warning: ${data.integrity}`);
      }
    } catch (e) {
      toast.error(extractError(e));
    } finally {
      setVerifyingId(null);
    }
  }

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

  async function handleMarkFixed() {
    if (!id) return;
    if (!fixNotes.trim()) {
      toast.error("Please explain the fix implementation notes.");
      return;
    }
    setSubmittingFix(true);
    try {
      await api.post(`/findings/${id}/remediation/ready`, {
        commitRef: fixCommitRef.trim() || undefined,
        fixNotes: fixNotes.trim(),
      });
      toast.success("Remediation marked as READY FOR RETEST");
      qc.invalidateQueries({ queryKey: ["remediation", id] });
      qc.invalidateQueries({ queryKey: ["finding", id] });
      setShowMarkFixedModal(false);
      setFixCommitRef("");
      setFixNotes("");
      setTab("remediation");
    } catch (e) {
      toast.error(extractError(e));
    } finally {
      setSubmittingFix(false);
    }
  }

  async function handleRetest() {
    if (!id) return;
    if (!retestNotes.trim()) {
      toast.error("Please record retest observations and notes.");
      return;
    }
    setSubmittingRetest(true);
    try {
      await api.post(`/findings/${id}/retest`, {
        result: retestResult,
        notes: retestNotes.trim(),
      });
      toast.success(`Retest recorded: ${retestResult}`);
      qc.invalidateQueries({ queryKey: ["remediation", id] });
      qc.invalidateQueries({ queryKey: ["retests", id] });
      qc.invalidateQueries({ queryKey: ["finding", id] });
      setShowRetestModal(false);
      setRetestNotes("");
      setTab("remediation");
    } catch (e) {
      toast.error(extractError(e));
    } finally {
      setSubmittingRetest(false);
    }
  }

  async function handleOverridePriority() {
    if (!id) return;
    if (!overrideReason.trim()) {
      toast.error("Security Manager justification reason is required for priority override.");
      return;
    }
    setSubmittingOverride(true);
    try {
      await api.patch(`/findings/${id}/priority`, {
        severity: overrideSeverity,
        reason: overrideReason.trim(),
      });
      toast.success("Finding priority overridden and audited");
      qc.invalidateQueries({ queryKey: ["finding", id] });
      setShowOverrideModal(false);
      setOverrideReason("");
    } catch (e) {
      toast.error(extractError(e));
    } finally {
      setSubmittingOverride(false);
    }
  }

  async function handleAssignUser() {
    if (!id) return;
    try {
      await api.patch(`/findings/${id}/assign`, {
        assignedTo: selectedAssignee ? selectedAssignee : null,
        dueDate: selectedDueDate ? new Date(selectedDueDate).toISOString() : undefined,
      });
      toast.success("Finding assignment updated");
      qc.invalidateQueries({ queryKey: ["finding", id] });
    } catch (e) {
      toast.error(extractError(e));
    }
  }

  if (isLoading) {
    return (
      <>
        <Header title="Finding" description="Loading…" />
        <div className="p-8 text-slate-500">Loading finding details…</div>
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

  const isOverdue = finding.dueDate && new Date(finding.dueDate) < new Date() &&
    finding.status !== "VERIFIED" && finding.status !== "CLOSED";

  const tabs: { key: Tab; label: string; icon: any; show: boolean }[] = [
    { key: "overview",    label: "Overview",    icon: FileText, show: true },
    { key: "evidence",    label: "Evidence",    icon: Shield,   show: canSeeEvidence },
    { key: "remediation", label: "Remediation", icon: Wrench,   show: canSeeRemediation },
    { key: "governance",  label: "Governance & SLA", icon: SlidersHorizontal, show: isManager },
  ];

  return (
    <>
      <Header
        title={`${finding.reference} — ${finding.title}`}
        description={`Detected ${formatDate(finding.createdAt)}`}
        actions={
          <div className="flex items-center gap-2 flex-wrap">
            <RoleGuard roles={["ADMIN", "SECURITY_MANAGER"]}>
              <Button size="sm" variant="outline" onClick={() => {
                setOverrideSeverity(finding.severity);
                setShowOverrideModal(true);
              }}>
                <SlidersHorizontal className="h-3.5 w-3.5 mr-1" /> Override Priority
              </Button>
            </RoleGuard>

            <RoleGuard roles={["ADMIN", "SECURITY_MANAGER", "ANALYST", "DEVELOPER"]}>
              {finding.status !== "VERIFIED" && finding.status !== "CLOSED" && finding.status !== "IN_PROGRESS" && (
                <Button size="sm" variant="outline" onClick={startRemediation}>
                  <Play className="h-3.5 w-3.5 mr-1" /> Start Remediation
                </Button>
              )}
            </RoleGuard>

            <RoleGuard roles={["ADMIN", "SECURITY_MANAGER", "ANALYST", "DEVELOPER"]}>
              {(finding.status === "IN_PROGRESS" || finding.status === "REOPENED") && (
                <Button size="sm" onClick={() => setShowMarkFixedModal(true)}>
                  <CheckCircle2 className="h-3.5 w-3.5 mr-1" /> Submit for Retest
                </Button>
              )}
            </RoleGuard>

            <RoleGuard roles={["ADMIN", "SECURITY_MANAGER", "PENTESTER", "ANALYST"]}>
              {finding.status === "READY_FOR_RETEST" && (
                <Button size="sm" onClick={() => {
                  setRetestResult("PASS");
                  setShowRetestModal(true);
                }}>
                  <TestTube className="h-3.5 w-3.5 mr-1" /> Perform Retest
                </Button>
              )}
            </RoleGuard>
          </div>
        }
      />

      <div className="flex-1 overflow-y-auto p-8 space-y-4">
        <Link to="/findings" className="text-xs text-blue-400 hover:underline inline-flex items-center gap-1">
          <ArrowLeft className="h-3 w-3" /> Back to findings
        </Link>

        {finding.priorityOverrideReason && (
          <div className="p-3 bg-amber-500/10 border border-amber-500/30 rounded-lg flex items-center gap-3 text-xs text-amber-200">
            <AlertTriangle className="h-4 w-4 text-amber-400 shrink-0" />
            <div>
              <span className="font-semibold">Security Manager Priority Override:</span> {finding.priorityOverrideReason}
            </div>
          </div>
        )}

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
            <div className="text-xs text-slate-500 uppercase tracking-wider">SLA Due Date</div>
            <div className="mt-2 flex items-center gap-2">
              {finding.dueDate ? (
                <>
                  <span className="text-sm font-medium text-white">{formatDate(finding.dueDate)}</span>
                  {isOverdue && (
                    <Badge kind="danger" className="text-[10px]">OVERDUE</Badge>
                  )}
                </>
              ) : (
                <span className="text-sm text-slate-500">Not set</span>
              )}
            </div>
          </Card>
        </div>

        <div className="border-b border-slate-800 flex gap-1">
          {tabs.filter(t => t.show).map(({ key, label, icon: Icon }) => (
            <button
              key={key}
              onClick={() => setTab(key)}
              className={`flex items-center gap-2 px-4 py-2 text-sm font-medium border-b-2 transition-colors ${
                tab === key ? "border-blue-500 text-blue-400"
                            : "border-transparent text-slate-400 hover:text-white"
              }`}
            >
              <Icon className="h-4 w-4" />
              {label}
            </button>
          ))}
        </div>

        {tab === "overview" && (
          <div className="space-y-4">
            {/* Risk Formula Explanation Card */}
            <Card className="border-slate-800 bg-slate-900/60">
              <CardHeader className="pb-2">
                <CardTitle className="text-sm">Risk Score Breakdown (Configurable Formula)</CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                <p className="text-xs text-slate-400">
                  Risk score = <span className="text-blue-300 font-mono">0.40 × Severity</span> +{" "}
                  <span className="text-blue-300 font-mono">0.25 × Confidence</span> +{" "}
                  <span className="text-blue-300 font-mono">0.35 × Business Impact</span>.
                </p>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-3 pt-2">
                  <div className="p-3 bg-slate-950 rounded border border-slate-800">
                    <div className="text-[10px] uppercase tracking-wider text-slate-500">Severity Factor (40%)</div>
                    <div className="text-sm font-bold text-white mt-1">{finding.severity}</div>
                  </div>
                  <div className="p-3 bg-slate-950 rounded border border-slate-800">
                    <div className="text-[10px] uppercase tracking-wider text-slate-500">Confidence Factor (25%)</div>
                    <div className="text-sm font-bold text-white mt-1">
                      {finding.confidence != null ? `${(Number(finding.confidence) * 100).toFixed(0)}%` : "100%"}
                    </div>
                  </div>
                  <div className="p-3 bg-slate-950 rounded border border-slate-800">
                    <div className="text-[10px] uppercase tracking-wider text-slate-500">Business Impact (35%)</div>
                    <div className="text-sm font-bold text-white mt-1">Application Criticality</div>
                  </div>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader><CardTitle>Technical Details</CardTitle></CardHeader>
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
                    <dt className="text-xs text-slate-500 uppercase tracking-wider">Source Origin</dt>
                    <dd className="text-slate-200 mt-1">{finding.source ?? "MANUAL"}</dd>
                  </div>
                  <div>
                    <dt className="text-xs text-slate-500 uppercase tracking-wider">Assigned Developer</dt>
                    <dd className="text-slate-200 font-mono text-xs mt-1">
                      {finding.assignedTo ?? "Unassigned"}
                    </dd>
                  </div>
                </dl>
              </CardContent>
            </Card>
          </div>
        )}

        {tab === "evidence" && canSeeEvidence && (
          <Card>
            <CardHeader className="flex flex-row items-center justify-between pb-4">
              <CardTitle>Evidence Artifacts</CardTitle>
              <div>
                <input
                  type="file"
                  ref={fileInputRef}
                  className="hidden"
                  onChange={(evt) => {
                    const file = evt.target.files?.[0];
                    if (file) handleEvidenceUpload(file);
                    evt.target.value = "";
                  }}
                />
                <Button
                  size="sm"
                  onClick={() => fileInputRef.current?.click()}
                  disabled={uploadingEvidence}
                  className="gap-2 bg-blue-600 hover:bg-blue-500 text-white"
                >
                  <Upload className="h-4 w-4" />
                  {uploadingEvidence ? "Uploading..." : "Upload Evidence"}
                </Button>
              </div>
            </CardHeader>
            <CardContent>
              {evidenceQ.isLoading ? (
                <div className="text-sm text-slate-500 py-8 text-center">Loading…</div>
              ) : !evidenceQ.data || evidenceQ.data.length === 0 ? (
                <EmptyState icon={Shield} title="No evidence attached"
                  description="Upload HTTP requests, responses, or screenshots to cryptographically prove this finding." />
              ) : (
                <div className="space-y-3">
                  {evidenceQ.data.map((e: any) => {
                    const vResult = verifyResults[e.id];
                    return (
                      <div key={e.id} className="rounded-md border border-slate-800 bg-slate-800/40 p-4">
                        <div className="flex items-center justify-between mb-2">
                          <div className="flex items-center gap-2">
                            <FileText className="h-4 w-4 text-blue-400" />
                            <span className="text-sm font-medium text-white">{e.filename}</span>
                          </div>
                          <div className="flex items-center gap-3">
                            <span className="text-xs text-slate-500">{formatDate(e.createdAt)}</span>
                            <Button
                              size="sm"
                              variant="outline"
                              className="text-xs h-7 px-2 border-slate-700 hover:border-slate-600"
                              disabled={verifyingId === e.id}
                              onClick={() => handleVerifyEvidence(e.id)}
                            >
                              <ShieldCheck className="h-3.5 w-3.5 mr-1 text-emerald-400" />
                              {verifyingId === e.id ? "Verifying..." : "Verify Integrity"}
                            </Button>
                          </div>
                        </div>
                        <div className="flex items-center justify-between text-xs text-slate-500">
                          <div>
                            <span className="font-medium text-slate-400">Size:</span> {e.sizeBytes} bytes
                          </div>
                          {vResult && (
                            <span className={`px-2 py-0.5 rounded text-[11px] font-semibold ${
                              vResult.integrity === "INTACT"
                                ? "bg-emerald-500/15 text-emerald-300 border border-emerald-500/30"
                                : "bg-red-500/15 text-red-300 border border-red-500/30"
                            }`}>
                              {vResult.integrity === "INTACT" ? "✓ INTACT (SHA-256 Validated)" : `⚠ ${vResult.integrity}`}
                            </span>
                          )}
                        </div>
                        <div className="text-xs mt-2 break-all bg-slate-900/60 p-2 rounded border border-slate-800/80">
                          <span className="font-medium text-slate-400">SHA-256:</span>{" "}
                          <span className="font-mono text-emerald-400">{e.sha256}</span>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </CardContent>
          </Card>
        )}

        {tab === "remediation" && canSeeRemediation && (
          <div className="space-y-4">
            <Card>
              <CardHeader><CardTitle>Remediation Lifecycle</CardTitle></CardHeader>
              <CardContent>
                {remediationQ.isLoading ? (
                  <div className="text-sm text-slate-500 py-4">Loading…</div>
                ) : !remediationQ.data ? (
                  <EmptyState icon={Wrench} title="No remediation started"
                    description="Developers can start remediation to record code fixes and submit for verification." />
                ) : (
                  <div className="space-y-4 text-sm">
                    <div className="flex items-center gap-2">
                      <Badge kind="remediation" value={remediationQ.data.status}>
                        {remediationQ.data.status}
                      </Badge>
                      {remediationQ.data.reopenedCount > 0 && (
                        <span className="text-xs text-red-400 font-medium">
                          Reopened {remediationQ.data.reopenedCount}×
                        </span>
                      )}
                    </div>
                    {remediationQ.data.fixNotes && (
                      <div className="p-3 bg-slate-950 rounded border border-slate-800">
                        <div className="text-xs text-slate-500 uppercase tracking-wider mb-1">Developer Fix Notes</div>
                        <div className="text-slate-200">{remediationQ.data.fixNotes}</div>
                      </div>
                    )}
                    {remediationQ.data.commitRef && (
                      <div>
                        <div className="text-xs text-slate-500 uppercase tracking-wider mb-1">Commit Reference</div>
                        <div className="font-mono text-xs text-blue-300">{remediationQ.data.commitRef}</div>
                      </div>
                    )}
                    {remediationQ.data.fixedAt && (
                      <div className="text-xs text-slate-500">
                        Submitted for Retest: {formatDate(remediationQ.data.fixedAt)}
                      </div>
                    )}
                  </div>
                )}
              </CardContent>
            </Card>

            {retestsQ.data && retestsQ.data.length > 0 && (
              <Card>
                <CardHeader><CardTitle>Retest Audit History</CardTitle></CardHeader>
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
                            {r.performedBy && (
                              <span className="text-xs font-mono text-slate-400">by {r.performedBy.slice(0, 8)}…</span>
                            )}
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

        {tab === "governance" && isManager && (
          <Card>
            <CardHeader><CardTitle>Security Governance & Assignment</CardTitle></CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="text-xs text-slate-400 font-medium mb-1 block">Assign Developer</label>
                  <select
                    value={selectedAssignee || finding.assignedTo || ""}
                    onChange={(e) => setSelectedAssignee(e.target.value)}
                    className="w-full h-10 rounded-md border border-slate-700 bg-slate-900 px-3 text-sm text-slate-100"
                  >
                    <option value="">-- Unassigned --</option>
                    {usersQ.data?.map(u => (
                      <option key={u.id} value={u.id}>{u.fullName} ({u.role}) - {u.email}</option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="text-xs text-slate-400 font-medium mb-1 block">Remediation SLA Due Date</label>
                  <Input
                    type="date"
                    value={selectedDueDate || (finding.dueDate ? finding.dueDate.slice(0, 10) : "")}
                    onChange={(e) => setSelectedDueDate(e.target.value)}
                  />
                </div>
              </div>

              <div className="flex justify-end pt-2">
                <Button size="sm" onClick={handleAssignUser}>
                  <UserCheck className="h-4 w-4 mr-1" /> Save Assignment & SLA
                </Button>
              </div>
            </CardContent>
          </Card>
        )}
      </div>

      {/* Mark Fixed Modal */}
      {showMarkFixedModal && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
          <Card className="w-full max-w-lg border-slate-700 bg-slate-900 shadow-xl">
            <CardHeader className="flex flex-row items-center justify-between pb-3">
              <CardTitle>Submit Fix for Retest</CardTitle>
              <Button size="sm" variant="ghost" onClick={() => setShowMarkFixedModal(false)}>
                <X className="h-4 w-4" />
              </Button>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <label className="text-xs text-slate-400 font-medium mb-1 block">
                  Git Commit Reference / PR link
                </label>
                <Input
                  placeholder="e.g. git:commit-a1b2c3d or PR #42"
                  value={fixCommitRef}
                  onChange={(e) => setFixCommitRef(e.target.value)}
                />
              </div>

              <div>
                <label className="text-xs text-slate-400 font-medium mb-1 block">
                  Implementation Notes & Fix Details <span className="text-rose-400">*</span>
                </label>
                <textarea
                  rows={4}
                  placeholder="Explain the security patch, validation applied, or configuration changed..."
                  value={fixNotes}
                  onChange={(e) => setFixNotes(e.target.value)}
                  className="w-full rounded-md border border-slate-700 bg-slate-950 p-3 text-sm text-slate-100 placeholder:text-slate-600 focus:outline-none focus:ring-1 focus:ring-blue-500"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <Button variant="ghost" onClick={() => setShowMarkFixedModal(false)} disabled={submittingFix}>
                  Cancel
                </Button>
                <Button onClick={handleMarkFixed} disabled={submittingFix}>
                  {submittingFix && <Loader2 className="h-4 w-4 animate-spin mr-1" />}
                  Submit for Retest
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>
      )}

      {/* Retest Modal */}
      {showRetestModal && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
          <Card className="w-full max-w-lg border-slate-700 bg-slate-900 shadow-xl">
            <CardHeader className="flex flex-row items-center justify-between pb-3">
              <CardTitle>Record Verification Retest</CardTitle>
              <Button size="sm" variant="ghost" onClick={() => setShowRetestModal(false)}>
                <X className="h-4 w-4" />
              </Button>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <label className="text-xs text-slate-400 font-medium mb-1 block">Retest Outcome</label>
                <select
                  value={retestResult}
                  onChange={(e) => setRetestResult(e.target.value as "PASS" | "FAIL")}
                  className="w-full h-10 rounded-md border border-slate-700 bg-slate-950 px-3 text-sm text-slate-100"
                >
                  <option value="PASS">PASS (Vulnerability Verified Mitigated - Closes Finding)</option>
                  <option value="FAIL">FAIL (Vulnerability Still Present - Reopens Finding)</option>
                </select>
              </div>

              <div>
                <label className="text-xs text-slate-400 font-medium mb-1 block">
                  Retest Methodology & Observations <span className="text-rose-400">*</span>
                </label>
                <textarea
                  rows={4}
                  placeholder="Record retest payload, HTTP response, or proof of mitigation..."
                  value={retestNotes}
                  onChange={(e) => setRetestNotes(e.target.value)}
                  className="w-full rounded-md border border-slate-700 bg-slate-950 p-3 text-sm text-slate-100 placeholder:text-slate-600 focus:outline-none focus:ring-1 focus:ring-blue-500"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <Button variant="ghost" onClick={() => setShowRetestModal(false)} disabled={submittingRetest}>
                  Cancel
                </Button>
                <Button
                  onClick={handleRetest}
                  disabled={submittingRetest}
                  variant={retestResult === "PASS" ? "primary" : "outline"}
                >
                  {submittingRetest && <Loader2 className="h-4 w-4 animate-spin mr-1" />}
                  Confirm Retest ({retestResult})
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>
      )}

      {/* Priority Override Modal */}
      {showOverrideModal && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
          <Card className="w-full max-w-lg border-slate-700 bg-slate-900 shadow-xl">
            <CardHeader className="flex flex-row items-center justify-between pb-3">
              <CardTitle>Override Finding Priority</CardTitle>
              <Button size="sm" variant="ghost" onClick={() => setShowOverrideModal(false)}>
                <X className="h-4 w-4" />
              </Button>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <label className="text-xs text-slate-400 font-medium mb-1 block">New Severity</label>
                <select
                  value={overrideSeverity}
                  onChange={(e) => setOverrideSeverity(e.target.value as Severity)}
                  className="w-full h-10 rounded-md border border-slate-700 bg-slate-950 px-3 text-sm text-slate-100"
                >
                  {SEVERITIES.map(s => <option key={s} value={s}>{s}</option>)}
                </select>
              </div>

              <div>
                <label className="text-xs text-slate-400 font-medium mb-1 block">
                  Manager Justification Reason <span className="text-rose-400">*</span>
                </label>
                <textarea
                  rows={4}
                  placeholder="Explain why this finding priority is being overridden (audited)..."
                  value={overrideReason}
                  onChange={(e) => setOverrideReason(e.target.value)}
                  className="w-full rounded-md border border-slate-700 bg-slate-950 p-3 text-sm text-slate-100 placeholder:text-slate-600 focus:outline-none focus:ring-1 focus:ring-blue-500"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <Button variant="ghost" onClick={() => setShowOverrideModal(false)} disabled={submittingOverride}>
                  Cancel
                </Button>
                <Button onClick={handleOverridePriority} disabled={submittingOverride}>
                  {submittingOverride && <Loader2 className="h-4 w-4 animate-spin mr-1" />}
                  Apply Override
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>
      )}
    </>
  );
}
