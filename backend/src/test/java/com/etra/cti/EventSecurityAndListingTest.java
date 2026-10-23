package com.etra.cti;

import com.etra.cti.application.ApplicationType;
import com.etra.cti.application.Criticality;
import com.etra.cti.application.Environment;
import com.etra.cti.application.apikey.dto.ApiKeyCreatedResponse;
import com.etra.cti.application.apikey.dto.CreateApiKeyRequest;
import com.etra.cti.application.dto.ApplicationDto;
import com.etra.cti.application.dto.CreateApplicationRequest;
import com.etra.cti.auth.dto.LoginRequest;
import com.etra.cti.auth.dto.RegisterRequest;
import com.etra.cti.auth.dto.TokenPair;
import com.etra.cti.event.EventSource;
import com.etra.cti.event.dto.EventDto;
import com.etra.cti.event.dto.IngestEventRequest;
import com.etra.cti.organization.dto.AddMemberRequest;
import com.etra.cti.organization.dto.CreateOrganizationRequest;
import com.etra.cti.organization.dto.OrganizationDto;
import com.etra.cti.project.dto.CreateProjectRequest;
import com.etra.cti.project.dto.ProjectDto;
import com.etra.cti.user.UserRole;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class EventSecurityAndListingTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();
    }

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
    @DisplayName("Verify event ingestion, persistence, scoped listing, RBAC, tenant isolation, and machine key restrictions")
    void testEventLifecycleListingAndSecurityIsolation() throws Exception {
        String runId = UUID.randomUUID().toString().substring(0, 8);

        // 1. Create Org A, Project A, Application A under Admin A
        String adminToken = registerAndLogin("admin_" + runId + "@tenant-a.com", "AdminPass123!", "Admin " + runId, UserRole.ADMIN);

        MvcResult orgARes = mockMvc.perform(post("/api/organizations")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new CreateOrganizationRequest("Org A " + runId, "org-a-" + runId))))
            .andExpect(status().isCreated())
            .andReturn();
        OrganizationDto orgA = mapper.readValue(orgARes.getResponse().getContentAsString(), OrganizationDto.class);

        MvcResult projARes = mockMvc.perform(post("/api/organizations/" + orgA.id() + "/projects")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new CreateProjectRequest("Project A", "Desc"))))
            .andExpect(status().isCreated())
            .andReturn();
        ProjectDto projA = mapper.readValue(projARes.getResponse().getContentAsString(), ProjectDto.class);

        MvcResult appARes = mockMvc.perform(post("/api/projects/" + projA.id() + "/applications")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new CreateApplicationRequest(
                    "Banking API " + runId, ApplicationType.API, Environment.PRODUCTION, Criticality.HIGH, "https://bank.et"))))
            .andExpect(status().isCreated())
            .andReturn();
        ApplicationDto appA = mapper.readValue(appARes.getResponse().getContentAsString(), ApplicationDto.class);

        // 2. Issue API Key for Application A
        MvcResult keyRes = mockMvc.perform(post("/api/applications/" + appA.id() + "/api-keys")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new CreateApiKeyRequest("Ingestion Key", 30))))
            .andExpect(status().isCreated())
            .andReturn();
        ApiKeyCreatedResponse keyData = mapper.readValue(keyRes.getResponse().getContentAsString(), ApiKeyCreatedResponse.class);
        String rawApiKey = keyData.rawApiKey();

        // 3. Ingest Test Security Event via POST /api/events with X-ETRA-API-KEY
        IngestEventRequest ingestReq = new IngestEventRequest(
            appA.id(),
            EventSource.APPLICATION_LOG,
            Instant.parse("2026-10-21T19:27:59.536494Z"),
            "192.0.2.25",
            "POST",
            "/login",
            401,
            "test-user",
            Map.of("authMechanism", "PASSWORD", "failureReason", "INVALID_PASSWORD", "testEvent", true)
        );

        MvcResult ingestRes = mockMvc.perform(post("/api/events")
                .header("X-ETRA-API-KEY", rawApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(ingestReq)))
            .andExpect(status().isCreated())
            .andReturn();

        EventDto createdEvent = mapper.readValue(ingestRes.getResponse().getContentAsString(), EventDto.class);
        assertNotNull(createdEvent.id());
        assertEquals(appA.id(), createdEvent.applicationId());
        assertEquals("POST", createdEvent.method());
        assertEquals("/login", createdEvent.path());
        assertEquals(401, createdEvent.statusCode());
        assertEquals("192.0.2.25", createdEvent.sourceIp());
        assertEquals("test-user", createdEvent.userIdentifier());

        // 4. Verify user-facing event-list API: GET /api/applications/{appId}/events
        MvcResult listForAppRes = mockMvc.perform(get("/api/applications/" + appA.id() + "/events")
                .header("Authorization", adminToken))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode listForAppJson = mapper.readTree(listForAppRes.getResponse().getContentAsString());
        assertTrue(listForAppJson.get("totalElements").asInt() >= 1);
        assertEquals(createdEvent.id().toString(), listForAppJson.get("content").get(0).get("id").asText());

        // 5. Verify user-facing event-list API: GET /api/events?applicationId={appId}
        MvcResult listEventsRes = mockMvc.perform(get("/api/events")
                .param("applicationId", appA.id().toString())
                .header("Authorization", adminToken))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode listEventsJson = mapper.readTree(listEventsRes.getResponse().getContentAsString());
        assertTrue(listEventsJson.get("totalElements").asInt() >= 1);
        assertEquals(createdEvent.id().toString(), listEventsJson.get("content").get(0).get("id").asText());

        // 6. Verify single event retrieval: GET /api/events/{id}
        mockMvc.perform(get("/api/events/" + createdEvent.id())
                .header("Authorization", adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(createdEvent.id().toString()))
            .andExpect(jsonPath("$.applicationId").value(appA.id().toString()))
            .andExpect(jsonPath("$.statusCode").value(401));

        // 7. Test RBAC: Register a VIEWER user, add to Org A, and verify they CAN list events
        String viewerEmail = "viewer_" + runId + "@tenant-a.com";
        String viewerToken = registerAndLogin(viewerEmail, "ViewerPass123!", "Viewer " + runId, UserRole.VIEWER);

        // Get viewer user ID
        MvcResult whoamiRes = mockMvc.perform(get("/api/auth/me")
                .header("Authorization", viewerToken))
            .andExpect(status().isOk())
            .andReturn();
        UUID viewerId = UUID.fromString(mapper.readTree(whoamiRes.getResponse().getContentAsString()).get("id").asText());

        // Add viewer to Org A
        mockMvc.perform(post("/api/organizations/" + orgA.id() + "/members")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new AddMemberRequest(viewerId, UserRole.VIEWER))))
            .andExpect(status().isNoContent());

        // Viewer can list events for Org A's application
        mockMvc.perform(get("/api/applications/" + appA.id() + "/events")
                .header("Authorization", viewerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1));

        // 8. Test Machine Credentials Restriction:
        // Application API Key MUST NOT be able to call GET endpoints (listing or dashboard)
        mockMvc.perform(get("/api/events")
                .header("X-ETRA-API-KEY", rawApiKey))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/applications/" + appA.id() + "/events")
                .header("X-ETRA-API-KEY", rawApiKey))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/events/" + createdEvent.id())
                .header("X-ETRA-API-KEY", rawApiKey))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/events")
                .header("Authorization", "Bearer " + rawApiKey))
            .andExpect(status().isForbidden());

        // 9. Test Cross-Tenant Isolation:
        // User B belongs to Tenant B. Attempts to list or get events for App A must be 403 Forbidden!
        String userBToken = registerAndLogin("userb_" + runId + "@foreign-tenant.com", "UserBPass123!", "User B " + runId, UserRole.ANALYST);

        // User B attempts to access App A's events -> 403 Forbidden
        mockMvc.perform(get("/api/applications/" + appA.id() + "/events")
                .header("Authorization", userBToken))
            .andExpect(status().isForbidden());

        // User B attempts to get App A's event by ID -> 403 Forbidden
        mockMvc.perform(get("/api/events/" + createdEvent.id())
                .header("Authorization", userBToken))
            .andExpect(status().isForbidden());

        // User B attempts to list with param applicationId=AppA -> 403 Forbidden
        mockMvc.perform(get("/api/events")
                .param("applicationId", appA.id().toString())
                .header("Authorization", userBToken))
            .andExpect(status().isForbidden());

        // User B lists all events -> receives only their own organization's events (empty in this case, 0 events)
        MvcResult userBAllEvents = mockMvc.perform(get("/api/events")
                .header("Authorization", userBToken))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode userBJson = mapper.readTree(userBAllEvents.getResponse().getContentAsString());
        assertEquals(0, userBJson.get("totalElements").asInt());

        // 10. Test Unauthenticated access -> 401/403
        mockMvc.perform(get("/api/applications/" + appA.id() + "/events"))
            .andExpect(status().isForbidden()); // Spring Security 403 for anonymous when denied
    }
}
