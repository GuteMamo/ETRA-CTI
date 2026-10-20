import { useState, useRef, useCallback } from "react";
import { Upload, FileUp, CheckCircle2, XCircle, Loader2, FileText, AppWindow } from "lucide-react";
import { Header } from "@/components/layout/Header";
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/Card";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import { api, extractError } from "@/lib/api";
import { useImports } from "@/lib/queries";
import { useQueryClient, useQuery } from "@tanstack/react-query";
import { formatDate } from "@/lib/utils";
import { toast } from "sonner";
import type { IntegrationSource, ApplicationDto } from "@/lib/types";

const TOOLS: { source: IntegrationSource; label: string; endpoint: string; accept: string; hint: string }[] = [
  { source: "ZAP",  label: "OWASP ZAP",   endpoint: "zap",  accept: ".json", hint: "JSON report from OWASP ZAP (recommended)" },
  { source: "BURP", label: "Burp Suite",  endpoint: "burp", accept: ".xml",  hint: "XML scan export from Burp Suite" },
  { source: "NMAP", label: "Nmap",        endpoint: "nmap", accept: ".xml",  hint: "XML scan output from Nmap" },
];

export function ImportsPage() {
  const qc = useQueryClient();
  const { data: imports = [], isLoading } = useImports();
  const [uploading, setUploading] = useState<IntegrationSource | null>(null);
  const [dragActive, setDragActive] = useState<IntegrationSource | null>(null);
  const [selectedAppId, setSelectedAppId] = useState<string>("");
  const inputRefs = useRef<Record<string, HTMLInputElement | null>>({});

  const appsQ = useQuery({
    queryKey: ["applications-all"],
    queryFn: async () => (await api.get<ApplicationDto[]>("/applications")).data,
  });

  const apps = appsQ.data ?? [];
  const currentAppId = selectedAppId || (apps.length > 0 ? apps[0].id : "");

  async function doUpload(source: IntegrationSource, endpoint: string, file: File) {
    if (!currentAppId) {
      toast.error("Please select a target application for the import.");
      return;
    }
    setUploading(source);
    try {
      const form = new FormData();
      form.append("file", file);
      const res = await api.post(`/integrations/${endpoint}/${currentAppId}`, form, {
        headers: { "Content-Type": "multipart/form-data" },
      });
      const data = res.data as { findingsCreated: number; assetsCreated: number; findingsDuplicated: number };
      toast.success(
        `Imported ${data.findingsCreated} findings, ${data.assetsCreated} assets ` +
        `(${data.findingsDuplicated} duplicates skipped)`
      );
      qc.invalidateQueries({ queryKey: ["imports"] });
      qc.invalidateQueries({ queryKey: ["findings"] });
    } catch (e) {
      toast.error(extractError(e));
    } finally {
      setUploading(null);
    }
  }

  const onDrop = useCallback((source: IntegrationSource, endpoint: string, e: React.DragEvent) => {
    e.preventDefault();
    setDragActive(null);
    const file = e.dataTransfer.files?.[0];
    if (file) doUpload(source, endpoint, file);
  }, [currentAppId]);

  return (
    <>
      <Header
        title="Import Security Tool Reports"
        description="Upload OWASP ZAP (JSON), Burp Suite (XML), or Nmap (XML) to ingest scanner findings"
      />
      <div className="flex-1 overflow-y-auto p-8 space-y-6">

        {/* Application Selector */}
        <Card className="p-4 border-slate-800 bg-slate-900/60">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div className="flex items-center gap-2">
              <AppWindow className="h-5 w-5 text-blue-400" />
              <div>
                <div className="text-sm font-semibold text-white">Target Application Scope</div>
                <div className="text-xs text-slate-400">Scanner findings will be linked to the selected application</div>
              </div>
            </div>
            <div className="min-w-[280px]">
              <select
                value={currentAppId}
                onChange={(e) => setSelectedAppId(e.target.value)}
                className="w-full h-10 rounded-md border border-slate-700 bg-slate-950 px-3 text-sm text-slate-100"
              >
                {apps.map(app => (
                  <option key={app.id} value={app.id}>
                    {app.name} ({app.type} - {app.environment})
                  </option>
                ))}
              </select>
            </div>
          </div>
        </Card>

        {apps.length === 0 && !appsQ.isLoading && (
          <Card className="border-amber-500/40 bg-amber-500/5 p-4">
            <div className="text-sm text-amber-200">
              No applications available. Please create or request access to an application before importing tool reports.
            </div>
          </Card>
        )}

        {/* Drop zones */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {TOOLS.map(({ source, label, endpoint, accept, hint }) => (
            <div
              key={source}
              onDragOver={(e) => { e.preventDefault(); setDragActive(source); }}
              onDragLeave={() => setDragActive(null)}
              onDrop={(e) => onDrop(source, endpoint, e)}
              onClick={() => inputRefs.current[source]?.click()}
              className={`cursor-pointer rounded-lg border-2 border-dashed p-6 transition-colors ${
                dragActive === source
                  ? "border-blue-500 bg-blue-500/10"
                  : "border-slate-700 bg-slate-900/40 hover:border-slate-600"
              }`}
            >
              <input
                ref={(el) => { inputRefs.current[source] = el; }}
                type="file"
                accept={accept}
                hidden
                onChange={(e) => {
                  const file = e.target.files?.[0];
                  if (file) doUpload(source, endpoint, file);
                  e.target.value = "";
                }}
              />
              <div className="flex flex-col items-center text-center">
                {uploading === source ? (
                  <Loader2 className="h-8 w-8 text-blue-400 animate-spin mb-3" />
                ) : (
                  <FileUp className="h-8 w-8 text-slate-400 mb-3" />
                )}
                <div className="text-sm font-semibold text-white">{label}</div>
                <div className="text-xs text-slate-500 mt-1">{hint}</div>
                <div className="text-xs text-slate-600 mt-3">Click or drag report file</div>
              </div>
            </div>
          ))}
        </div>

        {/* Import history */}
        <Card>
          <CardHeader><CardTitle>Import Provenance & History</CardTitle></CardHeader>
          <CardContent>
            {isLoading ? (
              <div className="text-sm text-slate-500 py-4 text-center">Loading…</div>
            ) : imports.length === 0 ? (
              <EmptyState icon={Upload} title="No scanner reports imported yet"
                description="Upload an OWASP ZAP JSON, Burp XML, or Nmap XML report to populate findings." />
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead className="bg-slate-900/80 text-slate-400 text-xs uppercase tracking-wider">
                    <tr>
                      <th className="text-left px-4 py-3">Source Tool</th>
                      <th className="text-left px-4 py-3">Report File</th>
                      <th className="text-left px-4 py-3">Findings Created</th>
                      <th className="text-left px-4 py-3">Duplicates Skipped</th>
                      <th className="text-left px-4 py-3">Assets Created</th>
                      <th className="text-left px-4 py-3">Status</th>
                      <th className="text-left px-4 py-3">Timestamp</th>
                    </tr>
                  </thead>
                  <tbody>
                    {imports.map((imp) => (
                      <tr key={imp.id} className="border-t border-slate-800 hover:bg-slate-800/40">
                        <td className="px-4 py-3 text-white font-medium">{imp.source}</td>
                        <td className="px-4 py-3 text-slate-400 font-mono text-xs max-w-[200px] truncate">
                          {imp.filename ?? "—"}
                        </td>
                        <td className="px-4 py-3 text-emerald-400 font-medium">+{imp.findingsCreated}</td>
                        <td className="px-4 py-3 text-slate-500">{imp.findingsDuplicated}</td>
                        <td className="px-4 py-3 text-blue-400">+{imp.assetsCreated}</td>
                        <td className="px-4 py-3">
                          <Badge kind={imp.status === "COMPLETED" ? "info" : imp.status === "FAILED" ? "danger" : "neutral"}>
                            {imp.status}
                          </Badge>
                        </td>
                        <td className="px-4 py-3 text-xs text-slate-500 whitespace-nowrap">
                          {formatDate(imp.startedAt)}
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
