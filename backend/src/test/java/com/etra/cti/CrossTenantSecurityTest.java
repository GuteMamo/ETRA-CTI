package com.etra.cti;

import com.etra.cti.auth.dto.LoginRequest;
import com.etra.cti.auth.dto.RegisterRequest;
import com.etra.cti.auth.dto.TokenPair;
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
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @Test
    @DisplayName("Cross-tenant isolation: User A cannot access or mutate resources in Org B")
    void testCrossTenantIsolation() throws Exception {
        String runId = UUID.randomUUID().toString().substring(0, 8);

        // Register User A
        String emailA = "usera_" + runId + "@tenant-a.com";
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new RegisterRequest(emailA, "Pass1234!", "User A"))))
            .andExpect(status().isCreated());

        MvcResult loginA = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new LoginRequest(emailA, "Pass1234!"))))
            .andExpect(status().isOk())
            .andReturn();
        String tokenA = "Bearer " + mapper.readValue(loginA.getResponse().getContentAsString(), TokenPair.class).accessToken();

        // Register User B
        String emailB = "userb_" + runId + "@tenant-b.com";
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new RegisterRequest(emailB, "Pass1234!", "User B"))))
            .andExpect(status().isCreated());

        MvcResult loginB = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new LoginRequest(emailB, "Pass1234!"))))
            .andExpect(status().isOk())
            .andReturn();
        String tokenB = "Bearer " + mapper.readValue(loginB.getResponse().getContentAsString(), TokenPair.class).accessToken();

        // User A creates Org A
        MvcResult orgAResult = mockMvc.perform(post("/api/organizations")
                .header("Authorization", tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new CreateOrganizationRequest("Tenant A Org " + runId, "tenant-a-" + runId))))
            .andExpect(status().isCreated())
            .andReturn();
        OrganizationDto orgA = mapper.readValue(orgAResult.getResponse().getContentAsString(), OrganizationDto.class);

        // User A creates Project A in Org A
        MvcResult projAResult = mockMvc.perform(post("/api/organizations/" + orgA.id() + "/projects")
                .header("Authorization", tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new CreateProjectRequest("Project A", "Secret Project"))))
            .andExpect(status().isCreated())
            .andReturn();
        ProjectDto projA = mapper.readValue(projAResult.getResponse().getContentAsString(), ProjectDto.class);

        // User B attempts to access Org A -> Forbidden (403)
        mockMvc.perform(get("/api/organizations/" + orgA.id())
                .header("Authorization", tokenB))
            .andExpect(status().isForbidden());

        // User B attempts to access Project A -> Forbidden (403)
        mockMvc.perform(get("/api/projects/" + projA.id())
                .header("Authorization", tokenB))
            .andExpect(status().isForbidden());

        // User B attempts to create application in Project A -> Forbidden (403)
        mockMvc.perform(post("/api/projects/" + projA.id() + "/applications")
                .header("Authorization", tokenB)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Hacked App\",\"type\":\"WEB\",\"environment\":\"DEV\",\"criticality\":\"LOW\"}"))
            .andExpect(status().isForbidden());
    }
}
