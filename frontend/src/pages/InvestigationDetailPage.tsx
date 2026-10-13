import { useParams, Link } from "react-router-dom";
import { useState } from "react";
import {
  ArrowLeft, Search, Swords, AlertTriangle, MessageSquare,
  Send, Sparkles
} from "lucide-react";
import { Header } from "@/components/layout/Header";
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/Card";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import { api, extractError } from "@/lib/api";
import { useInvestigationSummary } from "@/lib/queries";
import { useQueryClient } from "@tanstack/react-query";
import { formatDate } from "@/lib/utils";
import { toast } from "sonner";

const STAGE_LABEL: Record<string, string> = {
  RECON: "Reconnaissance",
  AUTH_ABUSE: "Authentication Abuse",
  EXPLOITATION: "Exploitation",
  DATA_ACCESS: "Data Access",
  IMPACT: "Impact",
};

export function InvestigationDetailPage() {
  const { id } = useParams<{ id: string }>();
  const qc = useQueryClient();
  const { data: summary, isLoading } = useInvestigationSummary(id);
  const [note, setNote] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [aiLoading, setAiLoading] = useState(false);
  const [aiResult, setAiResult] = useState<string | null>(null);

  async function submitNote() {
    if (!note.trim() || !id) return;
    setSubmitting(true);
    try {
      await api.post(`/investigations/${id}/notes`, { note });
      setNote("");
      qc.invalidateQueries({ queryKey: ["investigation-summary", id] });
      toast.success("Note added");
    } catch (e) {
      toast.error(extractError(e));
    } finally {
      setSubmitting(false);
    }
  }

  async function askAi() {
    if (!id) return;
    setAiLoading(true);
    setAiResult(null);
    try {
      const res = await api.post(`/ai/summarize/investigation/${id}`);
      setAiResult((res.data as { result: string }).result);
    } catch (e) {
      toast.error(extractError(e));
    } finally {
      setAiLoading(false);
    }
  }

  if (isLoading) {
    return (
      <>
        <Header title="Investigation" description="Loading…" />
        <div className="p-8 text-slate-500">Loading…</div>
      </>
    );
  }

  if (!summary) {
    return (
      <>
        <Header title="Investigation not found" />
        <div className="p-8">
          <Link to="/investigations" className="text-blue-400 hover:underline flex items-center gap-1">
            <ArrowLeft className="h-4 w-4" /> Back
          </Link>
        </div>
      </>
    );
  }

  const { investigation, campaign, timeline, findings, notes } = summary;

  return (
    <>
      <Header
        title={`${investigation.reference} — ${investigation.title}`}
        description={`Opened ${formatDate(investigation.createdAt)}`}
        actions={
          <Button variant="outline" size="sm" onClick={askAi} disabled={aiLoading}>
            <Sparkles className="h-3.5 w-3.5" />
            {aiLoading ? "Analyzing…" : "AI Summary"}
          </Button>
        }
      />
      <div className="flex-1 overflow-y-auto p-8 space-y-6">

        <Link to="/investigations" className="text-xs text-blue-400 hover:underline inline-flex items-center gap-1">
          <ArrowLeft className="h-3 w-3" /> Back to investigations
        </Link>

        {/* AI Result */}
        {aiResult && (
          <Card className="border-blue-500/40 bg-blue-500/5">
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Sparkles className="h-4 w-4 text-blue-400" />
                AI Analysis
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="text-sm text-slate-200 whitespace-pre-wrap leading-relaxed">
                {aiResult}
              </div>
            </CardContent>
          </Card>
        )}

        {/* Meta */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <Card className="p-5">
            <div className="text-xs text-slate-500 uppercase tracking-wider">Status</div>
            <div className="mt-2"><Badge kind="status" value={investigation.status}>{investigation.status}</Badge></div>
          </Card>
          <Card className="p-5">
            <div className="text-xs text-slate-500 uppercase tracking-wider">Priority</div>
            <div className="mt-2"><Badge kind="risk" value={investigation.priority}>{investigation.priority}</Badge></div>
          </Card>
          <Card className="p-5">
            <div className="text-xs text-slate-500 uppercase tracking-wider">Assigned To</div>
            <div className="mt-2 text-white font-mono text-xs">
              {investigation.assignedTo ? investigation.assignedTo.slice(0, 12) + "…" : "unassigned"}
            </div>
          </Card>
          <Card className="p-5">
            <div className="text-xs text-slate-500 uppercase tracking-wider">Findings</div>
            <div className="mt-2 text-white text-2xl font-semibold">{findings.length}</div>
          </Card>
        </div>

        {/* Description */}
        {investigation.description && (
          <Card>
            <CardHeader><CardTitle>Description</CardTitle></CardHeader>
            <CardContent><p className="text-sm text-slate-300">{investigation.description}</p></CardContent>
          </Card>
        )}

        {/* Campaign + Timeline */}
        {campaign && (
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Swords className="h-4 w-4 text-orange-400" />
                Linked Campaign — {campaign.reference}
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="text-sm text-slate-400 mb-4">
                {campaign.title} — Source: <span className="font-mono text-slate-200">{campaign.sourceIp}</span>
                {" · "}Risk: <Badge kind="risk" value={campaign.riskBand ?? "MEDIUM"}>{campaign.riskBand ?? "—"}</Badge>
              </div>

              <div className="relative pl-6">
                <div className="absolute left-2 top-2 bottom-2 w-0.5 bg-slate-800" />
                {timeline.length === 0 ? (
                  <div className="text-sm text-slate-500 py-4">No events</div>
                ) : (
                  timeline.map((evt) => (
                    <div key={evt.id} className="relative pb-4 last:pb-0">
                      <div className="absolute -left-4 top-1.5 h-3 w-3 rounded-full border-2 border-slate-700 bg-slate-900" />
                      <div className="text-xs text-slate-500">Step {evt.sequenceNo}</div>
                      <div className="text-sm text-slate-200">
                        {STAGE_LABEL[evt.stage] ?? evt.stage}
                        {evt.detectionType && (
                          <span className="text-slate-500 text-xs ml-2">— {evt.detectionType}</span>
                        )}
                      </div>
                      <div className="text-xs text-slate-600 mt-0.5">{formatDate(evt.createdAt)}</div>
                    </div>
                  ))
                )}
              </div>
            </CardContent>
          </Card>
        )}

        {/* Findings */}
        {findings.length > 0 && (
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <AlertTriangle className="h-4 w-4 text-orange-400" />
                Linked Findings
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-2">
                {findings.map((f) => (
                  <Link key={f.id} to={`/findings/${f.id}`}
                    className="flex items-center justify-between p-3 rounded-md bg-slate-800/40 border border-slate-800 hover:border-slate-700 transition-colors">
                    <div className="min-w-0">
                      <div className="flex items-center gap-2">
                        <span className="text-xs font-mono text-slate-500">{f.reference}</span>
                        <Badge kind="severity" value={f.severity}>{f.severity}</Badge>
                      </div>
                      <div className="text-sm text-white mt-1 truncate">{f.title}</div>
                    </div>
                    <div className="text-right shrink-0 ml-3">
                      <div className="text-xs text-slate-500">risk</div>
                      <div className="text-sm font-semibold text-orange-300">
                        {f.riskScore?.toFixed(1) ?? "—"}
                      </div>
                    </div>
                  </Link>
                ))}
              </div>
            </CardContent>
          </Card>
        )}

        {/* Notes */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <MessageSquare className="h-4 w-4 text-blue-400" />
              Analyst Notes ({notes.length})
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-3 mb-4">
              {notes.length === 0 ? (
                <div className="text-sm text-slate-500 text-center py-4">
                  No notes yet. Add the first note below.
                </div>
              ) : (
                notes.map((n) => (
                  <div key={n.id} className="rounded-md border border-slate-800 bg-slate-800/40 p-3">
                    <div className="flex items-center justify-between mb-1">
                      <span className="text-xs font-mono text-slate-500">
                        {n.authorId ? n.authorId.slice(0, 8) + "…" : "system"}
                      </span>
                      <span className="text-xs text-slate-600">{formatDate(n.createdAt)}</span>
                    </div>
                    <div className="text-sm text-slate-200">{n.note}</div>
                  </div>
                ))
              )}
            </div>

            <div className="flex gap-2">
              <textarea
                value={note}
                onChange={(e) => setNote(e.target.value)}
                placeholder="Add an analyst note…"
                rows={2}
                className="flex-1 rounded-md border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-100 placeholder:text-slate-500 focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none"
              />
              <Button onClick={submitNote} disabled={submitting || !note.trim()} size="icon"
                className="h-auto w-12 shrink-0">
                <Send className="h-4 w-4" />
              </Button>
            </div>
          </CardContent>
        </Card>
      </div>
    </>
  );
}
