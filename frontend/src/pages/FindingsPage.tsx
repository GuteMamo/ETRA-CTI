import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { AlertTriangle, Search, Filter } from "lucide-react";
import { Header } from "@/components/layout/Header";
import { Card } from "@/components/ui/Card";
import { Badge } from "@/components/ui/Badge";
import { Input } from "@/components/ui/Input";
import { EmptyState } from "@/components/ui/EmptyState";
import { useFindings } from "@/lib/queries";
import { formatDate } from "@/lib/utils";
import type { Severity, FindingStatus } from "@/lib/types";

const SEVERITIES: (Severity | "ALL")[] = ["ALL", "CRITICAL", "HIGH", "MEDIUM", "LOW"];
const STATUSES: (FindingStatus | "ALL")[] = [
  "ALL", "OPEN", "CONFIRMED", "ASSIGNED", "IN_PROGRESS",
  "READY_FOR_RETEST", "VERIFIED", "CLOSED", "REOPENED", "FALSE_POSITIVE"
];

export function FindingsPage() {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [severity, setSeverity] = useState<Severity | "ALL">("ALL");
  const [status, setStatus] = useState<FindingStatus | "ALL">("ALL");

  const { data, isLoading } = useFindings(page, 25);
  const all = data?.content ?? [];

  const filtered = useMemo(() => {
    return all.filter(f => {
      if (severity !== "ALL" && f.severity !== severity) return false;
      if (status !== "ALL" && f.status !== status) return false;
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
  }, [all, severity, status, search]);

  return (
    <>
      <Header
        title="Findings"
        description="All security findings across applications, sorted by risk score"
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
            <div className="flex items-center gap-2">
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
              description="Adjust the filters above or check back after new events are ingested." />
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
                    <th className="text-left px-4 py-3 font-medium">Source</th>
                    <th className="text-left px-4 py-3 font-medium">Created</th>
                  </tr>
                </thead>
                <tbody>
                  {filtered.map(f => (
                    <tr key={f.id} className="border-t border-slate-800 hover:bg-slate-800/40 transition-colors">
                      <td className="px-4 py-3 font-mono text-xs text-blue-300">
                        <Link to={`/findings/${f.id}`} className="hover:underline">{f.reference}</Link>
                      </td>
                      <td className="px-4 py-3 text-slate-100 max-w-md truncate">{f.title}</td>
                      <td className="px-4 py-3 text-slate-400 font-mono text-xs max-w-[220px] truncate">
                        {f.endpoint ?? "—"}{f.parameter ? `?${f.parameter}` : ""}
                      </td>
                      <td className="px-4 py-3">
                        <Badge kind="severity" value={f.severity}>{f.severity}</Badge>
                      </td>
                      <td className="px-4 py-3">
                        <span className="text-slate-300 font-medium">
                          {f.riskScore?.toFixed(1) ?? "—"}
                        </span>
                        <span className="text-xs text-slate-500 ml-2">({f.riskBand ?? "—"})</span>
                      </td>
                      <td className="px-4 py-3">
                        <Badge kind="status" value={f.status}>{f.status}</Badge>
                      </td>
                      <td className="px-4 py-3 text-xs text-slate-500">{f.source ?? "—"}</td>
                      <td className="px-4 py-3 text-xs text-slate-500 whitespace-nowrap">
                        {formatDate(f.createdAt)}
                      </td>
                    </tr>
                  ))}
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
    </>
  );
}
