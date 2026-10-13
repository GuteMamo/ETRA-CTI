import { cn } from "@/lib/utils";
import type { Severity, RiskBand, FindingStatus, RemediationStatus } from "@/lib/types";

type Kind = "severity" | "risk" | "status" | "remediation" | "neutral";
interface Props { children: React.ReactNode; kind?: Kind; value?: string; className?: string; }

function severityClass(v: string): string {
  switch (v) {
    case "LOW": return "bg-blue-500/15 text-blue-300 border-blue-500/30";
    case "MEDIUM": return "bg-amber-500/15 text-amber-300 border-amber-500/30";
    case "HIGH": return "bg-orange-500/15 text-orange-300 border-orange-500/30";
    case "CRITICAL": return "bg-red-500/15 text-red-300 border-red-500/30";
    default: return "bg-slate-500/15 text-slate-300 border-slate-500/30";
  }
}
function statusClass(v: string): string {
  switch (v) {
    case "OPEN": return "bg-red-500/15 text-red-300 border-red-500/30";
    case "CONFIRMED": return "bg-orange-500/15 text-orange-300 border-orange-500/30";
    case "ASSIGNED": case "IN_PROGRESS": return "bg-blue-500/15 text-blue-300 border-blue-500/30";
    case "FIXED": case "READY_FOR_RETEST": return "bg-cyan-500/15 text-cyan-300 border-cyan-500/30";
    case "VERIFIED": case "CLOSED": return "bg-emerald-500/15 text-emerald-300 border-emerald-500/30";
    case "REOPENED": return "bg-red-500/15 text-red-300 border-red-500/30";
    case "FALSE_POSITIVE": return "bg-slate-500/15 text-slate-300 border-slate-500/30";
    default: return "bg-slate-500/15 text-slate-300 border-slate-500/30";
  }
}
function remediationClass(v: string): string {
  switch (v) {
    case "OPEN": return "bg-red-500/15 text-red-300 border-red-500/30";
    case "IN_PROGRESS": return "bg-blue-500/15 text-blue-300 border-blue-500/30";
    case "READY_FOR_RETEST": case "FIXED": return "bg-cyan-500/15 text-cyan-300 border-cyan-500/30";
    case "VERIFIED": return "bg-emerald-500/15 text-emerald-300 border-emerald-500/30";
    case "REOPENED": return "bg-red-500/15 text-red-300 border-red-500/30";
    default: return "bg-slate-500/15 text-slate-300 border-slate-500/30";
  }
}

export function Badge({ children, kind = "neutral", value, className }: Props) {
  let tone = "bg-slate-500/15 text-slate-300 border-slate-500/30";
  if (kind === "severity" || kind === "risk") tone = severityClass(String(value ?? children));
  if (kind === "status") tone = statusClass(String(value ?? children));
  if (kind === "remediation") tone = remediationClass(String(value ?? children));

  return (
    <span className={cn(
      "inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-medium",
      tone, className
    )}>
      {children}
    </span>
  );
}
