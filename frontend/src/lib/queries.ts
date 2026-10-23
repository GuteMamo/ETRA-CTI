import { useQuery } from "@tanstack/react-query";
import { api } from "./api";
import type {
  AlertDto, CampaignDto, CampaignEventDto, FindingDto, ImportDto, Page,
  InvestigationDto, InvestigationSummaryDto, IocDto, RemediationDto, AuditLogDto, MobileAppDto,
  ApiKeyDto
} from "./types";

export function useFindings(page = 0, size = 20) {
  return useQuery({
    queryKey: ["findings", page, size],
    queryFn: async () => (await api.get<Page<FindingDto>>(`/findings?page=${page}&size=${size}`)).data,
    refetchInterval: 3000,
  });
}

export function useFinding(id: string | undefined) {
  return useQuery({
    queryKey: ["finding", id],
    enabled: !!id,
    queryFn: async () => (await api.get<FindingDto>(`/findings/${id}`)).data,
    refetchInterval: 3000,
  });
}

export function useCampaigns() {
  return useQuery({
    queryKey: ["campaigns"],
    queryFn: async () => (await api.get<CampaignDto[]>(`/campaigns`)).data,
    refetchInterval: 4000,
  });
}

export function useCampaign(id: string | undefined) {
  return useQuery({
    queryKey: ["campaign", id],
    enabled: !!id,
    queryFn: async () => (await api.get<CampaignDto>(`/campaigns/${id}`)).data,
    refetchInterval: 4000,
  });
}

export function useCampaignTimeline(id: string | undefined) {
  return useQuery({
    queryKey: ["campaign-timeline", id],
    enabled: !!id,
    queryFn: async () => (await api.get<CampaignEventDto[]>(`/campaigns/${id}/timeline`)).data,
    refetchInterval: 4000,
  });
}

export function useInvestigations() {
  return useQuery({
    queryKey: ["investigations"],
    queryFn: async () => (await api.get<InvestigationDto[]>(`/investigations`)).data,
    refetchInterval: 3000,
  });
}

export function useInvestigationSummary(id: string | undefined) {
  return useQuery({
    queryKey: ["investigation-summary", id],
    enabled: !!id,
    queryFn: async () => (await api.get<InvestigationSummaryDto>(`/investigations/${id}/summary`)).data,
    refetchInterval: 3000,
  });
}

export function useRemediations() {
  return useQuery({
    queryKey: ["remediations"],
    queryFn: async () => (await api.get<RemediationDto[]>(`/remediations`)).data,
    refetchInterval: 3000,
  });
}

export function useImports() {
  return useQuery({
    queryKey: ["imports"],
    queryFn: async () => (await api.get<ImportDto[]>(`/integrations/imports`)).data,
    refetchInterval: 3000,
  });
}

export function useIocs() {
  return useQuery({
    queryKey: ["iocs"],
    queryFn: async () => (await api.get<IocDto[]>(`/threat-intel/iocs`)).data,
    refetchInterval: 5000,
  });
}

export function useAuditLogs(page = 0, size = 50) {
  return useQuery({
    queryKey: ["audit", page, size],
    queryFn: async () =>
      (await api.get<Page<AuditLogDto>>(`/audit?page=${page}&size=${size}`)).data,
    refetchInterval: 4000,
  });
}

export function useMobileApps() {
  return useQuery({
    queryKey: ["mobile-apps"],
    queryFn: async () => (await api.get<MobileAppDto[]>(`/mobile-apps`)).data,
    refetchInterval: 4000,
  });
}

export function useAlerts(status?: string) {
  return useQuery({
    queryKey: ["alerts", status],
    queryFn: async () => {
      const url = status ? `/alerts?status=${status}` : `/alerts`;
      const res = await api.get<Page<AlertDto> | AlertDto[]>(url);
      if (res.data && typeof res.data === "object" && "content" in res.data && Array.isArray((res.data as Page<AlertDto>).content)) {
        return (res.data as Page<AlertDto>).content;
      }
      return Array.isArray(res.data) ? res.data : [];
    },
    refetchInterval: 3000,
  });
}

export function useApiKeys(applicationId: string | undefined) {
  return useQuery({
    queryKey: ["api-keys", applicationId],
    enabled: !!applicationId,
    queryFn: async () => (await api.get<ApiKeyDto[]>(`/applications/${applicationId}/api-keys`)).data,
    refetchInterval: 3000,
  });
}

