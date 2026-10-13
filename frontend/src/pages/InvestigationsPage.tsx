import { Link } from "react-router-dom";
import { Search, ExternalLink, User, Activity } from "lucide-react";
import { Header } from "@/components/layout/Header";
import { Card } from "@/components/ui/Card";
import { Badge } from "@/components/ui/Badge";
import { EmptyState } from "@/components/ui/EmptyState";
import { useInvestigations } from "@/lib/queries";
import { formatRelative } from "@/lib/utils";

export function InvestigationsPage() {
  const { data: investigations = [], isLoading } = useInvestigations();

  return (
    <>
      <Header
        title="Investigations"
        description="Analyst workspace tracking security incidents"
      />
      <div className="flex-1 overflow-y-auto p-8 space-y-4">

        {isLoading ? (
          <Card className="p-12 text-center text-sm text-slate-500">Loading…</Card>
        ) : investigations.length === 0 ? (
          <Card>
            <EmptyState
              icon={Search}
              title="No investigations yet"
              description="Open an investigation from an attack campaign to start tracking the incident."
            />
          </Card>
        ) : (
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
            {investigations.map((inv) => (
              <Card key={inv.id} className="p-5 hover:border-slate-700 transition-colors">
                <div className="flex items-start justify-between mb-3">
                  <div className="flex items-center gap-3">
                    <div className="h-10 w-10 rounded-lg bg-blue-500/15 border border-blue-500/30 flex items-center justify-center">
                      <Search className="h-5 w-5 text-blue-400" />
                    </div>
                    <div>
                      <Link to={`/investigations/${inv.id}`}
                        className="text-sm font-semibold text-white hover:underline flex items-center gap-2">
                        {inv.reference}
                        <ExternalLink className="h-3 w-3 text-slate-500" />
                      </Link>
                      <div className="text-xs text-slate-500 mt-0.5 truncate max-w-xs">
                        {inv.title}
                      </div>
                    </div>
                  </div>
                  <Badge kind="status" value={inv.status}>{inv.status}</Badge>
                </div>

                <div className="grid grid-cols-2 gap-2 text-xs mt-3">
                  <div className="rounded bg-slate-800/50 p-2 flex items-center gap-2">
                    <Activity className="h-3 w-3 text-slate-500" />
                    <div>
                      <div className="text-slate-500">Priority</div>
                      <div className="text-white mt-0.5">{inv.priority}</div>
                    </div>
                  </div>
                  <div className="rounded bg-slate-800/50 p-2 flex items-center gap-2">
                    <User className="h-3 w-3 text-slate-500" />
                    <div>
                      <div className="text-slate-500">Assigned</div>
                      <div className="text-white mt-0.5 font-mono text-xs">
                        {inv.assignedTo ? inv.assignedTo.slice(0, 8) + "…" : "unassigned"}
                      </div>
                    </div>
                  </div>
                </div>

                <div className="mt-3 text-xs text-slate-500">
                  Opened {formatRelative(inv.createdAt)}
                </div>
              </Card>
            ))}
          </div>
        )}
      </div>
    </>
  );
}
