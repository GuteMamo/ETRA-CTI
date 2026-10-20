package com.etra.cti;

import com.etra.cti.application.dto.ApplicationDto;
import com.etra.cti.application.dto.CreateApplicationRequest;
import com.etra.cti.auth.dto.LoginRequest;
import com.etra.cti.auth.dto.RegisterRequest;
import com.etra.cti.auth.dto.TokenPair;
import com.etra.cti.integration.dto.ImportDto;
import com.etra.cti.organization.dto.CreateOrganizationRequest;
import com.etra.cti.organization.dto.OrganizationDto;
import com.etra.cti.project.dto.CreateProjectRequest;
import com.etra.cti.project.dto.ProjectDto;
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class ZapScannerImportTest {

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
    @DisplayName("OWASP ZAP Import: parse, map findings, deduplicate, and enforce tenant isolation")
    void testZapScannerImportAndTenantIsolation() throws Exception {
        String runId = UUID.randomUUID().toString().substring(0, 8);

        // 1. Register & Login User A (Tenant A)
        String emailA = "sec_a_" + runId + "@tenant-a.com";
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new RegisterRequest(emailA, "Password123!", "Sec A"))))
            .andExpect(status().isCreated());

        MvcResult loginA = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new LoginRequest(emailA, "Password123!"))))
            .andExpect(status().isOk())
            .andReturn();
        String tokenA = "Bearer " + mapper.readValue(loginA.getResponse().getContentAsString(), TokenPair.class).accessToken();

        // Register User B (Tenant B)
        String emailB = "sec_b_" + runId + "@tenant-b.com";
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new RegisterRequest(emailB, "Password123!", "Sec B"))))
            .andExpect(status().isCreated());

        MvcResult loginB = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new LoginRequest(emailB, "Password123!"))))
            .andExpect(status().isOk())
            .andReturn();
        String tokenB = "Bearer " + mapper.readValue(loginB.getResponse().getContentAsString(), TokenPair.class).accessToken();

        // 2. Setup Tenant A Organization, Project, Application
        MvcResult orgRes = mockMvc.perform(post("/api/organizations")
                .header("Authorization", tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new CreateOrganizationRequest("Org A " + runId, "org-a-" + runId))))
            .andExpect(status().isCreated())
            .andReturn();
        OrganizationDto orgA = mapper.readValue(orgRes.getResponse().getContentAsString(), OrganizationDto.class);

        MvcResult projRes = mockMvc.perform(post("/api/organizations/" + orgA.id() + "/projects")
                .header("Authorization", tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new CreateProjectRequest("Project A", "Banking Core"))))
            .andExpect(status().isCreated())
            .andReturn();
        ProjectDto projA = mapper.readValue(projRes.getResponse().getContentAsString(), ProjectDto.class);

        MvcResult appRes = mockMvc.perform(post("/api/projects/" + projA.id() + "/applications")
                .header("Authorization", tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new CreateApplicationRequest(
                    "Banking Portal A", com.etra.cti.application.ApplicationType.WEB,
                    com.etra.cti.application.Environment.PRODUCTION,
                    com.etra.cti.application.Criticality.CRITICAL, "https://bank-a.etra.et"))))
            .andExpect(status().isCreated())
            .andReturn();
        ApplicationDto appA = mapper.readValue(appRes.getResponse().getContentAsString(), ApplicationDto.class);

        // 3. Load ZAP JSON sample
        byte[] zapBytes = Files.readAllBytes(Path.of("C:/dev/etra-cti/zap-sample.json"));
        MockMultipartFile zapFile = new MockMultipartFile(
            "file", "zap-sample.json", "application/json", zapBytes
        );

        // 4. Test Cross-Tenant Unauthorized Import: User B attempts to import into Tenant A's app -> 403 Forbidden!
        mockMvc.perform(multipart("/api/integrations/zap/" + appA.id())
                .file(zapFile)
                .header("Authorization", tokenB))
            .andExpect(status().isForbidden());

        // 5. User A imports ZAP report -> SUCCESS
        MvcResult importRes = mockMvc.perform(multipart("/api/integrations/zap/" + appA.id())
                .file(zapFile)
                .header("Authorization", tokenA))
            .andExpect(status().isCreated())
            .andReturn();

        ImportDto importData = mapper.readValue(importRes.getResponse().getContentAsString(), ImportDto.class);
        assertEquals(com.etra.cti.integration.ImportStatus.COMPLETED, importData.status());
        assertEquals(4, importData.findingsCreated());
        assertEquals(0, importData.findingsDuplicated());

        // 6. Verify Findings in system for Application A
        MvcResult findingsRes = mockMvc.perform(get("/api/findings?page=0&size=10")
                .header("Authorization", tokenA))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode findingsNode = mapper.readTree(findingsRes.getResponse().getContentAsString());
        assertTrue(findingsNode.get("totalElements").asInt() >= 4);

        // 7. Test Deduplication: Import the exact same ZAP report a second time
        MockMultipartFile zapFileDuplicate = new MockMultipartFile(
            "file", "zap-sample.json", "application/json", zapBytes
        );
        MvcResult dupRes = mockMvc.perform(multipart("/api/integrations/zap/" + appA.id())
                .file(zapFileDuplicate)
                .header("Authorization", tokenA))
            .andExpect(status().isCreated())
            .andReturn();

        ImportDto dupData = mapper.readValue(dupRes.getResponse().getContentAsString(), ImportDto.class);
        assertEquals(com.etra.cti.integration.ImportStatus.COMPLETED, dupData.status());
        assertEquals(0, dupData.findingsCreated());
        assertEquals(4, dupData.findingsDuplicated());
    }
}
