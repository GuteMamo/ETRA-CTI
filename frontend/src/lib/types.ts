export type Severity = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
export type RiskBand = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
export type FindingStatus =
  | "OPEN" | "CONFIRMED" | "FALSE_POSITIVE" | "ASSIGNED" | "IN_PROGRESS"
  | "FIXED" | "READY_FOR_RETEST" | "VERIFIED" | "CLOSED" | "REOPENED";
export type UserRole = "ADMIN" | "SECURITY_MANAGER" | "PENTESTER" | "ANALYST" | "DEVELOPER" | "VIEWER";
export type ApplicationType = "WEB" | "API" | "MOBILE" | "WEB_API";
export type Environment = "DEV" | "STAGING" | "PRODUCTION";
export type Criticality = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
export type EventSource = "APPLICATION_LOG" | "API_GATEWAY" | "BURP" | "ZAP" | "NMAP" | "MANUAL" | "OTHER";
export type AttackStage = "RECON" | "AUTH_ABUSE" | "EXPLOITATION" | "DATA_ACCESS" | "IMPACT";
export type InvestigationStatus = "OPEN" | "IN_PROGRESS" | "CONTAINED" | "CLOSED" | "ARCHIVED";
export type InvestigationPriority = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
export type RemediationStatus = "OPEN" | "IN_PROGRESS" | "FIXED" | "READY_FOR_RETEST" | "VERIFIED" | "REOPENED";
export type RetestResult = "PASS" | "FAIL" | "INCONCLUSIVE";
export type IocType = "IP" | "DOMAIN" | "URL" | "SHA256" | "MD5";
export type ImportStatus = "PENDING" | "RUNNING" | "COMPLETED" | "FAILED";
export type IntegrationSource = "BURP" | "ZAP" | "NMAP" | "GENERIC";
export type MobilePlatform = "ANDROID" | "IOS";

export interface LoginRequest { email: string; password: string; }
export interface RegisterRequest { email: string; password: string; fullName: string; }
export interface TokenPair { accessToken: string; refreshToken: string; }
export interface UserDto { id: string; email: string; fullName: string; role: UserRole; }

export interface OrganizationDto { id: string; name: string; slug: string; myRole: UserRole; }
export interface ProjectDto { id: string; organizationId: string; name: string; description?: string; }
export interface ApplicationDto {
  id: string; projectId: string; name: string;
  type: ApplicationType; environment: Environment;
  criticality: Criticality; baseUrl?: string; ownerId?: string;
}
export interface AssetDto {
  id: string; applicationId: string; type: string;
  hostname?: string; url?: string; port?: number;
  protocol?: string; environment?: string; criticality?: string; status: string;
}

export interface EventDto {
  id: string; applicationId: string; source: EventSource;
  eventTime: string; sourceIp?: string;
  method?: string; path?: string; statusCode?: number;
  userIdentifier?: string; normalized: Record<string, unknown>;
}
export interface DetectionDto {
  id: string; eventId: string; ruleId: string; type: string;
  severity: Severity; confidence: number; details: Record<string, unknown>;
}

export interface FindingDto {
  id: string; reference: string;
  applicationId: string; detectionId?: string | null;
  title: string; description?: string;
  endpoint?: string; parameter?: string;
  severity: Severity; confidence?: number;
  status: FindingStatus; source?: string;
  assignedTo?: string | null;
  riskScore?: number; riskBand?: RiskBand;
  createdAt: string;
}

export interface CampaignDto {
  id: string; reference: string; applicationId: string;
  sourceIp?: string; title: string;
  riskBand?: RiskBand; eventCount: number;
  firstSeen: string; lastSeen: string; status: string;
}
export interface CampaignEventDto {
  id: string; campaignId: string; eventId: string;
  stage: AttackStage; detectionType?: string | null;
  sequenceNo: number; createdAt: string;
}

export interface InvestigationDto {
  id: string; reference: string;
  campaignId?: string | null; applicationId: string;
  title: string; description?: string;
  status: InvestigationStatus; priority: InvestigationPriority;
  assignedTo?: string | null; createdBy?: string | null;
  createdAt: string;
}
export interface InvestigationNoteDto {
  id: string; investigationId: string; authorId?: string | null;
  note: string; createdAt: string;
}
export interface InvestigationSummaryDto {
  investigation: InvestigationDto;
  campaign?: CampaignDto | null;
  timeline: CampaignEventDto[];
  findings: FindingDto[];
  notes: InvestigationNoteDto[];
}

export interface EvidenceDto {
  id: string; findingId: string; filename: string;
  contentType?: string; sizeBytes: number;
  sha256: string; createdBy?: string | null; createdAt: string;
}
export interface IntegrityResultDto {
  evidenceId: string; filename: string;
  integrity: "INTACT" | "TAMPERED" | "FILE_MISSING";
  storedSha256: string; currentSha256?: string | null;
  sizeBytes: number;
}

export interface RemediationDto {
  id: string; findingId: string; developerId?: string | null;
  status: RemediationStatus;
  fixNotes?: string | null; commitRef?: string | null;
  startedAt?: string | null; fixedAt?: string | null; verifiedAt?: string | null;
  reopenedCount: number; createdAt: string;
}
export interface RetestDto {
  id: string; findingId: string; remediationId?: string | null;
  performedBy?: string | null; result: RetestResult;
  notes?: string | null; createdAt: string;
}

export interface IocDto {
  id: string; type: IocType; value: string;
  description?: string | null; source?: string | null;
  severity: Severity; active: boolean;
  expiresAt?: string | null; createdAt: string;
}

export interface ImportDto {
  id: string; source: IntegrationSource;
  applicationId: string; filename?: string | null;
  sizeBytes?: number | null;
  findingsCreated: number; findingsDuplicated: number; assetsCreated: number;
  status: ImportStatus; errorMessage?: string | null;
  startedAt: string; finishedAt?: string | null;
  durationMs?: number | null;
}

export interface MobileAppDto {
  id: string; applicationId: string; platform: MobilePlatform;
  packageName: string; versionName?: string; versionCode?: string;
  fileName?: string; sizeBytes?: number; sha256: string;
  signed?: boolean; minSdk?: string; targetSdk?: string;
  permissions?: string[]; createdAt: string;
}

export interface AuditLogDto {
  id: string; userId?: string | null; action: string;
  resourceType?: string; resourceId?: string;
  oldValue?: Record<string, unknown> | null;
  newValue?: Record<string, unknown> | null;
  ipAddress?: string | null; createdAt: string;
}

export interface AiResultDto {
  analysisType: string; resourceType: string; resourceId: string;
  result: string; model: string; fromCache: boolean;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}
