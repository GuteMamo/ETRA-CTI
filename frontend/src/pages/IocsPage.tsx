import { useState } from "react";
import { Target, Plus, Trash2, Loader2 } from "lucide-react";
import { Header } from "@/components/layout/Header";
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/Card";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { EmptyState } from "@/components/ui/EmptyState";
import { api, extractError } from "@/lib/api";
import { useIocs } from "@/lib/queries";
import { useQueryClient } from "@tanstack/react-query";
import { formatDate } from "@/lib/utils";
import { toast } from "sonner";
import type { IocType, Severity } from "@/lib/types";

const IOC_TYPES: IocType[] = ["IP", "DOMAIN", "URL", "SHA256", "MD5"];
const SEVERITIES: Severity[] = ["LOW", "MEDIUM", "HIGH", "CRITICAL"];

export function IocsPage() {
  const qc = useQueryClient();
  const { data: iocs = [], isLoading } = useIocs();
  const [open, setOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [form, setForm] = useState({
    type: "IP" as IocType,
    value: "",
    description: "",
    source: "manual",
    severity: "HIGH" as Severity,
  });

  async function submit() {
    if (!form.value.trim()) return;
    setSubmitting(true);
    try {
      await api.post("/threat-intel/iocs", form);
      toast.success("IOC added");
      setOpen(false);
      setForm({ ...form, value: "", description: "" });
      qc.invalidateQueries({ queryKey: ["iocs"] });
    } catch (e) {
      toast.error(extractError(e));
    } finally {
      setSubmitting(false);
    }
  }

  async function remove(id: string) {
    try {
      await api.delete(`/threat-intel/iocs/${id}`);
      toast.success("IOC deactivated");
      qc.invalidateQueries({ queryKey: ["iocs"] });
    } catch (e) {
      toast.error(extractError(e));
    }
  }

  return (
    <>
      <Header
        title="Threat Intelligence"
        description="Indicators of Compromise matched against incoming events"
        actions={
          <Button size="sm" onClick={() => setOpen((v) => !v)}>
            <Plus className="h-4 w-4" />
            Add IOC
          </Button>
        }
      />
      <div className="flex-1 overflow-y-auto p-8 space-y-4">

        {open && (
          <Card className="border-blue-500/40 bg-blue-500/5">
            <CardHeader><CardTitle>New Indicator of Compromise</CardTitle></CardHeader>
            <CardContent>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                <div>
                  <label className="text-xs text-slate-400 mb-1 block">Type</label>
                  <select value={form.type}
                    onChange={(e) => setForm({ ...form, type: e.target.value as IocType })}
                    className="h-10 w-full rounded-md border border-slate-700 bg-slate-900 px-3 text-sm text-slate-100">
                    {IOC_TYPES.map(t => <option key={t} value={t}>{t}</option>)}
                  </select>
                </div>
                <div>
                  <label className="text-xs text-slate-400 mb-1 block">Severity</label>
                  <select value={form.severity}
                    onChange={(e) => setForm({ ...form, severity: e.target.value as Severity })}
                    className="h-10 w-full rounded-md border border-slate-700 bg-slate-900 px-3 text-sm text-slate-100">
                    {SEVERITIES.map(s => <option key={s} value={s}>{s}</option>)}
                  </select>
                </div>
                <div className="md:col-span-2">
                  <label className="text-xs text-slate-400 mb-1 block">Value</label>
                  <Input value={form.value}
                    onChange={(e) => setForm({ ...form, value: e.target.value })}
                    placeholder="e.g. 10.20.30.40" />
                </div>
                <div className="md:col-span-2">
                  <label className="text-xs text-slate-400 mb-1 block">Description</label>
                  <Input value={form.description}
                    onChange={(e) => setForm({ ...form, description: e.target.value })}
                    placeholder="Why is this indicator suspicious?" />
                </div>
                <div className="md:col-span-2">
                  <label className="text-xs text-slate-400 mb-1 block">Source</label>
                  <Input value={form.source}
                    onChange={(e) => setForm({ ...form, source: e.target.value })} />
                </div>
              </div>
              <div className="flex justify-end gap-2 mt-4">
                <Button variant="ghost" onClick={() => setOpen(false)}>Cancel</Button>
                <Button onClick={submit} disabled={submitting || !form.value.trim()}>
                  {submitting && <Loader2 className="h-4 w-4 animate-spin" />}
                  Add IOC
                </Button>
              </div>
            </CardContent>
          </Card>
        )}

        <Card>
          <CardHeader><CardTitle>Active Indicators ({iocs.filter(i => i.active).length})</CardTitle></CardHeader>
          <CardContent>
            {isLoading ? (
              <div className="text-sm text-slate-500 py-4 text-center">Loading…</div>
            ) : iocs.length === 0 ? (
              <EmptyState icon={Target} title="No IOCs yet"
                description="Add known malicious IPs, domains, or file hashes. Any incoming event matching them will be flagged as a critical detection." />
            ) : (
              <div className="space-y-2">
                {iocs.map((ioc) => (
                  <div key={ioc.id}
                    className="flex items-center justify-between p-3 rounded-md border border-slate-800 bg-slate-800/40">
                    <div className="flex items-center gap-3 min-w-0">
                      <Badge kind="neutral">{ioc.type}</Badge>
                      <span className="font-mono text-sm text-white truncate">{ioc.value}</span>
                      {ioc.description && (
                        <span className="text-xs text-slate-500 truncate hidden md:inline">
                          — {ioc.description}
                        </span>
                      )}
                    </div>
                    <div className="flex items-center gap-3 shrink-0">
                      <Badge kind="severity" value={ioc.severity}>{ioc.severity}</Badge>
                      <span className="text-xs text-slate-500 hidden md:inline">{formatDate(ioc.createdAt)}</span>
                      <button onClick={() => remove(ioc.id)}
                        className="text-slate-500 hover:text-red-400 transition-colors">
                        <Trash2 className="h-4 w-4" />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </>
  );
}
