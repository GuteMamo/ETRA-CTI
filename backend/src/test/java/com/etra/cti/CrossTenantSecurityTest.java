package com.etra.cti;

import com.etra.cti.asset.AssetType;
import com.etra.cti.asset.dto.CreateAssetRequest;
import com.etra.cti.auth.dto.LoginRequest;
import com.etra.cti.auth.dto.RegisterRequest;
import com.etra.cti.auth.dto.TokenPair;
import com.etra.cti.finding.FindingStatus;
import com.etra.cti.finding.dto.AssignFindingRequest;
import com.etra.cti.finding.dto.CreateFindingRequest;
import com.etra.cti.finding.dto.OverridePriorityRequest;
import com.etra.cti.finding.dto.UpdateStatusRequest;
import com.etra.cti.investigation.dto.AddNoteRequest;
import com.etra.cti.investigation.dto.CreateInvestigationRequest;
import com.etra.cti.investigation.dto.UpdateInvestigationStatusRequest;
import com.etra.cti.organization.dto.CreateOrganizationRequest;
import com.etra.cti.organization.dto.OrganizationDto;
import com.etra.cti.project.dto.CreateProjectRequest;
import com.etra.cti.project.dto.ProjectDto;
import com.etra.cti.user.UserRole;
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
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class CrossTenantSecurityTest {

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

    private String registerAndLogin(String email, String password, String name, UserRole role) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new RegisterRequest(email, password, name, role))))
            .andExpect(status().isCreated());

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new LoginRequest(email, password))))
            .andExpect(status().isOk())
            .andReturn();
        return "Bearer " + mapper.readValue(login.getResponse().getContentAsString(), TokenPair.class).accessToken();
    }

    @Test
    @DisplayName("Cross-tenant isolation across all resources: User B cannot access or mutate Org A resources")
    void testCrossTenantIsolation() throws Exception {
        String runId = UUID.randomUUID().toString().substring(0, 8);

        // 1. Register User A (Security Manager in Tenant A) and User B (Security Manager in Tenant B)
        String tokenA = registerAndLogin("usera_" + runId + "@tenant-a.com", "Pass1234!", "User A", UserRole.SECURITY_MANAGER);
        String tokenB = registerAndLogin("userb_" + runId + "@tenant-b.com", "Pass1234!", "User B", UserRole.SECURITY_MANAGER);
        String adminToken = registerAndLogin("admin_" + runId + "@etra.et", "AdminPass1234!", "Platform Admin", UserRole.ADMIN);

        // 2. User A creates Org A, Project A, App A
        MvcResult orgAResult = mockMvc.perform(post("/api/organizations")
                .header("Authorization", tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new CreateOrganizationRequest("Org A " + runId, "tenant-a-" + runId))))
            .andExpect(status().isCreated())
            .andReturn();
        OrganizationDto orgA = mapper.readValue(orgAResult.getResponse().getContentAsString(), OrganizationDto.class);

        MvcResult projAResult = mockMvc.perform(post("/api/organizations/" + orgA.id() + "/projects")
                .header("Authorization", tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new CreateProjectRequest("Project A", "Secret Project"))))
            .andExpect(status().isCreated())
            .andReturn();
        ProjectDto projA = mapper.readValue(projAResult.getResponse().getContentAsString(), ProjectDto.class);

        MvcResult appAResult = mockMvc.perform(post("/api/projects/" + projA.id() + "/applications")
                .header("Authorization", tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"App A\",\"type\":\"WEB\",\"environment\":\"DEV\",\"criticality\":\"LOW\"}"))
            .andExpect(status().isCreated())
            .andReturn();
        var appA = mapper.readTree(appAResult.getResponse().getContentAsString());
        UUID appAId = UUID.fromString(appA.get("id").asText());

        // 3. User A creates Asset in App A
        MvcResult assetAResult = mockMvc.perform(post("/api/applications/" + appAId + "/assets")
                .header("Authorization", tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new CreateAssetRequest(AssetType.SERVICE, "app-a.internal", "https://app-a.internal", 443, "HTTPS", "DEV", "LOW"))))
            .andExpect(status().isCreated())
            .andReturn();
        var assetA = mapper.readTree(assetAResult.getResponse().getContentAsString());
        UUID assetAId = UUID.fromString(assetA.get("id").asText());

        // 4. User A creates Finding in App A
        MvcResult findingAResult = mockMvc.perform(post("/api/findings")
                .header("Authorization", tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new CreateFindingRequest(
                    appAId, null, "SQL Injection in Search", "Unsanitized user input",
                    "/search", "q", com.etra.cti.detection.Severity.HIGH, new BigDecimal("0.900"), "MANUAL"
                ))))
            .andExpect(status().isCreated())
            .andReturn();
        var findingA = mapper.readTree(findingAResult.getResponse().getContentAsString());
        UUID findingAId = UUID.fromString(findingA.get("id").asText());

        // 5. User A uploads Evidence to Finding A
        MockMultipartFile file = new MockMultipartFile("file", "poc.txt", "text/plain", "POC evidence data".getBytes());
        MvcResult evidenceAResult = mockMvc.perform(multipart("/api/findings/" + findingAId + "/evidence")
                .file(file)
                .header("Authorization", tokenA))
            .andExpect(status().isCreated())
            .andReturn();
        var evidenceA = mapper.readTree(evidenceAResult.getResponse().getContentAsString());
        UUID evidenceAId = UUID.fromString(evidenceA.get("id").asText());

        // 6. User A creates Investigation in App A
        MvcResult invAResult = mockMvc.perform(post("/api/investigations")
                .header("Authorization", tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new CreateInvestigationRequest(
                    null, appAId, "Investigation on App A", "Investigating SQLi alert", com.etra.cti.investigation.InvestigationPriority.HIGH
                ))))
            .andExpect(status().isCreated())
            .andReturn();
        var invA = mapper.readTree(invAResult.getResponse().getContentAsString());
        UUID invAId = UUID.fromString(invA.get("id").asText());

        // ==================== CROSS-TENANT ATTEMPTS BY USER B ====================

        // Org A access by User B -> 403
        mockMvc.perform(get("/api/organizations/" + orgA.id())
                .header("Authorization", tokenB))
            .andExpect(status().isForbidden());

        // Project A access by User B -> 403
        mockMvc.perform(get("/api/projects/" + projA.id())
                .header("Authorization", tokenB))
            .andExpect(status().isForbidden());

        // Create Application in Project A by User B -> 403
        mockMvc.perform(post("/api/projects/" + projA.id() + "/applications")
                .header("Authorization", tokenB)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Hacked App\",\"type\":\"WEB\",\"environment\":\"DEV\",\"criticality\":\"LOW\"}"))
            .andExpect(status().isForbidden());

        // Create Asset in App A by User B -> 403
        mockMvc.perform(post("/api/applications/" + appAId + "/assets")
                .header("Authorization", tokenB)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new CreateAssetRequest(AssetType.SERVICE, "bad.internal", "https://bad.internal", 80, "HTTP", "DEV", "LOW"))))
            .andExpect(status().isForbidden());

        // List Assets of App A by User B -> 403
        mockMvc.perform(get("/api/applications/" + appAId + "/assets")
                .header("Authorization", tokenB))
            .andExpect(status().isForbidden());

        // Get Asset A by ID by User B -> 403
        mockMvc.perform(get("/api/assets/" + assetAId)
                .header("Authorization", tokenB))
            .andExpect(status().isForbidden());

        // Create Finding in App A by User B -> 403
        mockMvc.perform(post("/api/findings")
                .header("Authorization", tokenB)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new CreateFindingRequest(
                    appAId, null, "Rogue Finding", "desc", "/api", null, com.etra.cti.detection.Severity.LOW, new BigDecimal("0.5"), "MANUAL"
                ))))
            .andExpect(status().isForbidden());

        // Get Finding A by ID by User B -> 403
        mockMvc.perform(get("/api/findings/" + findingAId)
                .header("Authorization", tokenB))
            .andExpect(status().isForbidden());

        // Update Finding A status by User B -> 403
        mockMvc.perform(patch("/api/findings/" + findingAId + "/status")
                .header("Authorization", tokenB)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new UpdateStatusRequest(FindingStatus.FALSE_POSITIVE))))
            .andExpect(status().isForbidden());

        // Assign Finding A by User B -> 403
        mockMvc.perform(post("/api/findings/" + findingAId + "/assign")
                .header("Authorization", tokenB)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new AssignFindingRequest(UUID.randomUUID(), null))))
            .andExpect(status().isForbidden());

        // Override Priority on Finding A by User B -> 403
        mockMvc.perform(patch("/api/findings/" + findingAId + "/priority")
                .header("Authorization", tokenB)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new OverridePriorityRequest(com.etra.cti.detection.Severity.LOW, "Demoted by competitor"))))
            .andExpect(status().isForbidden());

        // Set Due Date on Finding A by User B -> 403
        mockMvc.perform(patch("/api/findings/" + findingAId + "/due-date")
                .header("Authorization", tokenB)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"dueDate\":\"2030-01-01T00:00:00Z\"}"))
            .andExpect(status().isForbidden());

        // Upload Evidence to Finding A by User B -> 403
        MockMultipartFile badFile = new MockMultipartFile("file", "evil.txt", "text/plain", "malicious payload".getBytes());
        mockMvc.perform(multipart("/api/findings/" + findingAId + "/evidence")
                .file(badFile)
                .header("Authorization", tokenB))
            .andExpect(status().isForbidden());

        // List Evidence of Finding A by User B -> 403
        mockMvc.perform(get("/api/findings/" + findingAId + "/evidence")
                .header("Authorization", tokenB))
            .andExpect(status().isForbidden());

        // Get Evidence A detail by User B -> 403
        mockMvc.perform(get("/api/evidence/" + evidenceAId)
                .header("Authorization", tokenB))
            .andExpect(status().isForbidden());

        // Verify Evidence A integrity by User B -> 403
        mockMvc.perform(get("/api/evidence/" + evidenceAId + "/verify")
                .header("Authorization", tokenB))
            .andExpect(status().isForbidden());

        // Add Evidence Note by User B -> 403
        mockMvc.perform(post("/api/evidence/" + evidenceAId + "/notes")
                .header("Authorization", tokenB)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"note\":\"Tampering attempt\"}"))
            .andExpect(status().isForbidden());

        // Create Investigation in App A by User B -> 403
        mockMvc.perform(post("/api/investigations")
                .header("Authorization", tokenB)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new CreateInvestigationRequest(
                    null, appAId, "Spy Investigation", "desc", com.etra.cti.investigation.InvestigationPriority.LOW
                ))))
            .andExpect(status().isForbidden());

        // Get Investigation A by ID by User B -> 403
        mockMvc.perform(get("/api/investigations/" + invAId)
                .header("Authorization", tokenB))
            .andExpect(status().isForbidden());

        // Update Investigation A status by User B -> 403
        mockMvc.perform(patch("/api/investigations/" + invAId + "/status")
                .header("Authorization", tokenB)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new UpdateInvestigationStatusRequest(com.etra.cti.investigation.InvestigationStatus.CLOSED))))
            .andExpect(status().isForbidden());

        // Add note to Investigation A by User B -> 403
        mockMvc.perform(post("/api/investigations/" + invAId + "/notes")
                .header("Authorization", tokenB)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new AddNoteRequest("Unauthorized note"))))
            .andExpect(status().isForbidden());

        // Link finding to Investigation A by User B -> 403
        mockMvc.perform(post("/api/investigations/" + invAId + "/findings/" + findingAId)
                .header("Authorization", tokenB))
            .andExpect(status().isForbidden());

        // List Alerts for App A by User B -> 403
        mockMvc.perform(get("/api/alerts?applicationId=" + appAId)
                .header("Authorization", tokenB))
            .andExpect(status().isForbidden());

        // Get Finding Report for Finding A by User B -> 403
        mockMvc.perform(get("/api/reports/finding/" + findingAId)
                .header("Authorization", tokenB))
            .andExpect(status().isForbidden());

        // ==================== UNAUTHENTICATED REQUESTS ====================
        mockMvc.perform(get("/api/findings/" + findingAId))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/investigations/" + invAId))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/assets/" + assetAId))
            .andExpect(status().isForbidden());

        // ==================== ADMIN PLATFORM ACCESS ====================
        mockMvc.perform(get("/api/findings/" + findingAId)
                .header("Authorization", adminToken))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/investigations/" + invAId)
                .header("Authorization", adminToken))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/assets/" + assetAId)
                .header("Authorization", adminToken))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/reports/finding/" + findingAId)
                .header("Authorization", adminToken))
            .andExpect(status().isOk());
    }
}
