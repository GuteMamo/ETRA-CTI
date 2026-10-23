import { useState } from "react";
import {
  KeyRound, Plus, Trash2, Copy, Check, ShieldCheck, Terminal, AlertCircle,
  Activity, RefreshCw, Eye, X, Search, ExternalLink
} from "lucide-react";
import { Link } from "react-router-dom";
import { Header } from "@/components/layout/Header";
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/Card";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import { api, extractError } from "@/lib/api";
import { useQueryClient, useQuery } from "@tanstack/react-query";
import { formatDate } from "@/lib/utils";
import { toast } from "sonner";
import type {
  ApplicationDto, ApiKeyDto, ApiKeyCreatedResponse, EventDto, ProjectDto,
  ApplicationType, Environment, Criticality
} from "@/lib/types";

export function ApplicationsPage() {
  const qc = useQueryClient();
  const [selectedAppId, setSelectedAppId] = useState<string | null>(null);
  const [newKeyName, setNewKeyName] = useState("");
  const [expiryDays, setExpiryDays] = useState("30");
  const [createdKey, setCreatedKey] = useState<ApiKeyCreatedResponse | null>(null);
  const [copied, setCopied] = useState(false);
  const [generating, setGenerating] = useState(false);
  const [eventSearch, setEventSearch] = useState("");
  const [inspectEvent, setInspectEvent] = useState<EventDto | null>(null);
  const [copiedEventId, setCopiedEventId] = useState<string | null>(null);

  // Application search filter for 60+ applications
  const [appSearch, setAppSearch] = useState("");

  // Application registration modal state
  const [showRegisterModal, setShowRegisterModal] = useState(false);
  const [registerName, setRegisterName] = useState("");
  const [registerProjectId, setRegisterProjectId] = useState("");
  const [registerType, setRegisterType] = useState<ApplicationType>("API");
  const [registerEnv, setRegisterEnv] = useState<Environment>("PRODUCTION");
  const [registerCriticality, setRegisterCriticality] = useState<Criticality>("HIGH");
  const [registerBaseUrl, setRegisterBaseUrl] = useState("");
  const [registeringApp, setRegisteringApp] = useState(false);

  // Fetch projects for registration selection
  const projectsQ = useQuery({
    queryKey: ["projects-all"],
    queryFn: async () => (await api.get<ProjectDto[]>("/projects")).data,
  });
  const projects = projectsQ.data ?? [];

  // Fetch applications
  const appsQ = useQuery({
    queryKey: ["apps-all"],
    queryFn: async () => (await api.get<ApplicationDto[]>("/applications")).data,
    refetchInterval: 3000,
  });

  const apps = appsQ.data ?? [];
  const currentApp = apps.find(a => a.id === selectedAppId) || apps[0];
  const activeAppId = currentApp?.id;

  // Fetch API keys for selected app
  const keysQ = useQuery({
    queryKey: ["api-keys", activeAppId],
    enabled: !!activeAppId,
    queryFn: async () => (await api.get<ApiKeyDto[]>(`/applications/${activeAppId}/api-keys`)).data,
    refetchInterval: 3000,
  });

  const keys = keysQ.data ?? [];

  // Fetch ingested telemetry events for selected app
  const eventsQ = useQuery({
    queryKey: ["app-events", activeAppId],
    enabled: !!activeAppId,
    queryFn: async () => (await api.get<{ content: EventDto[]; totalElements: number }>(`/applications/${activeAppId}/events?page=0&size=50`)).data,
    refetchInterval: 2000,
  });

  const appEvents = eventsQ.data?.content ?? [];

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

  async function handleRegisterApp(e: React.FormEvent) {
    e.preventDefault();
    if (!registerName.trim()) {
      toast.error("Please enter an application name");
      return;
    }
    setRegisteringApp(true);
    try {
      const targetProjectId = registerProjectId || projects[0]?.id;
      const url = targetProjectId ? `/projects/${targetProjectId}/applications` : "/applications";
      const res = await api.post<ApplicationDto>(url, {
        name: registerName.trim(),
        type: registerType,
        environment: registerEnv,
        criticality: registerCriticality,
        baseUrl: registerBaseUrl.trim() || undefined,
        projectId: targetProjectId || undefined,
      });
      toast.success(`Application "${res.data.name}" registered successfully!`);
      qc.invalidateQueries({ queryKey: ["apps-all"] });
      setSelectedAppId(res.data.id);
      setShowRegisterModal(false);
      setRegisterName("");
      setRegisterBaseUrl("");
    } catch (err) {
      toast.error(extractError(err));
    } finally {
      setRegisteringApp(false);
    }
  }

  async function handleGenerateKey(e: React.FormEvent) {
    e.preventDefault();
    if (!activeAppId) {
      toast.error("Please select an application first");
      return;
    }
    const keyName = newKeyName.trim() || `${currentApp?.name || "Application"} Ingestion Key`;
    setGenerating(true);
    try {
      const res = await api.post<ApiKeyCreatedResponse>(`/applications/${activeAppId}/api-keys`, {
        name: keyName,
        expiryDays: expiryDays ? parseInt(expiryDays) : undefined,
      });
      setCreatedKey(res.data);
      setNewKeyName("");
      qc.invalidateQueries({ queryKey: ["api-keys", activeAppId] });
      toast.success("Application API key generated successfully!");
    } catch (err) {
      toast.error(extractError(err));
    } finally {
      setGenerating(false);
    }
  }

  async function handleRevokeKey(keyId: string) {
    if (!activeAppId) return;
    if (!confirm("Are you sure you want to revoke this API key? Connected applications using it will be rejected immediately.")) {
      return;
    }
    try {
      await api.post(`/applications/${activeAppId}/api-keys/${keyId}/revoke`);
      qc.invalidateQueries({ queryKey: ["api-keys", activeAppId] });
      toast.success("API key revoked");
    } catch (err) {
      toast.error(extractError(err));
    }
  }

  function fallbackCopy(text: string) {
    try {
      const textarea = document.createElement("textarea");
      textarea.value = text;
      textarea.style.position = "fixed";
      textarea.style.opacity = "0";
      document.body.appendChild(textarea);
      textarea.select();
      document.execCommand("copy");
      document.body.removeChild(textarea);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
      toast.success("API Key copied to clipboard!");
    } catch {
      toast.error("Failed to copy automatically. Please copy the key manually.");
    }
  }

  function copyToClipboard(text: string) {
    if (navigator.clipboard && window.isSecureContext) {
      navigator.clipboard.writeText(text).then(() => {
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
        toast.success("API Key copied to clipboard!");
      }).catch(() => fallbackCopy(text));
    } else {
      fallbackCopy(text);
    }
  }

  const filteredApps = apps.filter((app) => {
    if (!appSearch.trim()) return true;
    const q = appSearch.toLowerCase().trim();
    return (
      app.name.toLowerCase().includes(q) ||
      app.id.toLowerCase().includes(q) ||
      app.type.toLowerCase().includes(q) ||
      app.environment.toLowerCase().includes(q) ||
      (app.baseUrl?.toLowerCase().includes(q) ?? false)
    );
  });

  return (
    <>
      <Header
        title="Applications & Integrations"
        description="Manage connected external applications and revocable machine-to-machine API keys"
      />
      <div className="flex-1 overflow-y-auto p-8 space-y-6">

        {/* Modal / Alert for newly created key */}
        {createdKey && (
          <div className="p-5 rounded-lg border border-amber-500/50 bg-amber-950/30 space-y-3">
            <div className="flex items-center gap-2 text-amber-400 font-semibold text-sm">
              <AlertCircle className="h-5 w-5" />
              <span>Save Your Application API Key</span>
            </div>
            <p className="text-xs text-slate-300">
              This API key will only be displayed <strong>once</strong>. Copy and store it securely in your external application's secrets manager.
            </p>
            <div className="flex items-center gap-2 bg-slate-900 border border-slate-700 rounded-md p-2">
              <code className="text-xs text-amber-300 font-mono flex-1 select-all break-all">
                {createdKey.rawApiKey}
              </code>
              <Button
                size="sm"
                variant="outline"
                onClick={() => copyToClipboard(createdKey.rawApiKey)}
                className="shrink-0"
              >
                {copied ? <Check className="h-3.5 w-3.5 text-emerald-400" /> : <Copy className="h-3.5 w-3.5" />}
                <span className="ml-1 text-xs">{copied ? "Copied" : "Copy"}</span>
              </Button>
            </div>
            <div className="flex justify-end">
              <Button size="sm" variant="ghost" onClick={() => setCreatedKey(null)}>
                Dismiss
              </Button>
            </div>
          </div>
        )}

        {/* Application selector & details */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <Card className="lg:col-span-1 flex flex-col">
            <CardHeader className="space-y-3">
              <div className="flex items-center justify-between">
                <CardTitle className="text-base flex items-center gap-2">
                  <span>Registered Apps</span>
                  <Badge className="bg-blue-500/20 text-blue-300 border-blue-500/30 font-mono text-xs">
                    {apps.length}
                  </Badge>
                </CardTitle>
                <Button
                  size="sm"
                  onClick={() => setShowRegisterModal(true)}
                  className="h-7 text-xs bg-blue-600 hover:bg-blue-500 text-white font-medium"
                >
                  <Plus className="h-3.5 w-3.5 mr-1" />
                  Register App
                </Button>
              </div>

              {/* Search across 60+ applications */}
              <div className="relative">
                <Search className="h-3.5 w-3.5 absolute left-2.5 top-2.5 text-slate-500" />
                <input
                  type="text"
                  placeholder={`Search ${apps.length} applications...`}
                  value={appSearch}
                  onChange={(e) => setAppSearch(e.target.value)}
                  className="w-full h-8 pl-8 pr-2.5 rounded bg-slate-900 border border-slate-700/80 text-xs text-slate-200 placeholder:text-slate-500 focus:outline-none focus:border-blue-500"
                />
              </div>
            </CardHeader>
            <CardContent className="space-y-2 max-h-[620px] overflow-y-auto pr-1">
              {filteredApps.length === 0 ? (
                <EmptyState
                  icon={ShieldCheck}
                  title={appSearch ? "No matching apps" : "No applications"}
                  description={appSearch ? "Try a different search query" : "Click 'Register App' above to connect your first application"}
                />
              ) : (
                filteredApps.map((app) => (
                  <button
                    key={app.id}
                    onClick={() => setSelectedAppId(app.id)}
                    className={`w-full text-left p-3 rounded-lg border transition-all ${(selectedAppId === app.id || (!selectedAppId && app.id === apps[0]?.id))
                        ? "border-blue-500/60 bg-blue-500/10"
                        : "border-slate-800 bg-slate-900/50 hover:border-slate-700"
                      }`}
                  >
                    <div className="flex items-center justify-between">
                      <span className="text-sm font-semibold text-white truncate max-w-[180px]">{app.name}</span>
                      <Badge kind="severity" value={app.criticality}>
                        {app.criticality}
                      </Badge>
                    </div>
                    <div className="mt-1 flex items-center gap-2 text-xs text-slate-400">
                      <span>{app.type}</span>
                      <span>•</span>
                      <span>{app.environment}</span>
                    </div>
                    {app.baseUrl && (
                      <div className="mt-1 text-[11px] text-slate-500 truncate font-mono">
                        {app.baseUrl}
                      </div>
                    )}
                    <div className="mt-1 text-[10px] text-slate-500/80 font-mono truncate">
                      ID: {app.id}
                    </div>
                  </button>
                ))
              )}
            </CardContent>
          </Card>

          {/* API Keys management panel */}
          <div className="lg:col-span-2 space-y-6">
            {currentApp ? (
              <>
                <Card>
                  <CardHeader>
                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                      <CardTitle className="text-base flex items-center gap-2">
                        <KeyRound className="h-4 w-4 text-blue-400" />
                        <span>Issue API Key for {currentApp.name}</span>
                      </CardTitle>
                      <div className="flex items-center gap-1.5 bg-slate-900 border border-slate-700/70 rounded px-2.5 py-1 text-xs">
                        <span className="text-slate-400 font-medium">App UUID:</span>
                        <code className="text-blue-300 font-mono text-xs select-all">{currentApp.id}</code>
                        <button
                          type="button"
                          onClick={() => copyToClipboard(currentApp.id)}
                          className="hover:text-white text-slate-400 ml-1 p-0.5 rounded transition-colors"
                          title="Copy Application UUID"
                        >
                          <Copy className="h-3.5 w-3.5" />
                        </button>
                      </div>
                    </div>
                  </CardHeader>
                  <CardContent>
                    <form onSubmit={handleGenerateKey} className="flex flex-col sm:flex-row gap-3">
                      <div className="flex-1">
                        <input
                          type="text"
                          placeholder="Key identifier (e.g. Production Ingestion Service)"
                          value={newKeyName}
                          onChange={(e) => setNewKeyName(e.target.value)}
                          className="w-full h-9 px-3 rounded-md bg-slate-900 border border-slate-700 text-sm text-slate-100 placeholder:text-slate-500 focus:outline-none focus:border-blue-500"
                        />
                      </div>
                      <div className="w-32">
                        <select
                          value={expiryDays}
                          onChange={(e) => setExpiryDays(e.target.value)}
                          className="w-full h-9 px-2 rounded-md bg-slate-900 border border-slate-700 text-sm text-slate-100 focus:outline-none focus:border-blue-500"
                        >
                          <option value="30">30 days</option>
                          <option value="90">90 days</option>
                          <option value="365">1 year</option>
                          <option value="">No expiry</option>
                        </select>
                      </div>
                      <Button type="submit" disabled={generating} className="shrink-0">
                        <Plus className="h-4 w-4 mr-1" />
                        {generating ? "Generating..." : "Generate Key"}
                      </Button>
                    </form>
                  </CardContent>
                </Card>

                {/* Existing Keys Table */}
                <Card>
                  <CardHeader>
                    <CardTitle className="text-base">Active Credentials</CardTitle>
                  </CardHeader>
                  <CardContent>
                    {keysQ.isLoading ? (
                      <div className="text-center py-6 text-sm text-slate-400">Loading credentials...</div>
                    ) : keys.length === 0 ? (
                      <EmptyState
                        icon={KeyRound}
                        title="No API keys"
                        description="Generate an API key to allow this application to submit security events via HTTPS API"
                      />
                    ) : (
                      <div className="overflow-x-auto">
                        <table className="w-full text-left text-xs">
                          <thead>
                            <tr className="border-b border-slate-800 text-slate-400">
                              <th className="pb-2 font-medium">Name</th>
                              <th className="pb-2 font-medium">Key Prefix</th>
                              <th className="pb-2 font-medium">Status</th>
                              <th className="pb-2 font-medium">Created</th>
                              <th className="pb-2 font-medium">Expires</th>
                              <th className="pb-2 font-medium text-right">Actions</th>
                            </tr>
                          </thead>
                          <tbody className="divide-y divide-slate-800/60 text-slate-300">
                            {keys.map((k) => (
                              <tr key={k.id} className="hover:bg-slate-900/30">
                                <td className="py-2.5 font-medium text-white">{k.name}</td>
                                <td className="py-2.5 font-mono text-slate-400">{k.keyPrefix}••••••</td>
                                <td className="py-2.5">
                                  <Badge
                                    className={
                                      k.status === "ACTIVE"
                                        ? "bg-emerald-500/15 text-emerald-300 border-emerald-500/30"
                                        : k.status === "REVOKED"
                                          ? "bg-red-500/15 text-red-300 border-red-500/30"
                                          : "bg-slate-500/15 text-slate-300 border-slate-500/30"
                                    }
                                  >
                                    {k.status}
                                  </Badge>
                                </td>
                                <td className="py-2.5">{formatDate(k.createdAt)}</td>
                                <td className="py-2.5">{k.expiresAt ? formatDate(k.expiresAt) : "Never"}</td>
                                <td className="py-2.5 text-right">
                                  {k.status === "ACTIVE" && (
                                    <Button
                                      size="sm"
                                      variant="ghost"
                                      onClick={() => handleRevokeKey(k.id)}
                                      className="text-red-400 hover:text-red-300 hover:bg-red-950/40"
                                    >
                                      <Trash2 className="h-3.5 w-3.5 mr-1" />
                                      Revoke
                                    </Button>
                                  )}
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    )}
                  </CardContent>
                </Card>

                {/* Integration Quickstart */}
                <Card>
                  <CardHeader>
                    <CardTitle className="text-base flex items-center gap-2">
                      <Terminal className="h-4 w-4 text-emerald-400" />
                      <span>Integration Quickstart</span>
                    </CardTitle>
                  </CardHeader>
                  <CardContent className="space-y-3">
                    <p className="text-xs text-slate-300">
                      Send authorized security events to ETRA-CTI from your external server using <code className="text-blue-400">POST /api/events</code>:
                    </p>
                    <div className="bg-slate-950 border border-slate-800 rounded-lg p-3 overflow-x-auto">
                      <pre className="text-xs font-mono text-slate-300">
                        {`curl -X POST http://localhost:8081/api/events \\
  -H "Content-Type: application/json" \\
  -H "X-ETRA-API-KEY: <your-api-key>" \\
  -d '{
    "applicationId": "${currentApp.id}",
    "source": "API_GATEWAY",
    "timestamp": "${new Date().toISOString()}",
    "sourceIp": "198.51.100.22",
    "method": "POST",
    "path": "/api/v1/auth/login",
    "statusCode": 401,
    "userId": "user_1029",
    "metadata": { "userAgent": "curl/8.0", "reason": "invalid_credentials" }
  }'`}
                      </pre>
                    </div>
                  </CardContent>
                </Card>

                {/* Ingested Telemetry / Security Events */}
                <Card>
                  <CardHeader>
                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                      <div className="flex items-center gap-2">
                        <Activity className="h-4 w-4 text-blue-400" />
                        <CardTitle className="text-base">Ingested Security Events ({appEvents.length})</CardTitle>
                      </div>
                      <div className="flex items-center gap-2">
                        <div className="relative">
                          <Search className="h-3.5 w-3.5 absolute left-2 top-2.5 text-slate-500" />
                          <input
                            type="text"
                            placeholder="Filter path, IP, user, ID..."
                            value={eventSearch}
                            onChange={(e) => setEventSearch(e.target.value)}
                            className="h-8 pl-7 pr-2.5 rounded bg-slate-900 border border-slate-700/80 text-xs text-slate-200 placeholder:text-slate-500 focus:outline-none focus:border-blue-500 w-44"
                          />
                        </div>
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => {
                            qc.invalidateQueries({ queryKey: ["app-events", activeAppId] });
                            toast.info("Refreshed events");
                          }}
                          disabled={eventsQ.isFetching}
                          className="h-8 text-xs shrink-0"
                        >
                          <RefreshCw className={`h-3.5 w-3.5 mr-1 ${eventsQ.isFetching ? "animate-spin" : ""}`} />
                          Refresh
                        </Button>
                        <Link to="/events">
                          <Button size="sm" variant="ghost" className="h-8 text-xs text-blue-400 hover:text-blue-300">
                            Full View
                            <ExternalLink className="h-3 w-3 ml-1" />
                          </Button>
                        </Link>
                      </div>
                    </div>
                  </CardHeader>
                  <CardContent>
                    {eventsQ.isLoading ? (
                      <div className="text-center py-8 text-sm text-slate-400 flex flex-col items-center gap-2">
                        <RefreshCw className="h-5 w-5 animate-spin text-blue-400" />
                        <span>Loading security events...</span>
                      </div>
                    ) : eventsQ.isError ? (
                      <div className="p-3 rounded-md bg-red-950/30 border border-red-500/40 text-red-300 text-xs flex items-center justify-between">
                        <div className="flex items-center gap-2">
                          <AlertCircle className="h-4 w-4 text-red-400 shrink-0" />
                          <span>Failed to load events: {extractError(eventsQ.error)}</span>
                        </div>
                        <Button size="sm" variant="outline" onClick={() => eventsQ.refetch()}>
                          Retry
                        </Button>
                      </div>
                    ) : appEvents.length === 0 ? (
                      <EmptyState
                        icon={Activity}
                        title="No security events ingested yet"
                        description={`Use the curl snippet above with an active API key to send security telemetry to ${currentApp.name}`}
                      />
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
                              <th className="pb-2 font-medium">Identity</th>
                              <th className="pb-2 font-medium">Telemetry Metadata</th>
                              <th className="pb-2 font-medium text-right">Actions</th>
                            </tr>
                          </thead>
                          <tbody className="divide-y divide-slate-800/60 text-slate-300">
                            {appEvents
                              .filter((ev) => {
                                if (!eventSearch.trim()) return true;
                                const q = eventSearch.toLowerCase().trim();
                                return (
                                  ev.path?.toLowerCase().includes(q) ||
                                  ev.sourceIp?.toLowerCase().includes(q) ||
                                  ev.userIdentifier?.toLowerCase().includes(q) ||
                                  ev.id?.toLowerCase().includes(q) ||
                                  ev.method?.toLowerCase().includes(q) ||
                                  JSON.stringify(ev.normalized ?? {}).toLowerCase().includes(q)
                                );
                              })
                              .map((ev) => (
                                <tr key={ev.id} className="hover:bg-slate-900/40 transition-colors">
                                  <td className="py-2.5 font-mono text-slate-400 whitespace-nowrap">
                                    {formatDate(ev.eventTime)}
                                  </td>
                                  <td className="py-2.5">{getMethodBadge(ev.method)}</td>
                                  <td className="py-2.5 font-mono text-slate-200 truncate max-w-[150px]" title={ev.path}>
                                    {ev.path ?? "—"}
                                  </td>
                                  <td className="py-2.5">{getStatusBadge(ev.statusCode)}</td>
                                  <td className="py-2.5 font-mono text-slate-400">{ev.sourceIp ?? "—"}</td>
                                  <td className="py-2.5 font-mono text-slate-400">{ev.userIdentifier ?? "—"}</td>
                                  <td className="py-2.5">
                                    {ev.normalized && Object.keys(ev.normalized).length > 0 ? (
                                      <div className="flex flex-wrap gap-1 max-w-[220px]">
                                        {Object.entries(ev.normalized)
                                          .filter(([k]) => k.startsWith("meta.") || k === "reason" || k === "failureReason")
                                          .slice(0, 2)
                                          .map(([k, v]) => (
                                            <span
                                              key={k}
                                              className="inline-block px-1.5 py-0.5 rounded text-[10px] bg-slate-800 text-amber-300/90 font-mono truncate max-w-[120px]"
                                              title={`${k}: ${String(v)}`}
                                            >
                                              {k.replace("meta.", "")}:{String(v)}
                                            </span>
                                          ))}
                                      </div>
                                    ) : (
                                      <span className="text-slate-500">—</span>
                                    )}
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
                              ))}
                          </tbody>
                        </table>
                      </div>
                    )}
                  </CardContent>
                </Card>
              </>
            ) : (
              <EmptyState icon={ShieldCheck} title="Select an application" description="Select an application from the left to manage its credentials" />
            )}
          </div>
        </div>

        {/* Event Detail Inspector Modal */}
        {inspectEvent && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
            <div className="w-full max-w-2xl bg-slate-900 border border-slate-800 rounded-xl shadow-2xl overflow-hidden flex flex-col max-h-[90vh]">
              <div className="p-4 border-b border-slate-800 flex items-center justify-between bg-slate-950/60">
                <div className="flex items-center gap-2">
                  <Activity className="h-4 w-4 text-blue-400" />
                  <span className="font-semibold text-sm text-white">Event Telemetry Inspector</span>
                </div>
                <button
                  onClick={() => setInspectEvent(null)}
                  className="p-1 rounded-md text-slate-400 hover:text-white hover:bg-slate-800"
                >
                  <X className="h-4 w-4" />
                </button>
              </div>

              <div className="p-6 overflow-y-auto space-y-5">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 bg-slate-950/50 p-3 rounded-lg border border-slate-800/80">
                  <div>
                    <span className="text-[11px] text-slate-400 block font-medium">Event UUID</span>
                    <div className="flex items-center gap-1.5 mt-0.5">
                      <code className="text-xs font-mono text-blue-300 truncate select-all">{inspectEvent.id}</code>
                      <button
                        onClick={() => {
                          copyToClipboard(inspectEvent.id);
                          setCopiedEventId(inspectEvent.id);
                          setTimeout(() => setCopiedEventId(null), 2000);
                        }}
                        className="p-1 hover:text-white text-slate-400 transition-colors"
                        title="Copy Event ID"
                      >
                        {copiedEventId === inspectEvent.id ? <Check className="h-3 w-3 text-emerald-400" /> : <Copy className="h-3 w-3" />}
                      </button>
                    </div>
                  </div>
                  <div>
                    <span className="text-[11px] text-slate-400 block font-medium">Application</span>
                    <div className="flex items-center gap-1.5 mt-0.5">
                      <span className="text-xs text-white font-medium">{currentApp?.name ?? inspectEvent.applicationId}</span>
                    </div>
                  </div>
                </div>

                <div>
                  <h4 className="text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">Request Envelope</h4>
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

                {inspectEvent.normalized && Object.keys(inspectEvent.normalized).length > 0 && (
                  <div>
                    <h4 className="text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">Normalized Attributes</h4>
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

                <div>
                  <div className="flex items-center justify-between mb-1.5">
                    <h4 className="text-xs font-semibold text-slate-300 uppercase tracking-wider">Complete Payload (JSON)</h4>
                    <Button
                      size="sm"
                      variant="ghost"
                      onClick={() => copyToClipboard(JSON.stringify(inspectEvent, null, 2))}
                      className="h-6 text-xs text-slate-400 hover:text-white"
                    >
                      <Copy className="h-3 w-3 mr-1" />
                      Copy JSON
                    </Button>
                  </div>
                  <pre className="p-3 rounded-lg bg-slate-950 border border-slate-800 text-[11px] font-mono text-slate-300 overflow-x-auto max-h-48">
                    {JSON.stringify(inspectEvent, null, 2)}
                  </pre>
                </div>
              </div>

              <div className="p-4 border-t border-slate-800 bg-slate-950/40 flex justify-end">
                <Button size="sm" variant="outline" onClick={() => setInspectEvent(null)}>
                  Close
                </Button>
              </div>
            </div>
          </div>
        )}

        {/* Register Application Modal */}
        {showRegisterModal && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
            <div className="w-full max-w-lg bg-slate-900 border border-slate-800 rounded-xl shadow-2xl overflow-hidden flex flex-col">
              <div className="p-4 border-b border-slate-800 flex items-center justify-between bg-slate-950/60">
                <div className="flex items-center gap-2">
                  <ShieldCheck className="h-4 w-4 text-blue-400" />
                  <span className="font-semibold text-sm text-white">Register New Application</span>
                </div>
                <button
                  type="button"
                  onClick={() => setShowRegisterModal(false)}
                  className="p-1 rounded-md text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
                >
                  <X className="h-4 w-4" />
                </button>
              </div>

              <form onSubmit={handleRegisterApp} className="p-6 space-y-4">
                <div>
                  <label className="text-xs font-medium text-slate-300 block mb-1.5">
                    Application Name <span className="text-red-400">*</span>
                  </label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. Core Banking API, Mobile App Gateway"
                    value={registerName}
                    onChange={(e) => setRegisterName(e.target.value)}
                    className="w-full h-9 px-3 rounded-md bg-slate-950 border border-slate-800 text-xs text-slate-100 placeholder:text-slate-500 focus:outline-none focus:border-blue-500"
                  />
                </div>

                {projects.length > 0 && (
                  <div>
                    <label className="text-xs font-medium text-slate-300 block mb-1.5">
                      Parent Project
                    </label>
                    <select
                      value={registerProjectId || projects[0]?.id}
                      onChange={(e) => setRegisterProjectId(e.target.value)}
                      className="w-full h-9 px-3 rounded-md bg-slate-950 border border-slate-800 text-xs text-slate-100 focus:outline-none focus:border-blue-500"
                    >
                      {projects.map((p) => (
                        <option key={p.id} value={p.id}>
                          {p.name}
                        </option>
                      ))}
                    </select>
                  </div>
                )}

                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="text-xs font-medium text-slate-300 block mb-1.5">
                      Application Type
                    </label>
                    <select
                      value={registerType}
                      onChange={(e) => setRegisterType(e.target.value as ApplicationType)}
                      className="w-full h-9 px-2 rounded-md bg-slate-950 border border-slate-800 text-xs text-slate-100 focus:outline-none focus:border-blue-500"
                    >
                      <option value="API">API</option>
                      <option value="WEB">Web Application</option>
                      <option value="WEB_API">Web & API</option>
                      <option value="MOBILE">Mobile App</option>
                    </select>
                  </div>

                  <div>
                    <label className="text-xs font-medium text-slate-300 block mb-1.5">
                      Environment
                    </label>
                    <select
                      value={registerEnv}
                      onChange={(e) => setRegisterEnv(e.target.value as Environment)}
                      className="w-full h-9 px-2 rounded-md bg-slate-950 border border-slate-800 text-xs text-slate-100 focus:outline-none focus:border-blue-500"
                    >
                      <option value="PRODUCTION">Production</option>
                      <option value="STAGING">Staging</option>
                      <option value="DEV">Development</option>
                    </select>
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="text-xs font-medium text-slate-300 block mb-1.5">
                      Criticality
                    </label>
                    <select
                      value={registerCriticality}
                      onChange={(e) => setRegisterCriticality(e.target.value as Criticality)}
                      className="w-full h-9 px-2 rounded-md bg-slate-950 border border-slate-800 text-xs text-slate-100 focus:outline-none focus:border-blue-500"
                    >
                      <option value="CRITICAL">Critical</option>
                      <option value="HIGH">High</option>
                      <option value="MEDIUM">Medium</option>
                      <option value="LOW">Low</option>
                    </select>
                  </div>

                  <div>
                    <label className="text-xs font-medium text-slate-300 block mb-1.5">
                      Base URL (Optional)
                    </label>
                    <input
                      type="text"
                      placeholder="https://api.bank.et"
                      value={registerBaseUrl}
                      onChange={(e) => setRegisterBaseUrl(e.target.value)}
                      className="w-full h-9 px-3 rounded-md bg-slate-950 border border-slate-800 text-xs text-slate-100 placeholder:text-slate-500 focus:outline-none focus:border-blue-500"
                    />
                  </div>
                </div>

                <div className="pt-3 border-t border-slate-800 flex items-center justify-end gap-2">
                  <Button
                    type="button"
                    variant="ghost"
                    onClick={() => setShowRegisterModal(false)}
                    disabled={registeringApp}
                  >
                    Cancel
                  </Button>
                  <Button
                    type="submit"
                    disabled={registeringApp}
                    className="bg-blue-600 hover:bg-blue-500 text-white"
                  >
                    <Plus className="h-4 w-4 mr-1.5" />
                    {registeringApp ? "Registering..." : "Register Application"}
                  </Button>
                </div>
              </form>
            </div>
          </div>
        )}

      </div>
    </>
  );
}
