import { useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import {
  Activity, RefreshCw, Search, Eye, Copy, Check, X,
  Server, ChevronLeft, ChevronRight, AlertCircle
} from "lucide-react";
import { Header } from "@/components/layout/Header";
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/Card";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import { api, extractError } from "@/lib/api";
import { formatDate } from "@/lib/utils";
import { toast } from "sonner";
import type { EventDto, ApplicationDto } from "@/lib/types";

interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export function EventsPage() {
  const qc = useQueryClient();
  const [selectedAppId, setSelectedAppId] = useState<string>("ALL");
  const [methodFilter, setMethodFilter] = useState<string>("ALL");
  const [statusFilter, setStatusFilter] = useState<string>("ALL");
  const [searchQuery, setSearchQuery] = useState<string>("");
  const [page, setPage] = useState<number>(0);
  const pageSize = 25;

  const [inspectEvent, setInspectEvent] = useState<EventDto | null>(null);
  const [copiedId, setCopiedId] = useState<string | null>(null);

  // Fetch applications for dropdown selector
  const appsQ = useQuery({
    queryKey: ["apps-all"],
    queryFn: async () => (await api.get<ApplicationDto[]>("/applications")).data,
    refetchInterval: 3000,
  });
  const apps = appsQ.data ?? [];
  const appMap = new Map(apps.map(a => [a.id, a]));

  // Fetch security events
  const eventsQ = useQuery({
    queryKey: ["events-list", selectedAppId, page, pageSize],
    queryFn: async () => {
      const url = selectedAppId !== "ALL"
        ? `/applications/${selectedAppId}/events?page=${page}&size=${pageSize}`
        : `/events?page=${page}&size=${pageSize}`;
      const res = await api.get<PageResponse<EventDto>>(url);
      return res.data;
    },
    refetchInterval: 2500,
  });

  const rawEvents = eventsQ.data?.content ?? [];
  const totalElements = eventsQ.data?.totalElements ?? 0;
  const totalPages = eventsQ.data?.totalPages ?? 1;

  // Client-side filtering for search & method/status
  const filteredEvents = rawEvents.filter((ev) => {
    if (methodFilter !== "ALL" && ev.method?.toUpperCase() !== methodFilter) {
      return false;
    }
    if (statusFilter !== "ALL") {
      const code = ev.statusCode ?? 0;
      if (statusFilter === "2xx" && (code < 200 || code >= 300)) return false;
      if (statusFilter === "4xx" && (code < 400 || code >= 500)) return false;
      if (statusFilter === "5xx" && (code < 500 || code >= 600)) return false;
    }
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase().trim();
      const pathMatch = ev.path?.toLowerCase().includes(q) ?? false;
      const ipMatch = ev.sourceIp?.toLowerCase().includes(q) ?? false;
      const userMatch = ev.userIdentifier?.toLowerCase().includes(q) ?? false;
      const idMatch = ev.id?.toLowerCase().includes(q) ?? false;
      const sourceMatch = ev.source?.toLowerCase().includes(q) ?? false;
      if (!pathMatch && !ipMatch && !userMatch && !idMatch && !sourceMatch) {
        return false;
      }
    }
    return true;
  });

  function copyText(text: string, id: string) {
    if (navigator.clipboard && window.isSecureContext) {
      navigator.clipboard.writeText(text).then(() => {
        setCopiedId(id);
        setTimeout(() => setCopiedId(null), 2000);
        toast.success("Copied to clipboard");
      }).catch(() => fallbackCopy(text, id));
    } else {
      fallbackCopy(text, id);
    }
  }

  function fallbackCopy(text: string, id: string) {
    try {
      const textarea = document.createElement("textarea");
      textarea.value = text;
      textarea.style.position = "fixed";
      textarea.style.opacity = "0";
      document.body.appendChild(textarea);
      textarea.select();
      document.execCommand("copy");
      document.body.removeChild(textarea);
      setCopiedId(id);
      setTimeout(() => setCopiedId(null), 2000);
      toast.success("Copied to clipboard");
    } catch {
      toast.error("Failed to copy");
    }
  }

  function getStatusBadge(code?: number) {
    if (!code) return <span className="text-slate-500">—</span>;
    if (code >= 200 && code < 300) {
      return <Badge className="bg-emerald-500/15 text-emerald-400 border-emerald-500/30">{code}</Badge>;
    }
    if (code >= 300 && code < 400) {
      return <Badge className="bg-blue-500/15 text-blue-400 border-blue-500/30">{code}</Badge>;
    }
    if (code >= 400 && code < 500) {
      return <Badge className="bg-amber-500/15 text-amber-400 border-amber-500/30">{code}</Badge>;
    }
    return <Badge className="bg-red-500/15 text-red-400 border-red-500/30">{code}</Badge>;
  }

  function getMethodBadge(method?: string) {
    if (!method) return null;
    const m = method.toUpperCase();
    const colorMap: Record<string, string> = {
      GET: "bg-emerald-500/15 text-emerald-300 border-emerald-500/30",
      POST: "bg-blue-500/15 text-blue-300 border-blue-500/30",
      PUT: "bg-amber-500/15 text-amber-300 border-amber-500/30",
      PATCH: "bg-purple-500/15 text-purple-300 border-purple-500/30",
      DELETE: "bg-red-500/15 text-red-300 border-red-500/30",
    };
    return (
      <Badge className={colorMap[m] ?? "bg-slate-800 text-slate-300 border-slate-700"}>
        {m}
      </Badge>
    );
  }

  return (
    <>
      <Header
        title="Security Events Telemetry"
        description="Centralized audit stream of raw and normalized application security events ingested via API keys"
      />

      <div className="flex-1 overflow-y-auto p-8 space-y-6">
        {/* Filters and Controls Bar */}
        <Card className="bg-slate-900/60 border-slate-800">
          <CardContent className="p-4 flex flex-col md:flex-row items-center gap-4 justify-between">
            <div className="flex flex-wrap items-center gap-3 w-full md:w-auto">
              {/* Application Selector */}
              <div className="flex items-center gap-2">
                <Server className="h-4 w-4 text-blue-400 shrink-0" />
                <select
                  value={selectedAppId}
                  onChange={(e) => {
                    setSelectedAppId(e.target.value);
                    setPage(0);
                  }}
                  className="h-9 px-3 rounded-md bg-slate-950 border border-slate-800 text-xs text-slate-200 focus:outline-none focus:border-blue-500"
                >
                  <option value="ALL">All Authorized Applications</option>
                  {apps.map((app) => (
                    <option key={app.id} value={app.id}>
                      {app.name} ({app.environment})
                    </option>
                  ))}
                </select>
              </div>

              {/* Method Selector */}
              <select
                value={methodFilter}
                onChange={(e) => setMethodFilter(e.target.value)}
                className="h-9 px-2 rounded-md bg-slate-950 border border-slate-800 text-xs text-slate-200 focus:outline-none focus:border-blue-500"
              >
                <option value="ALL">All Methods</option>
                <option value="GET">GET</option>
                <option value="POST">POST</option>
                <option value="PUT">PUT</option>
                <option value="DELETE">DELETE</option>
              </select>

              {/* Status Selector */}
              <select
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
                className="h-9 px-2 rounded-md bg-slate-950 border border-slate-800 text-xs text-slate-200 focus:outline-none focus:border-blue-500"
              >
                <option value="ALL">All Statuses</option>
                <option value="2xx">2xx Success</option>
                <option value="4xx">4xx Client Error</option>
                <option value="5xx">5xx Server Error</option>
              </select>

              {/* Search query */}
              <div className="relative min-w-[200px] flex-1">
                <Search className="h-3.5 w-3.5 absolute left-2.5 top-3 text-slate-500" />
                <input
                  type="text"
                  placeholder="Filter path, IP, user, ID..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="w-full h-9 pl-8 pr-3 rounded-md bg-slate-950 border border-slate-800 text-xs text-slate-200 placeholder:text-slate-500 focus:outline-none focus:border-blue-500"
                />
              </div>
            </div>

            <div className="flex items-center gap-2 self-end md:self-auto">
              <Badge className="bg-blue-500/10 text-blue-300 border-blue-500/20 font-mono text-xs">
                {totalElements} Events
              </Badge>
              <Button
                size="sm"
                variant="outline"
                onClick={() => {
                  qc.invalidateQueries({ queryKey: ["events-list"] });
                  toast.info("Refreshed security events");
                }}
                disabled={eventsQ.isFetching}
              >
                <RefreshCw className={`h-3.5 w-3.5 mr-1.5 ${eventsQ.isFetching ? "animate-spin" : ""}`} />
                Refresh
              </Button>
            </div>
          </CardContent>
        </Card>

        {/* Events Table Card */}
        <Card>
          <CardHeader>
            <CardTitle className="text-base flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Activity className="h-4 w-4 text-blue-400" />
                <span>Ingested Telemetry Stream</span>
              </div>
              {selectedAppId !== "ALL" && appMap.has(selectedAppId) && (
                <span className="text-xs font-normal text-slate-400 font-mono">
                  App: {appMap.get(selectedAppId)?.name} ({selectedAppId})
                </span>
              )}
            </CardTitle>
          </CardHeader>
          <CardContent>
            {eventsQ.isLoading ? (
              <div className="text-center py-12 text-sm text-slate-400 flex flex-col items-center gap-2">
                <RefreshCw className="h-5 w-5 animate-spin text-blue-400" />
                <span>Loading security events telemetry...</span>
              </div>
            ) : eventsQ.isError ? (
              <div className="p-4 rounded-md bg-red-950/30 border border-red-500/40 text-red-300 text-xs flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <AlertCircle className="h-4 w-4 text-red-400 shrink-0" />
                  <span>Failed to load events: {extractError(eventsQ.error)}</span>
                </div>
                <Button size="sm" variant="outline" onClick={() => eventsQ.refetch()}>
                  Retry
                </Button>
              </div>
            ) : filteredEvents.length === 0 ? (
              <EmptyState
                icon={Activity}
                title="No security events found"
                description={
                  searchQuery || methodFilter !== "ALL" || statusFilter !== "ALL"
                    ? "No events match the active search/filters. Try clearing your filters."
                    : "No security events have been ingested yet for this application context."
                }
              />
            ) : (
              <div className="space-y-4">
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-xs">
                    <thead>
                      <tr className="border-b border-slate-800 text-slate-400">
                        <th className="pb-2 font-medium">Timestamp</th>
                        <th className="pb-2 font-medium">Application</th>
                        <th className="pb-2 font-medium">Method</th>
                        <th className="pb-2 font-medium">Path</th>
                        <th className="pb-2 font-medium">Status</th>
                        <th className="pb-2 font-medium">Source IP</th>
                        <th className="pb-2 font-medium">Identity</th>
                        <th className="pb-2 font-medium">Source</th>
                        <th className="pb-2 font-medium text-right">Actions</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-800/60 text-slate-300">
                      {filteredEvents.map((ev) => {
                        const app = appMap.get(ev.applicationId);
                        return (
                          <tr key={ev.id} className="hover:bg-slate-900/40 transition-colors">
                            <td className="py-2.5 font-mono text-slate-400 whitespace-nowrap">
                              {formatDate(ev.eventTime)}
                            </td>
                            <td className="py-2.5">
                              {app ? (
                                <span className="font-medium text-white">{app.name}</span>
                              ) : (
                                <span className="font-mono text-slate-500 text-[11px] truncate max-w-[120px] inline-block">
                                  {ev.applicationId.slice(0, 8)}…
                                </span>
                              )}
                            </td>
                            <td className="py-2.5">{getMethodBadge(ev.method)}</td>
                            <td className="py-2.5 font-mono text-slate-200 max-w-[200px] truncate" title={ev.path}>
                              {ev.path ?? "—"}
                            </td>
                            <td className="py-2.5">{getStatusBadge(ev.statusCode)}</td>
                            <td className="py-2.5 font-mono text-slate-400">{ev.sourceIp ?? "—"}</td>
                            <td className="py-2.5 font-mono text-slate-400">{ev.userIdentifier ?? "—"}</td>
                            <td className="py-2.5">
                              <Badge className="bg-slate-800/80 text-slate-400 border-slate-700/60 text-[10px]">
                                {ev.source}
                              </Badge>
                            </td>
                            <td className="py-2.5 text-right whitespace-nowrap">
                              <Button
                                size="sm"
                                variant="ghost"
                                onClick={() => setInspectEvent(ev)}
                                className="h-7 px-2 text-xs text-blue-400 hover:text-blue-300 hover:bg-blue-500/10"
                              >
                                <Eye className="h-3.5 w-3.5 mr-1" />
                                Inspect
                              </Button>
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>

                {/* Pagination Controls */}
                {totalPages > 1 && (
                  <div className="flex items-center justify-between pt-2 border-t border-slate-800/80 text-xs text-slate-400">
                    <div>
                      Page {page + 1} of {totalPages} ({totalElements} total records)
                    </div>
                    <div className="flex items-center gap-1">
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => setPage((p) => Math.max(0, p - 1))}
                        disabled={page === 0}
                        className="h-8 w-8 p-0"
                      >
                        <ChevronLeft className="h-4 w-4" />
                      </Button>
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                        disabled={page >= totalPages - 1}
                        className="h-8 w-8 p-0"
                      >
                        <ChevronRight className="h-4 w-4" />
                      </Button>
                    </div>
                  </div>
                )}
              </div>
            )}
          </CardContent>
        </Card>

        {/* Event Detail Inspector Modal */}
        {inspectEvent && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
            <div className="w-full max-w-2xl bg-slate-900 border border-slate-800 rounded-xl shadow-2xl overflow-hidden flex flex-col max-h-[90vh]">
              {/* Modal Header */}
              <div className="p-4 border-b border-slate-800 flex items-center justify-between bg-slate-950/60">
                <div className="flex items-center gap-2">
                  <Activity className="h-4 w-4 text-blue-400" />
                  <span className="font-semibold text-sm text-white">Security Event Details</span>
                </div>
                <button
                  onClick={() => setInspectEvent(null)}
                  className="p-1 rounded-md text-slate-400 hover:text-white hover:bg-slate-800"
                >
                  <X className="h-4 w-4" />
                </button>
              </div>

              {/* Modal Body */}
              <div className="p-6 overflow-y-auto space-y-5">
                {/* ID & App info */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 bg-slate-950/50 p-3 rounded-lg border border-slate-800/80">
                  <div>
                    <span className="text-[11px] text-slate-400 block font-medium">Event UUID</span>
                    <div className="flex items-center gap-1.5 mt-0.5">
                      <code className="text-xs font-mono text-blue-300 truncate select-all">{inspectEvent.id}</code>
                      <button
                        onClick={() => copyText(inspectEvent.id, "event-id")}
                        className="p-1 hover:text-white text-slate-400 transition-colors"
                        title="Copy Event ID"
                      >
                        {copiedId === "event-id" ? <Check className="h-3 w-3 text-emerald-400" /> : <Copy className="h-3 w-3" />}
                      </button>
                    </div>
                  </div>
                  <div>
                    <span className="text-[11px] text-slate-400 block font-medium">Application</span>
                    <div className="flex items-center gap-1.5 mt-0.5">
                      <span className="text-xs text-white font-medium">
                        {appMap.get(inspectEvent.applicationId)?.name ?? inspectEvent.applicationId}
                      </span>
                      <button
                        onClick={() => copyText(inspectEvent.applicationId, "app-id")}
                        className="p-1 hover:text-white text-slate-400 transition-colors"
                        title="Copy Application ID"
                      >
                        {copiedId === "app-id" ? <Check className="h-3 w-3 text-emerald-400" /> : <Copy className="h-3 w-3" />}
                      </button>
                    </div>
                  </div>
                </div>

                {/* HTTP Request metadata */}
                <div>
                  <h4 className="text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
                    Request Envelope
                  </h4>
                  <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 text-xs">
                    <div className="p-2.5 rounded bg-slate-950 border border-slate-800/80">
                      <div className="text-[10px] text-slate-400 uppercase font-medium">Method</div>
                      <div className="mt-1">{getMethodBadge(inspectEvent.method)}</div>
                    </div>
                    <div className="p-2.5 rounded bg-slate-950 border border-slate-800/80">
                      <div className="text-[10px] text-slate-400 uppercase font-medium">Status Code</div>
                      <div className="mt-1">{getStatusBadge(inspectEvent.statusCode)}</div>
                    </div>
                    <div className="p-2.5 rounded bg-slate-950 border border-slate-800/80">
                      <div className="text-[10px] text-slate-400 uppercase font-medium">Source IP</div>
                      <div className="mt-1 font-mono text-slate-200">{inspectEvent.sourceIp ?? "—"}</div>
                    </div>
                    <div className="p-2.5 rounded bg-slate-950 border border-slate-800/80">
                      <div className="text-[10px] text-slate-400 uppercase font-medium">Source Type</div>
                      <div className="mt-1 font-mono text-slate-200">{inspectEvent.source}</div>
                    </div>
                  </div>
                  <div className="mt-2 p-2.5 rounded bg-slate-950 border border-slate-800/80 flex items-center justify-between">
                    <div>
                      <span className="text-[10px] text-slate-400 uppercase font-medium block">Endpoint / Path</span>
                      <code className="text-xs font-mono text-blue-300">{inspectEvent.path ?? "—"}</code>
                    </div>
                    <div>
                      <span className="text-[10px] text-slate-400 uppercase font-medium block">User Identity</span>
                      <span className="text-xs font-mono text-slate-200">{inspectEvent.userIdentifier ?? "—"}</span>
                    </div>
                  </div>
                </div>

                {/* Normalized Metadata Attributes */}
                {inspectEvent.normalized && Object.keys(inspectEvent.normalized).length > 0 && (
                  <div>
                    <h4 className="text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
                      Normalized Telemetry Attributes
                    </h4>
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-xs">
                      {Object.entries(inspectEvent.normalized).map(([k, v]) => (
                        <div key={k} className="p-2 rounded bg-slate-950 border border-slate-800/60 flex items-center justify-between">
                          <span className="text-slate-400 font-mono text-[11px] truncate">{k}</span>
                          <span className="font-mono text-amber-300 text-xs font-medium ml-2">
                            {typeof v === "object" ? JSON.stringify(v) : String(v)}
                          </span>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* Raw JSON */}
                <div>
                  <div className="flex items-center justify-between mb-1.5">
                    <h4 className="text-xs font-semibold text-slate-300 uppercase tracking-wider">
                      Complete Event Payload (JSON)
                    </h4>
                    <Button
                      size="sm"
                      variant="ghost"
                      onClick={() => copyText(JSON.stringify(inspectEvent, null, 2), "json-payload")}
                      className="h-6 text-xs text-slate-400 hover:text-white"
                    >
                      {copiedId === "json-payload" ? (
                        <>
                          <Check className="h-3 w-3 mr-1 text-emerald-400" />
                          Copied
                        </>
                      ) : (
                        <>
                          <Copy className="h-3 w-3 mr-1" />
                          Copy JSON
                        </>
                      )}
                    </Button>
                  </div>
                  <pre className="p-3 rounded-lg bg-slate-950 border border-slate-800 text-[11px] font-mono text-slate-300 overflow-x-auto max-h-48">
                    {JSON.stringify(inspectEvent, null, 2)}
                  </pre>
                </div>
              </div>

              {/* Modal Footer */}
              <div className="p-4 border-t border-slate-800 bg-slate-950/40 flex justify-end">
                <Button size="sm" variant="outline" onClick={() => setInspectEvent(null)}>
                  Close
                </Button>
              </div>
            </div>
          </div>
        )}
      </div>
    </>
  );
}
