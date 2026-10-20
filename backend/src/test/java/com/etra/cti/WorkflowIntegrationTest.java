package com.etra.cti;

import com.etra.cti.alert.AlertDto;
import com.etra.cti.alert.AlertStatus;
import com.etra.cti.application.dto.ApplicationDto;
import com.etra.cti.application.dto.CreateApplicationRequest;
import com.etra.cti.asset.dto.AssetDto;
import com.etra.cti.asset.dto.CreateAssetRequest;
import com.etra.cti.auth.dto.LoginRequest;
import com.etra.cti.auth.dto.RegisterRequest;
import com.etra.cti.auth.dto.TokenPair;
import com.etra.cti.detection.Severity;
import com.etra.cti.event.EventSource;
import com.etra.cti.event.dto.EventDto;
import com.etra.cti.event.dto.IngestEventRequest;
import com.etra.cti.evidence.dto.EvidenceDto;
import com.etra.cti.evidence.dto.IntegrityResultDto;
import com.etra.cti.finding.FindingStatus;
import com.etra.cti.finding.dto.CreateFindingRequest;
import com.etra.cti.finding.dto.FindingDto;
import com.etra.cti.organization.dto.CreateOrganizationRequest;
import com.etra.cti.organization.dto.OrganizationDto;
import com.etra.cti.project.dto.CreateProjectRequest;
import com.etra.cti.project.dto.ProjectDto;
import com.etra.cti.remediation.RemediationStatus;
import com.etra.cti.remediation.RetestResult;
import com.etra.cti.remediation.dto.MarkFixedRequest;
import com.etra.cti.remediation.dto.PerformRetestRequest;
import com.etra.cti.remediation.dto.RemediationDto;
import com.etra.cti.remediation.dto.RetestDto;
import com.etra.cti.user.UserRole;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class WorkflowIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();
    }

    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Test
    @DisplayName("End-to-End Security Workflow: Auth -> Ingest -> Detection -> Alert -> Finding -> Remediation -> Retest -> Evidence -> Audit")
    void testCompleteSecurityLifecycle() throws Exception {
        String runId = UUID.randomUUID().toString().substring(0, 8);

        // 1. Health check
        mockMvc.perform(get("/api/health"))
            .andExpect(status().isOk());

        // 2. Register analyst user
        String email = "analyst_" + runId + "@etra.et";
        RegisterRequest regReq = new RegisterRequest(email, "StrongPass123!", "Sec Analyst " + runId);
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(regReq)))
            .andExpect(status().isCreated());

        // Login to get token
        LoginRequest loginReq = new LoginRequest(email, "StrongPass123!");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(loginReq)))
            .andExpect(status().isOk())
            .andReturn();
        TokenPair tokens = mapper.readValue(loginResult.getResponse().getContentAsString(), TokenPair.class);
        String authHeader = "Bearer " + tokens.accessToken();

        // 3. Create Org, Project, Application, Asset
        CreateOrganizationRequest orgReq = new CreateOrganizationRequest("ETRA Org " + runId, "etra-org-" + runId);
        MvcResult orgResult = mockMvc.perform(post("/api/organizations")
                .header("Authorization", authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(orgReq)))
            .andExpect(status().isCreated())
            .andReturn();
        OrganizationDto org = mapper.readValue(orgResult.getResponse().getContentAsString(), OrganizationDto.class);

        CreateProjectRequest projReq = new CreateProjectRequest("Core Banking Sec", "Security review of core banking");
        MvcResult projResult = mockMvc.perform(post("/api/organizations/" + org.id() + "/projects")
                .header("Authorization", authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(projReq)))
            .andExpect(status().isCreated())
            .andReturn();
        ProjectDto proj = mapper.readValue(projResult.getResponse().getContentAsString(), ProjectDto.class);

        CreateApplicationRequest appReq = new CreateApplicationRequest(
            "Banking Portal",
            com.etra.cti.application.ApplicationType.WEB,
            com.etra.cti.application.Environment.PRODUCTION,
            com.etra.cti.application.Criticality.CRITICAL,
            "https://bank.etra.et"
        );
        MvcResult appResult = mockMvc.perform(post("/api/projects/" + proj.id() + "/applications")
                .header("Authorization", authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(appReq)))
            .andExpect(status().isCreated())
            .andReturn();
        ApplicationDto app = mapper.readValue(appResult.getResponse().getContentAsString(), ApplicationDto.class);

        CreateAssetRequest assetReq = new CreateAssetRequest(
            com.etra.cti.asset.AssetType.URL,
            "bank.etra.et",
            "https://bank.etra.et/api/transfer",
            443,
            "HTTPS",
            "PRODUCTION",
            "CRITICAL"
        );
        mockMvc.perform(post("/api/applications/" + app.id() + "/assets")
                .header("Authorization", authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(assetReq)))
            .andExpect(status().isCreated());

        // 4. Ingest Event with SQL Injection payload
        IngestEventRequest eventReq = new IngestEventRequest(
            app.id(),
            EventSource.APPLICATION_LOG,
            Instant.now(),
            "203.0.113.42",
            "POST",
            "/api/transfer?account=1' OR '1'='1",
            200,
            "attacker-session",
            Map.of("userAgent", "Mozilla/5.0", "password", "supersecret123")
        );
        MvcResult eventResult = mockMvc.perform(post("/api/events")
                .header("Authorization", authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(eventReq)))
            .andExpect(status().isCreated())
            .andReturn();
        EventDto event = mapper.readValue(eventResult.getResponse().getContentAsString(), EventDto.class);

        // Verify password redaction
        assertFalse(event.normalized().toString().contains("supersecret123"));

        // 5. Verify detection was recorded
        MvcResult detResult = mockMvc.perform(get("/api/detections?eventId=" + event.id())
                .header("Authorization", authHeader))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode detectionsNode = mapper.readTree(detResult.getResponse().getContentAsString());
        assertTrue(detectionsNode.get("totalElements").asInt() >= 1);

        // 6. Verify Alert was auto-generated
        MvcResult alertResult = mockMvc.perform(get("/api/alerts?applicationId=" + app.id())
                .header("Authorization", authHeader))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode alertsNode = mapper.readTree(alertResult.getResponse().getContentAsString());
        assertTrue(alertsNode.get("totalElements").asInt() >= 1);
        UUID alertId = UUID.fromString(alertsNode.get("content").get(0).get("id").asText());

        // Acknowledge Alert
        MvcResult ackResult = mockMvc.perform(post("/api/alerts/" + alertId + "/acknowledge")
                .header("Authorization", authHeader))
            .andExpect(status().isOk())
            .andReturn();
        AlertDto ackAlert = mapper.readValue(ackResult.getResponse().getContentAsString(), AlertDto.class);
        assertEquals(AlertStatus.ACKNOWLEDGED, ackAlert.status());

        // 7. Create Finding with Severity HIGH
        CreateFindingRequest findReq = new CreateFindingRequest(
            app.id(),
            null,
            "SQL Injection in Transfer Parameter",
            "SQL injection vulnerability allows unauthorized balance manipulation",
            "/api/transfer",
            "account",
            Severity.HIGH,
            new BigDecimal("0.900"),
            "APPLICATION_LOG"
        );
        MvcResult findResult = mockMvc.perform(post("/api/findings")
                .header("Authorization", authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(findReq)))
            .andExpect(status().isCreated())
            .andReturn();
        FindingDto finding = mapper.readValue(findResult.getResponse().getContentAsString(), FindingDto.class);

        // Verify risk calculation matches Section 8.3 formula:
        // Sev=75 (0.40=30), Conf=90 (0.25=22.5), Crit=100 (0.20=20), Expo=100 (0.15=15) -> 87.5 CRITICAL
        assertEquals("CRITICAL", finding.riskBand());
        assertTrue(finding.riskScore().doubleValue() >= 75.0);

        // 8. Verify auto-generated remediation existed
        MvcResult remResult = mockMvc.perform(get("/api/findings/" + finding.id() + "/remediation")
                .header("Authorization", authHeader))
            .andExpect(status().isOk())
            .andReturn();
        RemediationDto rem = mapper.readValue(remResult.getResponse().getContentAsString(), RemediationDto.class);
        assertNotNull(rem.recommendation());
        assertTrue(rem.recommendation().toLowerCase().contains("parameterized")
            || rem.recommendation().toLowerCase().contains("prepared"));

        // 9. Remediation workflow: Mark fixed (READY_FOR_RETEST)
        MarkFixedRequest fixReq = new MarkFixedRequest("Applied prepared statements in BankingDao.java", "git:a1b2c3d");
        mockMvc.perform(post("/api/findings/" + finding.id() + "/remediation/ready")
                .header("Authorization", authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(fixReq)))
            .andExpect(status().isOk());

        // 10. Retest fails -> Finding reopened
        PerformRetestRequest failRetest = new PerformRetestRequest(
            RetestResult.FAIL,
            "Bypassed filter using alternate SQLi payload 1' UNION SELECT"
        );
        MvcResult failResult = mockMvc.perform(post("/api/findings/" + finding.id() + "/retest")
                .header("Authorization", authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(failRetest)))
            .andExpect(status().isCreated())
            .andReturn();
        RetestDto retestDto = mapper.readValue(failResult.getResponse().getContentAsString(), RetestDto.class);
        assertEquals(RetestResult.FAIL, retestDto.result());

        // Check finding status is REOPENED
        MvcResult reopenedFindResult = mockMvc.perform(get("/api/findings/" + finding.id())
                .header("Authorization", authHeader))
            .andExpect(status().isOk())
            .andReturn();
        FindingDto reopenedFinding = mapper.readValue(reopenedFindResult.getResponse().getContentAsString(), FindingDto.class);
        assertEquals(FindingStatus.REOPENED, reopenedFinding.status());

        // Mark fixed again
        mockMvc.perform(post("/api/findings/" + finding.id() + "/remediation/ready")
                .header("Authorization", authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new MarkFixedRequest("Fixed all union injection vectors", "git:e4f5g6h"))))
            .andExpect(status().isOk());

        // Retest passes -> Finding verified / closed
        PerformRetestRequest passRetest = new PerformRetestRequest(
            RetestResult.PASS,
            "Exploit payload safely escaped, 400 Bad Request returned as expected"
        );
        mockMvc.perform(post("/api/findings/" + finding.id() + "/retest")
                .header("Authorization", authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(passRetest)))
            .andExpect(status().isCreated());

        MvcResult verifiedFindResult = mockMvc.perform(get("/api/findings/" + finding.id())
                .header("Authorization", authHeader))
            .andExpect(status().isOk())
            .andReturn();
        FindingDto verifiedFinding = mapper.readValue(verifiedFindResult.getResponse().getContentAsString(), FindingDto.class);
        assertEquals(FindingStatus.VERIFIED, verifiedFinding.status());

        // 11. Evidence upload and SHA-256 verification
        MockMultipartFile file = new MockMultipartFile(
            "file", "sqli_poc.txt", "text/plain", "SELECT * FROM accounts WHERE id = 1' OR '1'='1".getBytes()
        );
        MvcResult uploadResult = mockMvc.perform(multipart("/api/findings/" + finding.id() + "/evidence")
                .file(file)
                .header("Authorization", authHeader))
            .andExpect(status().isCreated())
            .andReturn();
        EvidenceDto evidence = mapper.readValue(uploadResult.getResponse().getContentAsString(), EvidenceDto.class);
        assertNotNull(evidence.sha256());

        // Verify SHA-256 integrity via POST
        MvcResult verifyResult = mockMvc.perform(post("/api/evidence/" + evidence.id() + "/verify")
                .header("Authorization", authHeader))
            .andExpect(status().isOk())
            .andReturn();
        IntegrityResultDto integrity = mapper.readValue(verifyResult.getResponse().getContentAsString(), IntegrityResultDto.class);
        assertEquals("INTACT", integrity.integrity());
        assertEquals(evidence.sha256(), integrity.storedSha256());

        // 12. Audit logs verification
        MvcResult auditResult = mockMvc.perform(get("/api/audit-logs?resourceType=Finding&resourceId=" + finding.id())
                .header("Authorization", authHeader))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode auditNode = mapper.readTree(auditResult.getResponse().getContentAsString());
        assertTrue(auditNode.get("totalElements").asInt() >= 1);
    }
}
