import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { Toaster } from "sonner";
import { AuthProvider, useAuth } from "@/contexts/AuthContext";
import { Layout } from "@/components/layout/Layout";
import { RoleProtectedRoute } from "@/components/RoleProtectedRoute";
import { LoginPage } from "@/pages/LoginPage";
import { DashboardPage } from "@/pages/DashboardPage";
import { FindingsPage } from "@/pages/FindingsPage";
import { FindingDetailPage } from "@/pages/FindingDetailPage";
import { CampaignsPage } from "@/pages/CampaignsPage";
import { CampaignDetailPage } from "@/pages/CampaignDetailPage";
import { InvestigationsPage } from "@/pages/InvestigationsPage";
import { InvestigationDetailPage } from "@/pages/InvestigationDetailPage";
import { RemediationsPage } from "@/pages/RemediationsPage";
import { ImportsPage } from "@/pages/ImportsPage";
import { MobilePage } from "@/pages/MobilePage";
import { IocsPage } from "@/pages/IocsPage";
import { AuditPage } from "@/pages/AuditPage";
import { UsersPage } from "@/pages/UsersPage";

const qc = new QueryClient({
  defaultOptions: { queries: { retry: 1, refetchOnWindowFocus: false, staleTime: 30_000 } },
});

function Protected({ children }: { children: React.ReactNode }) {
  const { user, loading } = useAuth();
  if (loading) return (
    <div className="min-h-screen flex items-center justify-center bg-slate-950">
      <div className="text-slate-400 text-sm">Loading…</div>
    </div>
  );
  if (!user) return <Navigate to="/login" replace />;
  return <>{children}</>;
}

export default function App() {
  return (
    <QueryClientProvider client={qc}>
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route element={<Protected><Layout /></Protected>}>
              <Route index element={<DashboardPage />} />
              <Route path="findings" element={<FindingsPage />} />
              <Route path="findings/:id" element={<FindingDetailPage />} />

              <Route path="campaigns" element={
                <RoleProtectedRoute roles={["ADMIN","SECURITY_MANAGER","ANALYST","PENTESTER"]}>
                  <CampaignsPage />
                </RoleProtectedRoute>
              } />
              <Route path="campaigns/:id" element={
                <RoleProtectedRoute roles={["ADMIN","SECURITY_MANAGER","ANALYST","PENTESTER"]}>
                  <CampaignDetailPage />
                </RoleProtectedRoute>
              } />

              <Route path="investigations" element={
                <RoleProtectedRoute roles={["ADMIN","SECURITY_MANAGER","ANALYST"]}>
                  <InvestigationsPage />
                </RoleProtectedRoute>
              } />
              <Route path="investigations/:id" element={
                <RoleProtectedRoute roles={["ADMIN","SECURITY_MANAGER","ANALYST"]}>
                  <InvestigationDetailPage />
                </RoleProtectedRoute>
              } />

              <Route path="remediations" element={
                <RoleProtectedRoute roles={["ADMIN","SECURITY_MANAGER","PENTESTER","ANALYST","DEVELOPER"]}>
                  <RemediationsPage />
                </RoleProtectedRoute>
              } />

              <Route path="imports" element={
                <RoleProtectedRoute roles={["ADMIN","SECURITY_MANAGER","PENTESTER","ANALYST"]}>
                  <ImportsPage />
                </RoleProtectedRoute>
              } />

              <Route path="mobile" element={
                <RoleProtectedRoute roles={["ADMIN","SECURITY_MANAGER","PENTESTER","ANALYST"]}>
                  <MobilePage />
                </RoleProtectedRoute>
              } />

              <Route path="iocs" element={
                <RoleProtectedRoute roles={["ADMIN","SECURITY_MANAGER","ANALYST"]}>
                  <IocsPage />
                </RoleProtectedRoute>
              } />

              <Route path="users" element={
                <RoleProtectedRoute roles={["ADMIN"]}>
                  <UsersPage />
                </RoleProtectedRoute>
              } />

              <Route path="audit" element={
                <RoleProtectedRoute roles={["ADMIN","SECURITY_MANAGER"]}>
                  <AuditPage />
                </RoleProtectedRoute>
              } />
            </Route>
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
          <Toaster theme="dark" position="top-right" richColors />
        </BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
  );
}
