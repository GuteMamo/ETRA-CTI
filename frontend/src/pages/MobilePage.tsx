import { Smartphone, Shield, Package, CheckCircle2, XCircle } from "lucide-react";
import { Header } from "@/components/layout/Header";
import { Card } from "@/components/ui/Card";
import { Badge } from "@/components/ui/Badge";
import { EmptyState } from "@/components/ui/EmptyState";
import { useMobileApps } from "@/lib/queries";
import { formatDate } from "@/lib/utils";

export function MobilePage() {
  const { data: apps = [], isLoading } = useMobileApps();

  return (
    <>
      <Header
        title="Mobile Applications"
        description="APK/IPA inventory with signed status and permissions"
      />
      <div className="flex-1 overflow-y-auto p-8 space-y-4">

        {isLoading ? (
          <Card className="p-12 text-center text-sm text-slate-500">Loading…</Card>
        ) : apps.length === 0 ? (
          <Card>
            <EmptyState icon={Smartphone} title="No mobile apps registered"
              description="Register an APK or IPA and import a MobSF report to see findings here." />
          </Card>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {apps.map((app) => (
              <Card key={app.id} className="p-5">
                <div className="flex items-start justify-between mb-3">
                  <div className="flex items-center gap-3">
                    <div className="h-10 w-10 rounded-lg bg-purple-500/15 border border-purple-500/30 flex items-center justify-center">
                      <Smartphone className="h-5 w-5 text-purple-400" />
                    </div>
                    <div>
                      <div className="text-sm font-semibold text-white">{app.packageName}</div>
                      <div className="text-xs text-slate-500 mt-0.5">
                        {app.platform} · v{app.versionName ?? "?"}
                      </div>
                    </div>
                  </div>
                  {app.signed ? (
                    <Badge kind="severity" value="LOW">
                      <CheckCircle2 className="h-3 w-3 mr-1" /> Signed
                    </Badge>
                  ) : (
                    <Badge kind="severity" value="HIGH">
                      <XCircle className="h-3 w-3 mr-1" /> Unsigned
                    </Badge>
                  )}
                </div>

                <div className="grid grid-cols-3 gap-2 text-xs mb-3">
                  <div className="rounded bg-slate-800/50 p-2">
                    <div className="text-slate-500">Min SDK</div>
                    <div className="text-white mt-0.5">{app.minSdk ?? "—"}</div>
                  </div>
                  <div className="rounded bg-slate-800/50 p-2">
                    <div className="text-slate-500">Target</div>
                    <div className="text-white mt-0.5">{app.targetSdk ?? "—"}</div>
                  </div>
                  <div className="rounded bg-slate-800/50 p-2">
                    <div className="text-slate-500">Perms</div>
                    <div className="text-white mt-0.5">{app.permissions?.length ?? 0}</div>
                  </div>
                </div>

                <div className="text-xs text-slate-500 font-mono truncate">
                  SHA-256: {app.sha256.slice(0, 24)}…
                </div>

                <div className="text-xs text-slate-600 mt-2">
                  Registered {formatDate(app.createdAt)}
                </div>

                {app.permissions && app.permissions.length > 0 && (
                  <div className="mt-3 pt-3 border-t border-slate-800">
                    <div className="text-xs text-slate-500 mb-2">Permissions:</div>
                    <div className="flex flex-wrap gap-1">
                      {app.permissions.slice(0, 4).map((p) => (
                        <span key={p} className="text-[10px] px-2 py-0.5 rounded bg-slate-800 text-slate-400 font-mono">
                          {p.split(".").pop()}
                        </span>
                      ))}
                      {app.permissions.length > 4 && (
                        <span className="text-[10px] px-2 py-0.5 text-slate-500">
                          +{app.permissions.length - 4} more
                        </span>
                      )}
                    </div>
                  </div>
                )}
              </Card>
            ))}
          </div>
        )}
      </div>
    </>
  );
}
