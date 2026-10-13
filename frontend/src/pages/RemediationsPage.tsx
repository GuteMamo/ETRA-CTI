import { Wrench, CheckCircle2, XCircle, Clock, RotateCcw } from "lucide-react";
import { Header } from "@/components/layout/Header";
import { Card } from "@/components/ui/Card";
import { Badge } from "@/components/ui/Badge";
import { EmptyState } from "@/components/ui/EmptyState";
import { useRemediations } from "@/lib/queries";
import { formatDate } from "@/lib/utils";
import type { RemediationStatus } from "@/lib/types";

const STATUS_ICON: Record<RemediationStatus, typeof Wrench> = {
  OPEN: Wrench,
  IN_PROGRESS: Clock,
  FIXED: CheckCircle2,
  READY_FOR_RETEST: Clock,
  VERIFIED: CheckCircle2,
  REOPENED: RotateCcw,
};

export function RemediationsPage() {
  const { data: remediations = [], isLoading } = useRemediations();

  const stats = {
    open: remediations.filter(r => r.status === "OPEN").length,
    inProgress: remediations.filter(r => r.status === "IN_PROGRESS").length,
    readyForRetest: remediations.filter(r => r.status === "READY_FOR_RETEST").length,
    verified: remediations.filter(r => r.status === "VERIFIED").length,
    reopened: remediations.filter(r => r.status === "REOPENED").length,
  };

  return (
    <>
      <Header
        title="Remediation Queue"
        description="Track developer fixes and retest verification across all findings"
      />
      <div className="flex-1 overflow-y-auto p-8 space-y-4">

        <div className="grid grid-cols-2 md:grid-cols-5 gap-3">
          {[
            { label: "Open",           value: stats.open,           tone: "red" as const },
            { label: "In Progress",    value: stats.inProgress,     tone: "blue" as const },
            { label: "Ready Retest",   value: stats.readyForRetest, tone: "orange" as const },
            { label: "Verified",       value: stats.verified,       tone: "emerald" as const },
            { label: "Reopened",       value: stats.reopened,       tone: "red" as const },
          ].map(({ label, value, tone }) => (
            <Card key={label} className={`p-4 border-${tone}-500/30 bg-${tone}-500/5`}>
              <div className="text-xs uppercase tracking-wider text-slate-400">{label}</div>
              <div className="text-2xl font-bold text-white mt-1">{value}</div>
            </Card>
          ))}
        </div>

        <Card className="overflow-hidden">
          {isLoading ? (
            <div className="p-12 text-center text-sm text-slate-500">Loading…</div>
          ) : remediations.length === 0 ? (
            <EmptyState icon={Wrench} title="No remediations in progress"
              description="Once developers start fixing findings, they'll appear here in the remediation queue." />
          ) : (
            <table className="w-full text-sm">
              <thead className="bg-slate-900/80 text-slate-400 text-xs uppercase tracking-wider">
                <tr>
                  <th className="text-left px-4 py-3">Finding</th>
                  <th className="text-left px-4 py-3">Status</th>
                  <th className="text-left px-4 py-3">Developer</th>
                  <th className="text-left px-4 py-3">Commit</th>
                  <th className="text-left px-4 py-3">Reopened</th>
                  <th className="text-left px-4 py-3">Started</th>
                </tr>
              </thead>
              <tbody>
                {remediations.map((r) => {
                  const Icon = STATUS_ICON[r.status] ?? Wrench;
                  return (
                    <tr key={r.id} className="border-t border-slate-800 hover:bg-slate-800/40">
                      <td className="px-4 py-3 font-mono text-xs text-blue-300">
                        {r.findingId.slice(0, 8)}…
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-2">
                          <Icon className="h-4 w-4 text-slate-400" />
                          <Badge kind="remediation" value={r.status}>{r.status}</Badge>
                        </div>
                      </td>
                      <td className="px-4 py-3 text-xs font-mono text-slate-400">
                        {r.developerId ? r.developerId.slice(0, 8) + "…" : "—"}
                      </td>
                      <td className="px-4 py-3 text-xs font-mono text-slate-400">
                        {r.commitRef ? r.commitRef.slice(0, 12) : "—"}
                      </td>
                      <td className="px-4 py-3">
                        {r.reopenedCount > 0 ? (
                          <span className="text-xs text-red-400">{r.reopenedCount}×</span>
                        ) : (
                          <span className="text-xs text-slate-600">—</span>
                        )}
                      </td>
                      <td className="px-4 py-3 text-xs text-slate-500 whitespace-nowrap">
                        {r.startedAt ? formatDate(r.startedAt) : "—"}
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
