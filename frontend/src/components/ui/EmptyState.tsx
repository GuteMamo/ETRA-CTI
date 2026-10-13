import type { LucideIcon } from "lucide-react";

interface Props { icon: LucideIcon; title: string; description?: string; }

export function EmptyState({ icon: Icon, title, description }: Props) {
  return (
    <div className="flex flex-col items-center justify-center py-16 text-center">
      <div className="h-14 w-14 rounded-full bg-slate-800 flex items-center justify-center mb-4">
        <Icon className="h-6 w-6 text-slate-500" />
      </div>
      <div className="text-sm font-medium text-slate-300">{title}</div>
      {description && <div className="text-xs text-slate-500 mt-1 max-w-sm">{description}</div>}
    </div>
  );
}
