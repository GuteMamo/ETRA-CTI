package com.etra.cti;

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
import com.etra.cti.organization.dto.CreateOrganizationRequest;
import com.etra.cti.organization.dto.OrganizationDto;
import com.etra.cti.project.dto.CreateProjectRequest;
import com.etra.cti.project.dto.ProjectDto;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class ApiKeyIntegrationTest {

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

    @Test
    @DisplayName("Application API Key lifecycle: create, M2M ingestion, BOLA/IDOR protection, revocation")
    void testApiKeyLifecycleAndM2MIngest() throws Exception {
        String runId = UUID.randomUUID().toString().substring(0, 8);

        // 1. Register & Login as Admin
        String email = "admin_" + runId + "@etra.et";
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new RegisterRequest(email, "SecureAdmin123!", "Admin " + runId, com.etra.cti.user.UserRole.ADMIN))))
            .andExpect(status().isCreated());

        MvcResult loginRes = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new LoginRequest(email, "SecureAdmin123!"))))
            .andExpect(status().isOk())
            .andReturn();
        String token = "Bearer " + mapper.readValue(loginRes.getResponse().getContentAsString(), TokenPair.class).accessToken();

        // 2. Create Org, Project, and two Applications (App A and App B)
        MvcResult orgRes = mockMvc.perform(post("/api/organizations")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new CreateOrganizationRequest("Org " + runId, "org-" + runId))))
            .andExpect(status().isCreated())
            .andReturn();
        OrganizationDto org = mapper.readValue(orgRes.getResponse().getContentAsString(), OrganizationDto.class);

        MvcResult projRes = mockMvc.perform(post("/api/organizations/" + org.id() + "/projects")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new CreateProjectRequest("Project " + runId, "Test Project"))))
            .andExpect(status().isCreated())
            .andReturn();
        ProjectDto proj = mapper.readValue(projRes.getResponse().getContentAsString(), ProjectDto.class);

        // Application A
        MvcResult appResA = mockMvc.perform(post("/api/projects/" + proj.id() + "/applications")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new CreateApplicationRequest(
                    "App A", com.etra.cti.application.ApplicationType.API,
                    com.etra.cti.application.Environment.PRODUCTION,
                    com.etra.cti.application.Criticality.HIGH, "https://api-a.etra.et"))))
            .andExpect(status().isCreated())
            .andReturn();
        ApplicationDto appA = mapper.readValue(appResA.getResponse().getContentAsString(), ApplicationDto.class);

        // Application B
        MvcResult appResB = mockMvc.perform(post("/api/projects/" + proj.id() + "/applications")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new CreateApplicationRequest(
                    "App B", com.etra.cti.application.ApplicationType.WEB,
                    com.etra.cti.application.Environment.DEV,
                    com.etra.cti.application.Criticality.LOW, "https://web-b.etra.et"))))
            .andExpect(status().isCreated())
            .andReturn();
        ApplicationDto appB = mapper.readValue(appResB.getResponse().getContentAsString(), ApplicationDto.class);

        // 3. Issue API Key for Application A
        CreateApiKeyRequest keyReq = new CreateApiKeyRequest("Ingestion Key A", 30);
        MvcResult keyRes = mockMvc.perform(post("/api/applications/" + appA.id() + "/api-keys")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(keyReq)))
            .andExpect(status().isCreated())
            .andReturn();
        ApiKeyCreatedResponse keyData = mapper.readValue(keyRes.getResponse().getContentAsString(), ApiKeyCreatedResponse.class);

        assertNotNull(keyData.rawApiKey());
        assertTrue(keyData.rawApiKey().startsWith("etra_live_"));
        assertEquals("Ingestion Key A", keyData.name());

        // 4. M2M Event Ingestion using X-ETRA-API-KEY header
        IngestEventRequest eventA = new IngestEventRequest(
            appA.id(),
            EventSource.API_GATEWAY,
            Instant.now(),
            "198.51.100.10",
            "POST",
            "/v1/payments",
            200,
            "m2m-client",
            Map.of("transactionId", "tx-999")
        );
        MvcResult ingestResA = mockMvc.perform(post("/api/events")
                .header("X-ETRA-API-KEY", keyData.rawApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(eventA)))
            .andExpect(status().isCreated())
            .andReturn();
        EventDto savedEventA = mapper.readValue(ingestResA.getResponse().getContentAsString(), EventDto.class);
        assertEquals(appA.id(), savedEventA.applicationId());

        // 5. M2M Event Ingestion using Authorization: Bearer <rawApiKey>
        mockMvc.perform(post("/api/events")
                .header("Authorization", "Bearer " + keyData.rawApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(eventA)))
            .andExpect(status().isCreated());

        // 6. Test BOLA/IDOR: Application A's API Key attempts to ingest for Application B -> 403 Forbidden!
        IngestEventRequest bolaEvent = new IngestEventRequest(
            appB.id(),
            EventSource.API_GATEWAY,
            Instant.now(),
            "198.51.100.10",
            "POST",
            "/v1/users",
            200,
            "attacker",
            Map.of()
        );
        mockMvc.perform(post("/api/events")
                .header("X-ETRA-API-KEY", keyData.rawApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(bolaEvent)))
            .andExpect(status().isForbidden());

        // 7. Revoke API Key
        mockMvc.perform(post("/api/applications/" + appA.id() + "/api-keys/" + keyData.id() + "/revoke")
                .header("Authorization", token))
            .andExpect(status().isOk());

        // 8. Try ingesting with revoked API Key -> 401 or 403 Unauthorized
        mockMvc.perform(post("/api/events")
                .header("X-ETRA-API-KEY", keyData.rawApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(eventA)))
            .andExpect(status().isForbidden());
    }
}
