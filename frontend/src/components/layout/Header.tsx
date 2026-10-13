import { Activity } from "lucide-react";

interface Props { title: string; description?: string; actions?: React.ReactNode; }

export function Header({ title, description, actions }: Props) {
  return (
    <header className="h-16 border-b border-slate-800 bg-slate-950/40 backdrop-blur flex items-center px-8 shrink-0">
      <div className="flex-1 min-w-0">
        <h1 className="text-lg font-semibold text-white truncate">{title}</h1>
        {description && <p className="text-xs text-slate-500 truncate">{description}</p>}
      </div>
      <div className="flex items-center gap-3">
        {actions}
        <div className="flex items-center gap-1.5 text-xs text-emerald-400 bg-emerald-500/10 border border-emerald-500/30 rounded-full px-2.5 py-1">
          <Activity className="h-3 w-3" />
          Live
        </div>
      </div>
    </header>
  );
}
