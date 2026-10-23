import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import {
  AlertTriangle, Swords, Search, Shield, TrendingUp, Activity, ExternalLink
} from "lucide-react";
import {
  BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer,
  PieChart, Pie, Cell, Legend
} from "recharts";
import { Header } from "@/components/layout/Header";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Badge } from "@/components/ui/Badge";
import { StatCard } from "@/components/ui/StatCard";
import { EmptyState } from "@/components/ui/EmptyState";
import { api } from "@/lib/api";
import { useCampaigns, useFindings, useInvestigations } from "@/lib/queries";
import { formatRelative } from "@/lib/utils";
import type { EventDto } from "@/lib/types";

const SEVERITY_COLORS: Record<string, string> = {
  LOW: "#3b82f6",
  MEDIUM: "#eab308",
  HIGH: "#f97316",
  CRITICAL: "#ef4444",
};

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

export function DashboardPage() {
  const findingsQ = useFindings(0, 100);
  const campaignsQ = useCampaigns();
  const investigationsQ = useInvestigations();

  const appsQ = useQuery({
    queryKey: ["apps-all"],
    queryFn: async () => (await api.get<unknown[]>("/applications")).data,
    refetchInterval: 3000,
  });

  const eventsQ = useQuery({
    queryKey: ["dashboard-recent-events"],
    queryFn: async () => (await api.get<{ content: EventDto[]; totalElements: number }>("/events?page=0&size=6")).data,
    refetchInterval: 2500,
  });

  const findings = findingsQ.data?.content ?? [];
  const campaigns = campaignsQ.data ?? [];
  const investigations = investigationsQ.data ?? [];
  const apps = appsQ.data ?? [];
  const recentEvents = eventsQ.data?.content ?? [];
  const totalEvents = eventsQ.data?.totalElements ?? 0;

  const criticalCount = findings.filter(f => f.severity === "CRITICAL").length;
  const highCount = findings.filter(f => f.severity === "HIGH").length;
  const openCount = findings.filter(f => f.status === "OPEN" || f.status === "ASSIGNED" || f.status === "IN_PROGRESS").length;

  const severityData = ["LOW", "MEDIUM", "HIGH", "CRITICAL"].map(sev => ({
    severity: sev,
    count: findings.filter(f => f.severity === sev).length,
  }));

  const riskBandData = ["LOW", "MEDIUM", "HIGH", "CRITICAL"].map(band => ({
    name: band,
    value: findings.filter(f => f.riskBand === band).length,
  })).filter(d => d.value > 0);

  const topFindings = [...findings]
    .sort((a, b) => (b.riskScore ?? 0) - (a.riskScore ?? 0))
    .slice(0, 6);

  return (
    <>
      <Header
        title="Security Dashboard"
        description="Real-time view of application exposure, attacks, and remediation"
      />
      <div className="flex-1 overflow-y-auto p-8 space-y-6">

        {/* Top stat cards */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          <StatCard label="Applications" value={apps.length} icon={Shield} tone="blue" />
          <StatCard label="Findings" value={findings.length} icon={AlertTriangle} tone="orange"
            hint={`${openCount} open`} />
          <StatCard label="Critical" value={criticalCount} icon={TrendingUp} tone="red"
            hint={`${highCount} high severity`} />
          <StatCard label="Attack Campaigns" value={campaigns.length} icon={Swords} tone="red" />
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <StatCard label="Security Events" value={totalEvents} icon={Activity} tone="emerald"
            hint="Telemetry ingested" />
          <StatCard label="Investigations" value={investigations.length} icon={Search} tone="blue" />
          <StatCard label="Active Threats" value={campaigns.filter(c => c.status === "OPEN").length}
            icon={Swords} tone="orange" />
          <StatCard label="Mean Risk Score"
            value={
              findings.length
                ? (findings.reduce((s, f) => s + (f.riskScore ?? 0), 0) / findings.length).toFixed(1)
                : "0.0"
            }
            icon={TrendingUp} tone="emerald" />
        </div>

        {/* Charts */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
          <Card className="lg:col-span-2">
            <CardHeader>
              <CardTitle>Findings by Severity</CardTitle>
            </CardHeader>
            <CardContent>
              {findings.length === 0 ? (
                <EmptyState icon={AlertTriangle} title="No findings yet" />
              ) : (
                <ResponsiveContainer width="100%" height={260}>
                  <BarChart data={severityData}>
                    <XAxis dataKey="severity" stroke="#64748b" fontSize={12} />
                    <YAxis stroke="#64748b" fontSize={12} allowDecimals={false} />
                    <Tooltip
                      contentStyle={{
                        backgroundColor: "#0f172a",
                        border: "1px solid #1e293b",
                        borderRadius: 8,
                        color: "#e2e8f0",
                      }}
                    />
                    <Bar dataKey="count" radius={[6, 6, 0, 0]}>
                      {severityData.map((entry) => (
                        <Cell key={entry.severity} fill={SEVERITY_COLORS[entry.severity]} />
                      ))}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Risk Distribution</CardTitle>
            </CardHeader>
            <CardContent>
              {riskBandData.length === 0 ? (
                <EmptyState icon={TrendingUp} title="No risk data" />
              ) : (
                <ResponsiveContainer width="100%" height={260}>
                  <PieChart>
                    <Pie
                      data={riskBandData}
                      dataKey="value"
                      nameKey="name"
                      innerRadius={60}
                      outerRadius={90}
                      paddingAngle={4}
                    >
                      {riskBandData.map((entry) => (
                        <Cell key={entry.name} fill={SEVERITY_COLORS[entry.name]} />
                      ))}
                    </Pie>
                    <Tooltip
                      contentStyle={{
                        backgroundColor: "#0f172a",
                        border: "1px solid #1e293b",
                        borderRadius: 8,
                        color: "#e2e8f0",
                      }}
                    />
                    <Legend wrapperStyle={{ fontSize: 12, color: "#94a3b8" }} />
                  </PieChart>
                </ResponsiveContainer>
              )}
            </CardContent>
          </Card>
        </div>

        {/* Recent attack campaigns + top findings */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Swords className="h-4 w-4 text-orange-400" />
                Recent Attack Campaigns
              </CardTitle>
            </CardHeader>
            <CardContent>
              {campaigns.length === 0 ? (
                <EmptyState icon={Swords} title="No attack campaigns"
                  description="Multi-stage attacks will appear here once detected by the correlation engine." />
              ) : (
                <div className="space-y-3">
                  {campaigns.slice(0, 5).map((c) => (
                    <div key={c.id} className="flex items-center justify-between p-3 rounded-md bg-slate-800/40 border border-slate-800">
                      <div className="min-w-0">
                        <div className="flex items-center gap-2">
                          <span className="text-sm font-semibold text-white">{c.reference}</span>
                          <Badge kind="risk" value={c.riskBand ?? "MEDIUM"}>{c.riskBand}</Badge>
                        </div>
                        <div className="text-xs text-slate-400 mt-0.5 truncate">
                          {c.title} — {c.eventCount} events
                        </div>
                      </div>
                      <div className="text-xs text-slate-500 shrink-0">
                        {formatRelative(c.lastSeen)}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <AlertTriangle className="h-4 w-4 text-orange-400" />
                Top Risk Findings
              </CardTitle>
            </CardHeader>
            <CardContent>
              {topFindings.length === 0 ? (
                <EmptyState icon={AlertTriangle} title="No findings yet" />
              ) : (
                <div className="space-y-3">
                  {topFindings.map((f) => (
                    <div key={f.id} className="flex items-center justify-between p-3 rounded-md bg-slate-800/40 border border-slate-800">
                      <div className="min-w-0">
                        <div className="flex items-center gap-2">
                          <span className="text-xs font-mono text-slate-500">{f.reference}</span>
                          <Badge kind="severity" value={f.severity}>{f.severity}</Badge>
                        </div>
                        <div className="text-sm text-white mt-0.5 truncate">{f.title}</div>
                      </div>
                      <div className="text-right shrink-0">
                        <div className="text-xs text-slate-400">risk</div>
                        <div className="text-sm font-semibold text-orange-300">
                          {f.riskScore?.toFixed(1) ?? "—"}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>
        </div>

        {/* Recent Ingested Telemetry Events */}
        <Card>
          <CardHeader>
            <div className="flex items-center justify-between">
              <CardTitle className="flex items-center gap-2">
                <Activity className="h-4 w-4 text-emerald-400" />
                <span>Recent Ingested Security Telemetry</span>
                <Badge className="bg-emerald-500/15 text-emerald-400 border-emerald-500/30 font-mono text-xs ml-2">
                  {totalEvents} events
                </Badge>
              </CardTitle>
              <Link to="/events" className="text-xs text-blue-400 hover:text-blue-300 flex items-center gap-1 font-medium transition-colors">
                <span>View All Ingested Events</span>
                <ExternalLink className="h-3 w-3" />
              </Link>
            </div>
          </CardHeader>
          <CardContent>
            {recentEvents.length === 0 ? (
              <EmptyState icon={Activity} title="No security events yet" description="Machine-to-machine telemetry events will appear here once ingested via API." />
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead>
                    <tr className="border-b border-slate-800 text-slate-400">
                      <th className="pb-2 font-medium">Time</th>
                      <th className="pb-2 font-medium">Method</th>
                      <th className="pb-2 font-medium">Path</th>
                      <th className="pb-2 font-medium">Status</th>
                      <th className="pb-2 font-medium">Source IP</th>
                      <th className="pb-2 font-medium">Identity / Client</th>
                      <th className="pb-2 font-medium text-right">Event UUID</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800/60 text-slate-300">
                    {recentEvents.map((ev) => (
                      <tr key={ev.id} className="hover:bg-slate-900/40 transition-colors">
                        <td className="py-2.5 font-mono text-slate-400 whitespace-nowrap">
                          {formatRelative(ev.eventTime)}
                        </td>
                        <td className="py-2.5">{getMethodBadge(ev.method)}</td>
                        <td className="py-2.5 font-mono text-slate-200 truncate max-w-[200px]" title={ev.path}>
                          {ev.path ?? "—"}
                        </td>
                        <td className="py-2.5">{getStatusBadge(ev.statusCode)}</td>
                        <td className="py-2.5 font-mono text-slate-400">{ev.sourceIp ?? "—"}</td>
                        <td className="py-2.5 font-mono text-slate-400">{ev.userIdentifier ?? "—"}</td>
                        <td className="py-2.5 text-right font-mono text-[11px] text-blue-400 truncate max-w-[140px]" title={ev.id}>
                          <Link to="/events" className="hover:underline">
                            {ev.id.substring(0, 8)}…
                          </Link>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </>
  );
}
