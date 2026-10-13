import { useState, useRef, useCallback } from "react";
import { Upload, FileUp, CheckCircle2, XCircle, Loader2, FileText } from "lucide-react";
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
import type { IntegrationSource } from "@/lib/types";

const TOOLS: { source: IntegrationSource; label: string; endpoint: string; accept: string; hint: string }[] = [
  { source: "BURP", label: "Burp Suite",  endpoint: "burp", accept: ".xml",  hint: "XML export from Burp" },
  { source: "ZAP",  label: "OWASP ZAP",   endpoint: "zap",  accept: ".json", hint: "JSON report from ZAP" },
  { source: "NMAP", label: "Nmap",        endpoint: "nmap", accept: ".xml",  hint: "XML scan output" },
];

export function ImportsPage() {
  const qc = useQueryClient();
  const { data: imports = [], isLoading } = useImports();
  const [uploading, setUploading] = useState<IntegrationSource | null>(null);
  const [dragActive, setDragActive] = useState<IntegrationSource | null>(null);
  const inputRefs = useRef<Record<string, HTMLInputElement | null>>({});

  const appsQ = useQuery({
    queryKey: ["apps-for-import"],
    queryFn: async () => {
      const orgs = (await api.get("/organizations")).data as { id: string }[];
      if (!orgs.length) return [];
      const projects = (await api.get(`/organizations/${orgs[0].id}/projects`)).data as { id: string }[];
      if (!projects.length) return [];
      return (await api.get(`/projects/${projects[0].id}/applications`)).data as { id: string; name: string }[];
    },
  });

  const firstApp = appsQ.data?.[0];

  async function doUpload(source: IntegrationSource, endpoint: string, file: File) {
    if (!firstApp) {
      toast.error("No application available. Create one first.");
      return;
    }
    setUploading(source);
    try {
      const form = new FormData();
      form.append("file", file);
      const res = await api.post(`/integrations/${endpoint}/${firstApp.id}`, form, {
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
  }, [firstApp]);

  return (
    <>
      <Header
        title="Import Security Tool Reports"
        description="Upload Burp, ZAP, or Nmap reports to bring external findings into ETRA-CTI"
      />
      <div className="flex-1 overflow-y-auto p-8 space-y-6">

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
                <div className="text-xs text-slate-600 mt-3">Click or drag file</div>
              </div>
            </div>
          ))}
        </div>

        {!firstApp && !appsQ.isLoading && (
          <Card className="border-amber-500/40 bg-amber-500/5 p-4">
            <div className="text-sm text-amber-200">
              No applications found. Create an application before importing tool reports.
            </div>
          </Card>
        )}

        {/* Import history */}
        <Card>
          <CardHeader><CardTitle>Import History</CardTitle></CardHeader>
          <CardContent>
            {isLoading ? (
              <div className="text-sm text-slate-500 py-4 text-center">Loading…</div>
            ) : imports.length === 0 ? (
              <EmptyState icon={Upload} title="No imports yet"
                description="Upload a Burp XML, ZAP JSON, or Nmap XML file above to import findings." />
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead className="text-xs uppercase tracking-wider text-slate-400">
                    <tr>
                      <th className="text-left py-2">Source</th>
                      <th className="text-left py-2">File</th>
                      <th className="text-left py-2">Status</th>
                      <th className="text-right py-2">Findings</th>
                      <th className="text-right py-2">Assets</th>
                      <th className="text-right py-2">Duration</th>
                      <th className="text-left py-2">When</th>
                    </tr>
                  </thead>
                  <tbody>
                    {imports.map((imp) => (
                      <tr key={imp.id} className="border-t border-slate-800">
                        <td className="py-3">
                          <Badge kind="neutral">{imp.source}</Badge>
                        </td>
                        <td className="py-3 text-slate-300 text-xs font-mono truncate max-w-[220px]">
                          {imp.filename ?? "—"}
                        </td>
                        <td className="py-3">
                          {imp.status === "COMPLETED" ? (
                            <span className="inline-flex items-center gap-1 text-emerald-400 text-xs">
                              <CheckCircle2 className="h-3 w-3" /> {imp.status}
                            </span>
                          ) : imp.status === "FAILED" ? (
                            <span className="inline-flex items-center gap-1 text-red-400 text-xs">
                              <XCircle className="h-3 w-3" /> {imp.status}
                            </span>
                          ) : (
                            <span className="inline-flex items-center gap-1 text-amber-400 text-xs">
                              <Loader2 className="h-3 w-3 animate-spin" /> {imp.status}
                            </span>
                          )}
                        </td>
                        <td className="py-3 text-right text-slate-200">{imp.findingsCreated}</td>
                        <td className="py-3 text-right text-slate-200">{imp.assetsCreated}</td>
                        <td className="py-3 text-right text-slate-400 text-xs">
                          {imp.durationMs ? `${imp.durationMs}ms` : "—"}
                        </td>
                        <td className="py-3 text-xs text-slate-500">{formatDate(imp.startedAt)}</td>
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
