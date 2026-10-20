package com.etra.cti.config;

import com.etra.cti.application.Application;
import com.etra.cti.application.ApplicationRepository;
import com.etra.cti.application.ApplicationType;
import com.etra.cti.application.Criticality;
import com.etra.cti.application.Environment;
import com.etra.cti.asset.Asset;
import com.etra.cti.asset.AssetRepository;
import com.etra.cti.asset.AssetType;
import com.etra.cti.detection.Severity;
import com.etra.cti.event.EventService;
import com.etra.cti.event.EventSource;
import com.etra.cti.event.SecurityEventRepository;
import com.etra.cti.event.dto.IngestEventRequest;
import com.etra.cti.finding.FindingRepository;
import com.etra.cti.finding.FindingService;
import com.etra.cti.finding.dto.CreateFindingRequest;
import com.etra.cti.organization.Organization;
import com.etra.cti.organization.OrganizationMember;
import com.etra.cti.organization.OrganizationMemberRepository;
import com.etra.cti.organization.OrganizationRepository;
import com.etra.cti.project.Project;
import com.etra.cti.project.ProjectRepository;
import com.etra.cti.threatintel.Ioc;
import com.etra.cti.threatintel.IocRepository;
import com.etra.cti.threatintel.IocType;
import com.etra.cti.user.User;
import com.etra.cti.user.UserRepository;
import com.etra.cti.user.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final OrganizationRepository orgs;
    private final OrganizationMemberRepository members;
    private final ProjectRepository projects;
    private final ApplicationRepository apps;
    private final AssetRepository assets;
    private final IocRepository iocs;
    private final EventService eventService;
    private final SecurityEventRepository events;
    private final FindingService findingService;
    private final FindingRepository findings;

    public DataInitializer(UserRepository users,
                           PasswordEncoder encoder,
                           OrganizationRepository orgs,
                           OrganizationMemberRepository members,
                           ProjectRepository projects,
                           ApplicationRepository apps,
                           AssetRepository assets,
                           IocRepository iocs,
                           EventService eventService,
                           SecurityEventRepository events,
                           FindingService findingService,
                           FindingRepository findings) {
        this.users = users;
        this.encoder = encoder;
        this.orgs = orgs;
        this.members = members;
        this.projects = projects;
        this.apps = apps;
        this.assets = assets;
        this.iocs = iocs;
        this.eventService = eventService;
        this.events = events;
        this.findingService = findingService;
        this.findings = findings;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Checking ETRA-CTI system initialization state...");

        User admin = initUser("admin@etra.et", "Admin1234!", "Platform Administrator", UserRole.ADMIN);
        User analyst = initUser("analyst@etra.et", "Analyst1234!", "Senior Security Analyst", UserRole.ANALYST);
        User manager = initUser("manager@etra.et", "Manager1234!", "Security Operations Manager", UserRole.SECURITY_MANAGER);
        User pentester = initUser("pentester@etra.et", "Pentest1234!", "Senior Penetration Tester", UserRole.PENTESTER);
        User developer = initUser("developer@etra.et", "Dev123456!", "Remediation Developer", UserRole.DEVELOPER);
        User auditor = initUser("auditor@etra.et", "Audit1234!", "Compliance Auditor", UserRole.VIEWER);

        Organization org = initOrganization(admin, analyst, manager, pentester, developer, auditor);
        Project project = initProject(org);
        Application bankingApp = initApplicationsAndAssets(project, admin);

        initThreatIntel(admin);
        initSyntheticEventsAndFindings(bankingApp, analyst);

        log.info("ETRA-CTI core features and baseline datasets successfully initialized.");
    }

    private User initUser(String email, String rawPassword, String fullName, UserRole role) {
        return users.findByEmail(email.toLowerCase()).orElseGet(() -> {
            log.info("Initializing system user: {} ({})", email, role);
            User u = new User();
            u.setEmail(email.toLowerCase());
            u.setPasswordHash(encoder.encode(rawPassword));
            u.setFullName(fullName);
            u.setRole(role);
            u.setEnabled(true);
            return users.save(u);
        });
    }

    private Organization initOrganization(User admin, User analyst, User manager, User pentester, User developer, User auditor) {
        final String slug = "etra-secops";
        Organization org = orgs.findBySlug(slug).orElseGet(() -> {
            log.info("Initializing baseline organization: ETRA Security Operations");
            Organization o = new Organization();
            o.setName("ETRA Security Operations");
            o.setSlug(slug);
            return orgs.save(o);
        });

        addMember(org, admin, UserRole.ADMIN);
        addMember(org, manager, UserRole.SECURITY_MANAGER);
        addMember(org, analyst, UserRole.ANALYST);
        addMember(org, pentester, UserRole.PENTESTER);
        addMember(org, developer, UserRole.DEVELOPER);
        addMember(org, auditor, UserRole.VIEWER);
        return org;
    }

    private void addMember(Organization org, User user, UserRole role) {
        if (members.findByOrganizationIdAndUserId(org.getId(), user.getId()).isEmpty()) {
            OrganizationMember m = new OrganizationMember();
            m.setOrganizationId(org.getId());
            m.setUserId(user.getId());
            m.setRole(role);
            members.save(m);
        }
    }

    private Project initProject(Organization org) {
        List<Project> existing = projects.findByOrganizationId(org.getId());
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        log.info("Initializing primary project: National Critical Infrastructure Security");
        Project p = new Project();
        p.setOrganizationId(org.getId());
        p.setName("National Critical Infrastructure Assessment");
        p.setDescription("Continuous threat intelligence, telemetry analysis, and vulnerability lifecycle for national applications");
        return projects.save(p);
    }

    private Application initApplicationsAndAssets(Project project, User admin) {
        List<Application> existing = apps.findByProjectId(project.getId());
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        log.info("Initializing primary application: Commercial Banking Portal");
        Application app = new Application();
        app.setProjectId(project.getId());
        app.setName("Commercial Banking Portal");
        app.setType(ApplicationType.WEB);
        app.setEnvironment(Environment.PRODUCTION);
        app.setCriticality(Criticality.CRITICAL);
        app.setBaseUrl("https://bank.etra.et");
        app.setOwnerId(admin.getId());
        Application savedApp = apps.save(app);

        // Assets
        Asset asset1 = new Asset();
        asset1.setApplicationId(savedApp.getId());
        asset1.setType(AssetType.URL);
        asset1.setHostname("bank.etra.et");
        asset1.setUrl("https://bank.etra.et/api/v1/transfer");
        asset1.setPort(443);
        asset1.setProtocol("HTTPS");
        asset1.setEnvironment("PRODUCTION");
        asset1.setCriticality("CRITICAL");
        asset1.setStatus("ACTIVE");
        assets.save(asset1);

        Asset asset2 = new Asset();
        asset2.setApplicationId(savedApp.getId());
        asset2.setType(AssetType.DOMAIN);
        asset2.setHostname("bank.etra.et");
        asset2.setPort(443);
        asset2.setProtocol("HTTPS");
        asset2.setEnvironment("PRODUCTION");
        asset2.setCriticality("CRITICAL");
        asset2.setStatus("ACTIVE");
        assets.save(asset2);

        return savedApp;
    }

    private void initThreatIntel(User admin) {
        if (iocs.count() > 0) return;
        log.info("Initializing baseline threat intelligence indicators of compromise (IOCs)...");

        Ioc ioc1 = new Ioc();
        ioc1.setType(IocType.IP);
        ioc1.setValue("198.51.100.24");
        ioc1.setDescription("Observed credential brute-forcing and automated directory enumeration source");
        ioc1.setSource("ETRA Threat Intelligence Core");
        ioc1.setSeverity(Severity.HIGH);
        ioc1.setActive(true);
        ioc1.setCreatedBy(admin.getId());
        iocs.save(ioc1);

        Ioc ioc2 = new Ioc();
        ioc2.setType(IocType.DOMAIN);
        ioc2.setValue("c2.apt-monitoring.et");
        ioc2.setDescription("Command and control delivery domain identified in targeted spearphishing telemetry");
        ioc2.setSource("National CERT Advisory");
        ioc2.setSeverity(Severity.CRITICAL);
        ioc2.setActive(true);
        ioc2.setCreatedBy(admin.getId());
        iocs.save(ioc2);

        Ioc ioc3 = new Ioc();
        ioc3.setType(IocType.SHA256);
        ioc3.setValue("a7f9b8c3d2e1f0a9b8c7d6e5f4a3b2c1d0e9f8a7b6c5d4e3f2a1b0c9d8e7f6a5");
        ioc3.setDescription("Trojanized financial mobile utility APK hash");
        ioc3.setSource("Mobile Malware Analysis Feed");
        ioc3.setSeverity(Severity.HIGH);
        ioc3.setActive(true);
        ioc3.setCreatedBy(admin.getId());
        iocs.save(ioc3);
    }

    private void initSyntheticEventsAndFindings(Application app, User analyst) {
        if (events.count() > 0) return;
        log.info("Initializing synthetic telemetry events, detections, alerts, and findings...");

        Instant now = Instant.now();

        // 1. Burst of failed logins (triggers AuthFailureBurstRule)
        for (int i = 0; i < 6; i++) {
            eventService.ingest(new IngestEventRequest(
                app.getId(),
                EventSource.APPLICATION_LOG,
                now.minus(120 - (i * 10), ChronoUnit.SECONDS),
                "198.51.100.24",
                "POST",
                "/api/v1/auth/login",
                401,
                "actor_brute",
                Map.of("eventType", "AUTH_LOGIN_FAILURE", "message", "Invalid password attempt")
            ));
        }

        // 2. SQL injection attempt (triggers SqlInjectionRule)
        eventService.ingest(new IngestEventRequest(
            app.getId(),
            EventSource.APPLICATION_LOG,
            now.minus(45, ChronoUnit.SECONDS),
            "198.51.100.24",
            "GET",
            "/api/v1/transfer?account=101'%20OR%201=1--",
            500,
            "actor_brute",
            Map.of("parameter", "account", "query", "account=101' OR 1=1--", "message", "SQL execution error in bank transfer")
        ));

        // 3. Probing sensitive administrative endpoints (triggers SuspiciousApiProbingRule)
        eventService.ingest(new IngestEventRequest(
            app.getId(),
            EventSource.APPLICATION_LOG,
            now.minus(20, ChronoUnit.SECONDS),
            "198.51.100.24",
            "GET",
            "/actuator/env",
            403,
            "actor_brute",
            Map.of("message", "Forbidden access to management actuator")
        ));

        // 4. Initial Finding with auto-remediation
        if (findings.count() == 0) {
            findingService.create(new CreateFindingRequest(
                app.getId(),
                null,
                "SQL Injection in Fund Transfer Endpoint",
                "Input parameter 'account' in transfer API is susceptible to boolean-based SQL injection, enabling unauthorized ledger enumeration.",
                "/api/v1/transfer",
                "account",
                Severity.HIGH,
                new BigDecimal("0.90"),
                "APPLICATION_LOG"
            ), analyst.getId());
        }
    }
}
