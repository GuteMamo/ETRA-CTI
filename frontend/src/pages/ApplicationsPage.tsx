import { useState } from "react";
import { KeyRound, Plus, Trash2, Copy, Check, ShieldCheck, Terminal, AlertCircle } from "lucide-react";
import { Header } from "@/components/layout/Header";
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/Card";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import { api, extractError } from "@/lib/api";
import { useQueryClient, useQuery } from "@tanstack/react-query";
import { formatDate } from "@/lib/utils";
import { toast } from "sonner";
import type { ApplicationDto, ApiKeyDto, ApiKeyCreatedResponse } from "@/lib/types";

export function ApplicationsPage() {
  const qc = useQueryClient();
  const [selectedAppId, setSelectedAppId] = useState<string | null>(null);
  const [newKeyName, setNewKeyName] = useState("");
  const [expiryDays, setExpiryDays] = useState("30");
  const [createdKey, setCreatedKey] = useState<ApiKeyCreatedResponse | null>(null);
  const [copied, setCopied] = useState(false);
  const [generating, setGenerating] = useState(false);

  // Fetch applications
  const appsQ = useQuery({
    queryKey: ["apps-all"],
    queryFn: async () => {
      const orgs = (await api.get("/organizations")).data as { id: string }[];
      if (!orgs.length) return [];
      const projects = (await api.get(`/organizations/${orgs[0].id}/projects`)).data as { id: string }[];
      if (!projects.length) return [];
      return (await api.get<ApplicationDto[]>(`/projects/${projects[0].id}/applications`)).data;
    },
  });

  const apps = appsQ.data ?? [];
  const currentApp = apps.find(a => a.id === selectedAppId) || apps[0];
  const activeAppId = currentApp?.id;

  // Fetch API keys for selected app
  const keysQ = useQuery({
    queryKey: ["api-keys", activeAppId],
    enabled: !!activeAppId,
    queryFn: async () => (await api.get<ApiKeyDto[]>(`/applications/${activeAppId}/api-keys`)).data,
  });

  const keys = keysQ.data ?? [];

  async function handleGenerateKey(e: React.FormEvent) {
    e.preventDefault();
    if (!activeAppId) return;
    if (!newKeyName.trim()) {
      toast.error("Please enter a name for the API key");
      return;
    }
    setGenerating(true);
    try {
      const res = await api.post<ApiKeyCreatedResponse>(`/applications/${activeAppId}/api-keys`, {
        name: newKeyName.trim(),
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

  function copyToClipboard(text: string) {
    navigator.clipboard.writeText(text);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
    toast.success("API Key copied to clipboard!");
  }

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
          <Card className="lg:col-span-1">
            <CardHeader>
              <CardTitle className="text-base flex items-center justify-between">
                <span>Registered Applications</span>
                <Badge className="bg-blue-500/20 text-blue-300 border-blue-500/30">{apps.length}</Badge>
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-2">
              {apps.length === 0 ? (
                <EmptyState icon={ShieldCheck} title="No applications" description="Register an application via project settings" />
              ) : (
                apps.map((app) => (
                  <button
                    key={app.id}
                    onClick={() => setSelectedAppId(app.id)}
                    className={`w-full text-left p-3 rounded-lg border transition-all ${
                      (selectedAppId === app.id || (!selectedAppId && app.id === apps[0]?.id))
                        ? "border-blue-500/60 bg-blue-500/10"
                        : "border-slate-800 bg-slate-900/50 hover:border-slate-700"
                    }`}
                  >
                    <div className="flex items-center justify-between">
                      <span className="text-sm font-semibold text-white">{app.name}</span>
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
                    <CardTitle className="text-base flex items-center gap-2">
                      <KeyRound className="h-4 w-4 text-blue-400" />
                      <span>Issue API Key for {currentApp.name}</span>
                    </CardTitle>
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
              </>
            ) : (
              <EmptyState icon={ShieldCheck} title="Select an application" description="Select an application from the left to manage its credentials" />
            )}
          </div>
        </div>

      </div>
    </>
  );
}
