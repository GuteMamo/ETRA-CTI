import { useState, type FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { Shield, Loader2 } from "lucide-react";
import { useAuth } from "@/contexts/AuthContext";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState("admin@etra.local");
  const [password, setPassword] = useState("Password123!");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null); setLoading(true);
    try {
      await login(email, password);
      navigate("/");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Login failed");
    } finally { setLoading(false); }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-slate-950 via-slate-900 to-blue-950 relative overflow-hidden">
      <div className="absolute inset-0 opacity-20"
        style={{ backgroundImage:
          "radial-gradient(circle at 20% 30%, rgba(59,130,246,0.4) 0%, transparent 40%), radial-gradient(circle at 80% 70%, rgba(59,130,246,0.3) 0%, transparent 40%)" }} />
      <div className="w-full max-w-md relative z-10">
        <div className="text-center mb-8">
          <div className="h-16 w-16 mx-auto rounded-2xl bg-gradient-to-br from-blue-500 to-blue-700 flex items-center justify-center shadow-lg shadow-blue-500/30 mb-4">
            <Shield className="h-8 w-8 text-white" />
          </div>
          <h1 className="text-3xl font-bold text-white">ETRA-CTI</h1>
          <p className="text-sm text-slate-400 mt-1">Security Intelligence Platform</p>
        </div>
        <form onSubmit={onSubmit}
          className="bg-slate-900/70 backdrop-blur-xl border border-slate-800 rounded-xl p-8 shadow-2xl">
          <h2 className="text-lg font-semibold text-white mb-6">Sign in to your workspace</h2>
          <div className="space-y-4">
            <div>
              <label className="text-xs font-medium text-slate-400 mb-1.5 block">Email</label>
              <Input type="email" required value={email} onChange={(e) => setEmail(e.target.value)}
                placeholder="you@company.com" autoComplete="email" />
            </div>
            <div>
              <label className="text-xs font-medium text-slate-400 mb-1.5 block">Password</label>
              <Input type="password" required value={password} onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••" autoComplete="current-password" />
            </div>
          </div>
          {error && (
            <div className="mt-4 rounded-md bg-red-500/10 border border-red-500/30 px-3 py-2 text-xs text-red-300">
              {error}
            </div>
          )}
          <Button type="submit" className="w-full mt-6" size="lg" disabled={loading}>
            {loading ? <><Loader2 className="h-4 w-4 animate-spin" /> Signing in…</> : "Sign in"}
          </Button>
          <p className="text-[10px] text-slate-500 text-center mt-6">
            Demo: admin@etra.local / Password123!
          </p>
        </form>
        <p className="text-center text-xs text-slate-600 mt-6">
          © 2026 ETRA-CTI — Application Exposure, Guardian, Intelligence & Security
        </p>
      </div>
    </div>
  );
}
