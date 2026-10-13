import { Link } from "react-router-dom";
import { Swords, ExternalLink, Activity } from "lucide-react";
import { Header } from "@/components/layout/Header";
import { Card } from "@/components/ui/Card";
import { Badge } from "@/components/ui/Badge";
import { EmptyState } from "@/components/ui/EmptyState";
import { useCampaigns } from "@/lib/queries";
import { formatRelative, formatDate } from "@/lib/utils";

export function CampaignsPage() {
  const { data: campaigns = [], isLoading } = useCampaigns();

  return (
    <>
      <Header
        title="Attack Campaigns"
        description="Multi-stage attacks reconstructed by the correlation engine"
      />
      <div className="flex-1 overflow-y-auto p-8 space-y-4">

        {isLoading ? (
          <Card className="p-12 text-center text-sm text-slate-500">Loading campaigns…</Card>
        ) : campaigns.length === 0 ? (
          <Card>
            <EmptyState
              icon={Swords}
              title="No attack campaigns detected yet"
              description="When the correlation engine groups related security events into a chain, the campaign will appear here."
            />
          </Card>
        ) : (
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
            {campaigns.map((c) => (
              <Card key={c.id} className="p-5 hover:border-slate-700 transition-colors">
                <div className="flex items-start justify-between mb-3">
                  <div className="flex items-center gap-3">
                    <div className="h-10 w-10 rounded-lg bg-red-500/15 border border-red-500/30 flex items-center justify-center">
                      <Swords className="h-5 w-5 text-red-400" />
                    </div>
                    <div>
                      <Link to={`/campaigns/${c.id}`} className="text-sm font-semibold text-white hover:underline flex items-center gap-2">
                        {c.reference}
                        <ExternalLink className="h-3 w-3 text-slate-500" />
                      </Link>
                      <div className="text-xs text-slate-500 mt-0.5">
                        {c.sourceIp ? `from ${c.sourceIp}` : "source unknown"}
                      </div>
                    </div>
                  </div>
                  <Badge kind="risk" value={c.riskBand ?? "MEDIUM"}>{c.riskBand ?? "—"}</Badge>
                </div>

                <div className="text-sm text-slate-300 mb-3">{c.title}</div>

                <div className="grid grid-cols-3 gap-2 text-xs">
                  <div className="rounded bg-slate-800/50 p-2">
                    <div className="text-slate-500">Events</div>
                    <div className="text-white font-medium mt-0.5">{c.eventCount}</div>
                  </div>
                  <div className="rounded bg-slate-800/50 p-2">
                    <div className="text-slate-500">First seen</div>
                    <div className="text-white mt-0.5">{formatRelative(c.firstSeen)}</div>
                  </div>
                  <div className="rounded bg-slate-800/50 p-2">
                    <div className="text-slate-500">Last seen</div>
                    <div className="text-white mt-0.5">{formatRelative(c.lastSeen)}</div>
                  </div>
                </div>

                <div className="mt-3 flex items-center gap-2 text-xs text-slate-500">
                  <Activity className="h-3 w-3" />
                  <span>Status: {c.status}</span>
                  <span className="text-slate-700">•</span>
                  <span>{formatDate(c.firstSeen)}</span>
                </div>
              </Card>
            ))}
          </div>
        )}
      </div>
    </>
  );
}
