import { useState } from "react";
import { useQueryClient, useQuery } from "@tanstack/react-query";
import { Bell, CheckCircle2, ShieldAlert, Clock, Filter, Eye, AlertOctagon, Loader2, X } from "lucide-react";
import { toast } from "sonner";
import { Header } from "@/components/layout/Header";
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/Card";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import { useAlerts } from "@/lib/queries";
import { api, extractError } from "@/lib/api";
import { formatDate } from "@/lib/utils";
import { useAuth } from "@/contexts/AuthContext";
import type { AlertDto, AlertStatus, EventDto } from "@/lib/types";

export function AlertsPage() {
  const qc = useQueryClient();
  const { user } = useAuth();
  const [statusFilter, setStatusFilter] = useState<string>("");
  const { data: alerts = [], isLoading } = useAlerts(statusFilter || undefined);
  const [acknowledgingId, setAcknowledgingId] = useState<string | null>(null);

  // Triage state
  const [triageAlert, setTriageAlert] = useState<AlertDto | null>(null);
  const [triageStatus, setTriageStatus] = useState<AlertStatus>("RESOLVED");
  const [triageNotes, setTriageNotes] = useState("");
  const [triaging, setTriaging] = useState(false);

  // Inspect event state
  const [inspectEventId, setInspectEventId] = useState<string | null>(null);
  const eventQ = useQuery({
    queryKey: ["event", inspectEventId],
    enabled: !!inspectEventId,
    queryFn: async () => (await api.get<EventDto>(`/events/${inspectEventId}`)).data,
  });

  const canTriage = user && ["ADMIN", "SECURITY_MANAGER", "ANALYST"].includes(user.role);

  async function acknowledgeAlert(id: string) {
    setAcknowledgingId(id);
    try {
      await api.post(`/alerts/${id}/acknowledge`);
      toast.success("Alert acknowledged");
      qc.invalidateQueries({ queryKey: ["alerts"] });
    } catch (e) {
      toast.error(extractError(e));
    } finally {
      setAcknowledgingId(null);
    }
  }

  async function handleTriage() {
    if (!triageAlert) return;
    if (!triageNotes.trim()) {
      toast.error("Please provide disposition notes explaining the triage rationale");
      return;
    }
    setTriaging(true);
    try {
      await api.post(`/alerts/${triageAlert.id}/triage`, {
        status: triageStatus,
        notes: triageNotes.trim(),
      });
      toast.success(`Alert triaged as ${triageStatus}`);
      qc.invalidateQueries({ queryKey: ["alerts"] });
      setTriageAlert(null);
      setTriageNotes("");
    } catch (e) {
      toast.error(extractError(e));
    } finally {
      setTriaging(false);
    }
  }

  const safeAlerts = Array.isArray(alerts) ? alerts : [];
  const openCount = safeAlerts.filter(a => a.status === "OPEN" || (a.status as string) === "NEW").length;
  const criticalCount = safeAlerts.filter(a => a.severity === "CRITICAL").length;
  const highCount = safeAlerts.filter(a => a.severity === "HIGH").length;

  return (
    <>
      <Header
        title="Security Alerts"
        description="Actionable threat notifications triggered by deterministic detection rules"
      />

      <div className="flex-1 overflow-y-auto p-8 space-y-6">
        {/* Metric summary */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <Card className="bg-slate-900/60 border-slate-800">
            <CardContent className="p-4 flex items-center justify-between">
              <div>
                <p className="text-xs text-slate-400 font-medium">Total Alerts</p>
                <p className="text-2xl font-bold text-white mt-1">{safeAlerts.length}</p>
              </div>
              <div className="h-10 w-10 rounded-lg bg-blue-500/15 flex items-center justify-center text-blue-400">
                <Bell className="h-5 w-5" />
              </div>
            </CardContent>
          </Card>

          <Card className="bg-slate-900/60 border-slate-800">
            <CardContent className="p-4 flex items-center justify-between">
              <div>
                <p className="text-xs text-slate-400 font-medium">Unacknowledged (Open)</p>
                <p className="text-2xl font-bold text-amber-400 mt-1">{openCount}</p>
              </div>
              <div className="h-10 w-10 rounded-lg bg-amber-500/15 flex items-center justify-center text-amber-400">
                <Clock className="h-5 w-5" />
              </div>
            </CardContent>
          </Card>

          <Card className="bg-slate-900/60 border-slate-800">
            <CardContent className="p-4 flex items-center justify-between">
              <div>
                <p className="text-xs text-slate-400 font-medium">Critical Alerts</p>
                <p className="text-2xl font-bold text-red-400 mt-1">{criticalCount}</p>
              </div>
              <div className="h-10 w-10 rounded-lg bg-red-500/15 flex items-center justify-center text-red-400">
                <ShieldAlert className="h-5 w-5" />
              </div>
            </CardContent>
          </Card>

          <Card className="bg-slate-900/60 border-slate-800">
            <CardContent className="p-4 flex items-center justify-between">
              <div>
                <p className="text-xs text-slate-400 font-medium">High Severity</p>
                <p className="text-2xl font-bold text-orange-400 mt-1">{highCount}</p>
              </div>
              <div className="h-10 w-10 rounded-lg bg-orange-500/15 flex items-center justify-center text-orange-400">
                <ShieldAlert className="h-5 w-5" />
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Filter controls */}
        <div className="flex items-center gap-2">
          <Filter className="h-4 w-4 text-slate-400" />
          <span className="text-xs text-slate-400 font-medium mr-2">Filter by Status:</span>
          {(["", "OPEN", "ACKNOWLEDGED", "RESOLVED", "DISMISSED"] as const).map((s) => (
            <button
              key={s}
              onClick={() => setStatusFilter(s)}
              className={`px-3 py-1 text-xs rounded-full border transition-colors ${
                statusFilter === s
                  ? "bg-blue-500/20 text-blue-300 border-blue-500/50 font-semibold"
                  : "bg-slate-900 text-slate-400 border-slate-800 hover:border-slate-700"
              }`}
            >
              {s === "" ? "ALL" : s}
            </button>
          ))}
        </div>

        {/* Alert list */}
        <Card className="border-slate-800 bg-slate-900/40">
          <CardHeader>
            <CardTitle>Incoming Security Alerts</CardTitle>
          </CardHeader>
          <CardContent>
            {isLoading ? (
              <div className="py-8 text-center text-sm text-slate-500">Loading alerts…</div>
            ) : safeAlerts.length === 0 ? (
              <EmptyState icon={Bell} title="No security alerts found" description="No alerts match the selected status filter." />
            ) : (
              <div className="space-y-3">
                {safeAlerts.map((alert) => (
                  <div
                    key={alert.id}
                    className="p-4 rounded-lg border border-slate-800 bg-slate-900/60 flex flex-col md:flex-row md:items-start justify-between gap-4 hover:border-slate-700 transition-colors"
                  >
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 mb-1 flex-wrap">
                        <span className="font-mono text-xs text-slate-400 font-semibold">{alert.reference}</span>
                        <Badge kind="severity" value={alert.severity}>{alert.severity}</Badge>
                        <span className={`text-[11px] px-2 py-0.5 rounded-full font-medium ${
                          alert.status === "OPEN" || (alert.status as string) === "NEW"
                            ? "bg-amber-500/15 text-amber-300 border border-amber-500/30"
                            : alert.status === "RESOLVED"
                            ? "bg-emerald-500/15 text-emerald-300 border border-emerald-500/30"
                            : alert.status === "DISMISSED"
                            ? "bg-slate-700/50 text-slate-400 border border-slate-600"
                            : "bg-blue-500/15 text-blue-300 border border-blue-500/30"
                        }`}>
                          {alert.status}
                        </span>
                      </div>
                      <h3 className="text-sm font-semibold text-white">{alert.title}</h3>
                      {alert.description && (
                        <p className="text-xs text-slate-400 mt-1 line-clamp-2">{alert.description}</p>
                      )}

                      {alert.dispositionNotes && (
                        <div className="mt-2 p-2 rounded bg-slate-950/60 border border-slate-800 text-xs">
                          <span className="text-slate-400 font-medium">Disposition Note: </span>
                          <span className="text-slate-200">{alert.dispositionNotes}</span>
                          {alert.dispositionAt && (
                            <span className="text-slate-500 ml-2">({formatDate(alert.dispositionAt)})</span>
                          )}
                        </div>
                      )}

                      <div className="flex items-center gap-4 text-[11px] text-slate-500 mt-2">
                        <span>Risk: <strong className="text-slate-300">{Number(alert.riskScore).toFixed(1)}</strong></span>
                        <span>Confidence: <strong className="text-slate-300">{(Number(alert.confidence) * 100).toFixed(0)}%</strong></span>
                        <span>Detected: {formatDate(alert.createdAt)}</span>
                        {alert.acknowledgedAt && (
                          <span className="text-blue-400">Ack'd: {formatDate(alert.acknowledgedAt)}</span>
                        )}
                      </div>
                    </div>

                    <div className="flex items-center gap-2 shrink-0 flex-wrap">
                      {alert.eventId && (
                        <Button
                          size="sm"
                          variant="ghost"
                          onClick={() => setInspectEventId(alert.eventId!)}
                          className="text-xs text-slate-300 hover:text-white"
                        >
                          <Eye className="h-3.5 w-3.5 mr-1" />
                          Inspect Event
                        </Button>
                      )}

                      {(alert.status === "OPEN" || (alert.status as string) === "NEW") && (
                        <Button
                          size="sm"
                          variant="outline"
                          disabled={acknowledgingId === alert.id}
                          onClick={() => acknowledgeAlert(alert.id)}
                          className="text-xs border-amber-500/40 text-amber-300 hover:bg-amber-500/10"
                        >
                          <CheckCircle2 className="h-3.5 w-3.5 mr-1" />
                          Acknowledge
                        </Button>
                      )}

                      {canTriage && (
                        <Button
                          size="sm"
                          variant="primary"
                          onClick={() => {
                            setTriageAlert(alert);
                            setTriageStatus(alert.status === "RESOLVED" ? "DISMISSED" : "RESOLVED");
                            setTriageNotes(alert.dispositionNotes ?? "");
                          }}
                          className="text-xs"
                        >
                          <AlertOctagon className="h-3.5 w-3.5 mr-1" />
                          Triage
                        </Button>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Triage Dialog */}
      {triageAlert && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
          <Card className="w-full max-w-lg border-slate-700 bg-slate-900 shadow-xl">
            <CardHeader className="flex flex-row items-center justify-between pb-3">
              <div>
                <CardTitle>Triage Alert: {triageAlert.reference}</CardTitle>
                <p className="text-xs text-slate-400 mt-1">{triageAlert.title}</p>
              </div>
              <Button size="sm" variant="ghost" onClick={() => setTriageAlert(null)}>
                <X className="h-4 w-4" />
              </Button>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <label className="text-xs text-slate-400 font-medium mb-1 block">Triage Disposition</label>
                <select
                  value={triageStatus}
                  onChange={(e) => setTriageStatus(e.target.value as AlertStatus)}
                  className="w-full h-10 rounded-md border border-slate-700 bg-slate-950 px-3 text-sm text-slate-100"
                >
                  <option value="RESOLVED">RESOLVED (Threat Mitigated / Benign Activity)</option>
                  <option value="DISMISSED">DISMISSED (False Positive / Expected Traffic)</option>
                  <option value="ACKNOWLEDGED">ACKNOWLEDGED (Under Ongoing Investigation)</option>
                </select>
              </div>

              <div>
                <label className="text-xs text-slate-400 font-medium mb-1 block">
                  Analyst Rationale / Disposition Notes <span className="text-rose-400">*</span>
                </label>
                <textarea
                  value={triageNotes}
                  onChange={(e) => setTriageNotes(e.target.value)}
                  placeholder="Explain findings, root cause, and reasons for disposition..."
                  rows={4}
                  className="w-full rounded-md border border-slate-700 bg-slate-950 p-3 text-sm text-slate-100 placeholder:text-slate-600 focus:outline-none focus:ring-1 focus:ring-blue-500"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <Button variant="ghost" onClick={() => setTriageAlert(null)} disabled={triaging}>
                  Cancel
                </Button>
                <Button onClick={handleTriage} disabled={triaging}>
                  {triaging && <Loader2 className="h-4 w-4 animate-spin mr-1" />}
                  Submit Disposition
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>
      )}

      {/* Inspect Event Dialog */}
      {inspectEventId && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
          <Card className="w-full max-w-2xl border-slate-700 bg-slate-900 shadow-xl max-h-[85vh] flex flex-col">
            <CardHeader className="flex flex-row items-center justify-between pb-3 border-b border-slate-800">
              <div>
                <CardTitle>Supporting Event Telemetry</CardTitle>
                <p className="text-xs text-slate-400 font-mono mt-1">Event ID: {inspectEventId}</p>
              </div>
              <Button size="sm" variant="ghost" onClick={() => setInspectEventId(null)}>
                <X className="h-4 w-4" />
              </Button>
            </CardHeader>
            <CardContent className="p-4 overflow-y-auto space-y-4 flex-1">
              {eventQ.isLoading ? (
                <div className="py-12 text-center text-sm text-slate-500">Loading event telemetry…</div>
              ) : !eventQ.data ? (
                <div className="py-8 text-center text-sm text-slate-400">Event record not found or inaccessible.</div>
              ) : (
                <div className="space-y-3">
                  <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
                    <div className="p-3 bg-slate-950 rounded border border-slate-800">
                      <div className="text-[10px] uppercase tracking-wider text-slate-500">Source</div>
                      <div className="text-xs font-semibold text-white mt-1">{eventQ.data.source}</div>
                    </div>
                    <div className="p-3 bg-slate-950 rounded border border-slate-800">
                      <div className="text-[10px] uppercase tracking-wider text-slate-500">Source IP</div>
                      <div className="text-xs font-mono text-blue-400 mt-1">{eventQ.data.sourceIp ?? "—"}</div>
                    </div>
                    <div className="p-3 bg-slate-950 rounded border border-slate-800">
                      <div className="text-[10px] uppercase tracking-wider text-slate-500">Method / Status</div>
                      <div className="text-xs font-mono text-emerald-400 mt-1">
                        {eventQ.data.method ?? "—"} {eventQ.data.statusCode ? `(${eventQ.data.statusCode})` : ""}
                      </div>
                    </div>
                    <div className="p-3 bg-slate-950 rounded border border-slate-800">
                      <div className="text-[10px] uppercase tracking-wider text-slate-500">Timestamp</div>
                      <div className="text-xs text-slate-300 mt-1">{formatDate(eventQ.data.eventTime)}</div>
                    </div>
                  </div>

                  {eventQ.data.path && (
                    <div className="p-3 bg-slate-950 rounded border border-slate-800">
                      <div className="text-[10px] uppercase tracking-wider text-slate-500 mb-1">Request Path</div>
                      <div className="text-xs font-mono text-slate-200 break-all">{eventQ.data.path}</div>
                    </div>
                  )}

                  {eventQ.data.userIdentifier && (
                    <div className="p-3 bg-slate-950 rounded border border-slate-800">
                      <div className="text-[10px] uppercase tracking-wider text-slate-500 mb-1">User Identifier</div>
                      <div className="text-xs font-mono text-slate-200">{eventQ.data.userIdentifier}</div>
                    </div>
                  )}

                  {eventQ.data.normalized && (
                    <div>
                      <div className="text-xs text-slate-400 font-medium mb-1">Normalized Attributes</div>
                      <pre className="p-3 rounded bg-slate-950 border border-slate-800 text-xs font-mono text-slate-300 overflow-x-auto max-h-60">
                        {JSON.stringify(eventQ.data.normalized, null, 2)}
                      </pre>
                    </div>
                  )}
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      )}
    </>
  );
}
