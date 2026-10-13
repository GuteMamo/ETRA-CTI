import type { LucideIcon } from "lucide-react";
import { cn } from "@/lib/utils";

interface Props {
  label: string;
  value: number | string;
  icon: LucideIcon;
  tone?: "blue" | "red" | "orange" | "emerald" | "slate";
  hint?: string;
}

const tones = {
  blue: "from-blue-500/20 to-blue-500/5 border-blue-500/30 text-blue-300",
  red: "from-red-500/20 to-red-500/5 border-red-500/30 text-red-300",
  orange: "from-orange-500/20 to-orange-500/5 border-orange-500/30 text-orange-300",
  emerald: "from-emerald-500/20 to-emerald-500/5 border-emerald-500/30 text-emerald-300",
  slate: "from-slate-500/20 to-slate-500/5 border-slate-500/30 text-slate-300",
};

export function StatCard({ label, value, icon: Icon, tone = "slate", hint }: Props) {
  return (
    <div className={cn(
      "rounded-lg border bg-gradient-to-br p-5 backdrop-blur",
      tones[tone]
    )}>
      <div className="flex items-center justify-between">
        <div className="text-xs uppercase tracking-wider opacity-80">{label}</div>
        <Icon className="h-4 w-4 opacity-80" />
      </div>
      <div className="mt-3 text-3xl font-bold text-white">{value}</div>
      {hint && <div className="text-xs text-slate-500 mt-1">{hint}</div>}
    </div>
  );
}
