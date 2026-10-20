import { useParams, Link } from "react-router-dom";
import {
  ArrowLeft, Shield, Clock, Globe, Crosshair,
  ChevronRight, AlertTriangle
} from "lucide-react";
import { Header } from "@/components/layout/Header";
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/Card";
import { Badge } from "@/components/ui/Badge";
import { EmptyState } from "@/components/ui/EmptyState";
import { useCampaign, useCampaignTimeline } from "@/lib/queries";
import { formatDate } from "@/lib/utils";
import type { AttackStage } from "@/lib/types";

const STAGE_META: Record<AttackStage, { label: string; color: string; bg: string; border: string; icon: string }> = {
  RECON:         { label: "Reconnaissance",    color: "text-slate-300",   bg: "bg-slate-500/15",   border: "border-slate-500/40",   icon: "🔍" },
  AUTH_ABUSE:    { label: "Authentication Abuse", color: "text-amber-300",  bg: "bg-amber-500/15",   border: "border-amber-500/40",   icon: "🔑" },
  EXPLOITATION:  { label: "Exploitation",      color: "text-orange-300",  bg: "bg-orange-500/15",  border: "border-orange-500/40",  icon: "💥" },
  DATA_ACCESS:   { label: "Data Access",       color: "text-red-300",     bg: "bg-red-500/15",     border: "border-red-500/40",     icon: "📤" },
  IMPACT:        { label: "Impact",            color: "text-red-400",     bg: "bg-red-600/20",     border: "border-red-600/50",     icon: "⚠️" },
};

export function CampaignDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { data: campaign, isLoading } = useCampaign(id);
  const { data: timeline = [] } = useCampaignTimeline(id);

  if (isLoading) {
    return (
      <>
        <Header title="Campaign" description="Loading…" />
        <div className="p-8 text-slate-500">Loading campaign…</div>
      </>
    );
  }

  if (!campaign) {
    return (
      <>
        <Header title="Campaign not found" />
        <div className="p-8">
          <Link to="/campaigns" className="text-blue-400 hover:underline flex items-center gap-1">
            <ArrowLeft className="h-4 w-4" /> Back to campaigns
          </Link>
        </div>
      </>
    );
  }

  const duration = (() => {
    const ms = new Date(campaign.lastSeen).getTime() - new Date(campaign.firstSeen).getTime();
    const s = Math.round(ms / 1000);
    if (s < 60) return `${s} seconds`;
    const m = Math.floor(s / 60);
    if (m < 60) return `${m} min ${s % 60}s`;
    const h = Math.floor(m / 60);
    return `${h}h ${m % 60}m`;
  })();

  const stagesPresent: AttackStage[] = Array.from(new Set(timeline.map(t => t.stage)));

  return (
    <>
      <Header
        title={`${campaign.reference} — ${campaign.title}`}
        description={`Campaign detected ${formatDate(campaign.firstSeen)}`}
      />
      <div className="flex-1 overflow-y-auto p-8 space-y-6">

        <Link to="/campaigns" className="text-xs text-blue-400 hover:underline inline-flex items-center gap-1">
          <ArrowLeft className="h-3 w-3" /> Back to campaigns
        </Link>

        {/* Summary row */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <Card className="p-5">
            <div className="flex items-center gap-2 text-xs text-slate-500 uppercase tracking-wider">
              <Crosshair className="h-3 w-3" /> Risk Band
            </div>
            <div className="mt-2">
              <Badge kind="risk" value={campaign.riskBand ?? "MEDIUM"} className="text-base py-1 px-3">
                {campaign.riskBand ?? "—"}
              </Badge>
            </div>
          </Card>
          <Card className="p-5">
            <div className="flex items-center gap-2 text-xs text-slate-500 uppercase tracking-wider">
              <Globe className="h-3 w-3" /> Source IP
            </div>
            <div className="mt-2 font-mono text-white text-lg">{campaign.sourceIp ?? "—"}</div>
          </Card>
          <Card className="p-5">
            <div className="flex items-center gap-2 text-xs text-slate-500 uppercase tracking-wider">
              <Shield className="h-3 w-3" /> Events
            </div>
            <div className="mt-2 text-white text-lg font-medium">{campaign.eventCount}</div>
          </Card>
          <Card className="p-5">
            <div className="flex items-center gap-2 text-xs text-slate-500 uppercase tracking-wider">
              <Clock className="h-3 w-3" /> Duration
            </div>
            <div className="mt-2 text-white text-lg font-medium">{duration}</div>
          </Card>
        </div>

        {/* Stage progression */}
        <Card>
          <CardHeader>
            <CardTitle>Attack Chain Progression</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="flex items-center gap-2 flex-wrap">
              {stagesPresent.map((stage, idx) => {
                const meta = STAGE_META[stage as AttackStage];
                return (
                  <div key={String(stage)} className="flex items-center gap-2">
                    <div className={`flex items-center gap-2 rounded-md border px-3 py-2 ${meta.bg} ${meta.border}`}>
                      <span>{meta.icon}</span>
                      <span className={`text-xs font-medium ${meta.color}`}>{meta.label}</span>
                    </div>
                    {idx < stagesPresent.length - 1 && (
                      <ChevronRight className="h-4 w-4 text-slate-600" />
                    )}
                  </div>
                );
              })}
            </div>
          </CardContent>
        </Card>

        {/* Timeline visualization */}
        <Card>
          <CardHeader>
            <CardTitle>Attack Timeline</CardTitle>
          </CardHeader>
          <CardContent>
            {timeline.length === 0 ? (
              <EmptyState icon={AlertTriangle} title="No events in this campaign" />
            ) : (
              <div className="relative pl-8">
                {/* Vertical line */}
                <div className="absolute left-4 top-2 bottom-2 w-0.5 bg-gradient-to-b from-amber-500/60 via-orange-500/60 to-red-500/60" />

                {timeline.map((evt, idx) => {
                  const meta = STAGE_META[evt.stage];
                  return (
                    <div key={evt.id} className="relative pb-6 last:pb-0">
                      {/* Node */}
                      <div className={`absolute -left-8 top-1 h-8 w-8 rounded-full border-2 flex items-center justify-center text-sm ${meta.bg} ${meta.border} bg-slate-950`}>
                        {meta.icon}
                      </div>

                      <div className={`rounded-md border p-4 ${meta.bg} ${meta.border} bg-slate-900/40`}>
                        <div className="flex items-start justify-between gap-4">
                          <div className="flex-1 min-w-0">
                            <div className="flex items-center gap-2 mb-1">
                              <span className="text-[10px] uppercase tracking-wider text-slate-500">
                                Step {evt.sequenceNo}
                              </span>
                              <Badge kind="severity" value={evt.stage === "DATA_ACCESS" || evt.stage === "IMPACT" ? "CRITICAL" : "HIGH"}>
                                {meta.label}
                              </Badge>
                            </div>
                            <div className="text-sm text-slate-200">
                              {evt.detectionType ?? "Manual observation"}
                            </div>
                            <div className="text-xs text-slate-500 mt-1 font-mono">
                              {formatDate(evt.createdAt)}
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </CardContent>
        </Card>

        {/* Campaign metadata */}
        <Card>
          <CardHeader>
            <CardTitle>Metadata</CardTitle>
          </CardHeader>
          <CardContent>
            <dl className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
              <div>
                <dt className="text-xs text-slate-500 uppercase tracking-wider">Reference</dt>
                <dd className="text-white font-mono mt-1">{campaign.reference}</dd>
              </div>
              <div>
                <dt className="text-xs text-slate-500 uppercase tracking-wider">Status</dt>
                <dd className="text-white mt-1">{campaign.status}</dd>
              </div>
              <div>
                <dt className="text-xs text-slate-500 uppercase tracking-wider">First Seen</dt>
                <dd className="text-white mt-1">{formatDate(campaign.firstSeen)}</dd>
              </div>
              <div>
                <dt className="text-xs text-slate-500 uppercase tracking-wider">Last Seen</dt>
                <dd className="text-white mt-1">{formatDate(campaign.lastSeen)}</dd>
              </div>
              <div>
                <dt className="text-xs text-slate-500 uppercase tracking-wider">Application ID</dt>
                <dd className="text-white font-mono text-xs mt-1">{campaign.applicationId}</dd>
              </div>
            </dl>
          </CardContent>
        </Card>
      </div>
    </>
  );
}
