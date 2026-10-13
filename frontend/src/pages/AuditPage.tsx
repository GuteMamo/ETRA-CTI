import { Shield } from "lucide-react";
import { Header } from "@/components/layout/Header";
import { Card } from "@/components/ui/Card";
import { EmptyState } from "@/components/ui/EmptyState";
import { useAuditLogs } from "@/lib/queries";
import { formatDate } from "@/lib/utils";

export function AuditPage() {
  const { data, isLoading } = useAuditLogs(0, 100);
  const logs = data?.content ?? [];

  return (
    <>
      <Header
        title="Audit Log"
        description="Immutable record of every state-changing action in the platform"
      />
      <div className="flex-1 overflow-y-auto p-8">
        <Card className="overflow-hidden">
          {isLoading ? (
            <div className="p-12 text-center text-sm text-slate-500">Loading…</div>
          ) : logs.length === 0 ? (
            <EmptyState icon={Shield} title="No audit events yet"
              description="Actions like finding creation, remediation, imports, and IOC changes will be recorded here." />
          ) : (
            <table className="w-full text-sm">
              <thead className="bg-slate-900/80 text-slate-400 text-xs uppercase tracking-wider">
                <tr>
                  <th className="text-left px-4 py-3">Action</th>
                  <th className="text-left px-4 py-3">Resource</th>
                  <th className="text-left px-4 py-3">Actor</th>
                  <th className="text-left px-4 py-3">Details</th>
                  <th className="text-left px-4 py-3">When</th>
                </tr>
              </thead>
              <tbody>
                {logs.map((log) => (
                  <tr key={log.id} className="border-t border-slate-800 hover:bg-slate-800/40">
                    <td className="px-4 py-3">
                      <span className="font-mono text-xs text-blue-300">{log.action}</span>
                    </td>
                    <td className="px-4 py-3 text-xs text-slate-400">
                      {log.resourceType}
                      <span className="text-slate-600 ml-1">
                        {log.resourceId?.slice(0, 8)}…
                      </span>
                    </td>
                    <td className="px-4 py-3 text-xs font-mono text-slate-400">
                      {log.userId?.slice(0, 8) ?? "system"}…
                    </td>
                    <td className="px-4 py-3 text-xs text-slate-500 max-w-md truncate font-mono">
                      {log.newValue ? JSON.stringify(log.newValue).slice(0, 80) : "—"}
                    </td>
                    <td className="px-4 py-3 text-xs text-slate-500 whitespace-nowrap">
                      {formatDate(log.createdAt)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </Card>
      </div>
    </>
  );
}
