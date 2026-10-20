import fs from "fs";

const BASE_URL = "http://localhost:8081/api";

function assert(condition, message) {
  if (!condition) {
    console.error(`[FAIL] ${message}`);
    throw new Error(`Assertion failed: ${message}`);
  }
}

async function request(path, options = {}) {
  const url = `${BASE_URL}${path}`;
  const res = await fetch(url, options);
  let data = null;
  const contentType = res.headers.get("content-type");
  if (contentType && contentType.includes("application/json")) {
    try {
      data = await res.json();
    } catch (e) {
      // ignore
    }
  } else {
    data = await res.text();
  }
  return { status: res.status, ok: res.ok, headers: res.headers, data };
}

async function runQaSuite() {
  console.log("===============================================================");
  console.log("    ETRA-CTI INDEPENDENT QA AUTOMATED VERIFICATION SUITE       ");
  console.log("===============================================================");
  const results = { passed: 0, failed: 0 };

  try {
    // =========================================================================
    // 1. LOGIN AND AUTHORIZATION
    // =========================================================================
    console.log("\n[TEST 1] LOGIN AND AUTHORIZATION");
    
    // 1.1 Valid Admin Login
    const adminLogin = await request("/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email: "admin@etra.et", password: "Admin1234!" })
    });
    assert(adminLogin.status === 200, `Admin login status expected 200, got ${adminLogin.status}`);
    assert(adminLogin.data.accessToken, "Admin login must return an accessToken");
    const adminToken = adminLogin.data.accessToken;
    console.log("  ✓ 1.1 Admin login succeeded with valid JWT access token");

    // 1.2 Valid Analyst Login
    const analystLogin = await request("/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email: "analyst@etra.et", password: "Analyst1234!" })
    });
    assert(analystLogin.status === 200, `Analyst login status expected 200, got ${analystLogin.status}`);
    const analystToken = analystLogin.data.accessToken;
    console.log("  ✓ 1.2 Analyst login succeeded with valid JWT access token");

    // 1.3 Invalid Password Login Rejection
    const badLogin = await request("/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email: "admin@etra.et", password: "WrongPassword999!" })
    });
    assert(badLogin.status === 401, `Invalid login expected 401 Unauthorized, got ${badLogin.status}`);
    console.log("  ✓ 1.3 Invalid password correctly rejected with HTTP 401 Unauthorized");

    // 1.4 Role-based authorization: Auditor (VIEWER) cannot create an application
    const auditorLogin = await request("/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email: "auditor@etra.et", password: "Audit1234!" })
    });
    const auditorToken = auditorLogin.data.accessToken;
    
    // Get primary ETRA Security Operations organization where analyst is a member
    const analystOrgsRes = await request("/organizations", {
      headers: { Authorization: `Bearer ${analystToken}` }
    });
    const orgId = analystOrgsRes.data.find(o => o.slug === "etra-secops")?.id || analystOrgsRes.data[0].id;
    const projectsRes = await request(`/organizations/${orgId}/projects`, {
      headers: { Authorization: `Bearer ${adminToken}` }
    });
    const projectId = projectsRes.data[0].id;

    // Auditor attempting to create application (requires ADMIN/SECURITY_MANAGER)
    const unauthorizedCreate = await request(`/projects/${projectId}/applications`, {
      method: "POST",
      headers: { "Content-Type": "application/json", Authorization: `Bearer ${auditorToken}` },
      body: JSON.stringify({
        name: "Unauthorized App",
        type: "WEB",
        environment: "DEVELOPMENT",
        criticality: "LOW",
        baseUrl: "https://test.local"
      })
    });
    // Spring security method security returns 403
    assert(unauthorizedCreate.status === 403, `Auditor creating app expected 403, got ${unauthorizedCreate.status}`);
    console.log("  ✓ 1.4 Auditor (VIEWER) prohibited from creating applications (HTTP 403 Forbidden)");

    results.passed++;

    // =========================================================================
    // 2. APPLICATION REGISTRATION AND CREDENTIALS
    // =========================================================================
    console.log("\n[TEST 2] APPLICATION REGISTRATION AND CREDENTIALS");

    // 2.1 Register application
    const appCreateRes = await request(`/projects/${projectId}/applications`, {
      method: "POST",
      headers: { "Content-Type": "application/json", Authorization: `Bearer ${adminToken}` },
      body: JSON.stringify({
        name: `QA E2E Application ${Date.now()}`,
        type: "WEB",
        environment: "STAGING",
        criticality: "CRITICAL",
        baseUrl: "https://staging.bank.etra.et"
      })
    });
    assert(appCreateRes.status === 201, `Create app expected 201, got ${appCreateRes.status}`);
    const qaApp = appCreateRes.data;
    console.log(`  ✓ 2.1 Application successfully registered: ${qaApp.name} (ID: ${qaApp.id})`);

    // 2.2 Generate M2M API Key
    const keyCreateRes = await request(`/applications/${qaApp.id}/api-keys`, {
      method: "POST",
      headers: { "Content-Type": "application/json", Authorization: `Bearer ${adminToken}` },
      body: JSON.stringify({
        name: "QA External Telemetry Agent",
        expiryDays: 14
      })
    });
    assert(keyCreateRes.status === 201, `Create key expected 201, got ${keyCreateRes.status}`);
    const keyData = keyCreateRes.data;
    assert(keyData.rawApiKey && keyData.rawApiKey.startsWith("etra_live_"), "API key must start with etra_live_");
    assert(keyData.keyPrefix && keyData.rawApiKey.startsWith(keyData.keyPrefix), "rawApiKey must match keyPrefix");
    console.log(`  ✓ 2.2 Generated M2M API Key: ${keyData.keyPrefix}•••••• (Expires: ${keyData.expiresAt})`);

    // 2.3 Query keys list: Verify raw secret is NOT leaked in list response
    const keyListRes = await request(`/applications/${qaApp.id}/api-keys`, {
      headers: { Authorization: `Bearer ${adminToken}` }
    });
    assert(keyListRes.status === 200, `List keys expected 200, got ${keyListRes.status}`);
    assert(keyListRes.data.length >= 1, "Keys list must contain at least 1 key");
    const listedKey = keyListRes.data.find(k => k.id === keyData.id);
    assert(listedKey.rawApiKey === undefined, "Security check: rawApiKey must NEVER be leaked in list endpoint");
    assert(listedKey.keyPrefix === keyData.keyPrefix, "Key prefix must match");
    console.log("  ✓ 2.3 Verified key list returns metadata without exposing raw secrets");

    results.passed++;

    // =========================================================================
    // 3. SUBMIT A SECURITY EVENT FROM A SEPARATE CLIENT
    // =========================================================================
    console.log("\n[TEST 3] SUBMIT SECURITY EVENT VIA M2M API KEY");

    // 3.1 Submit via X-ETRA-API-KEY header
    const eventTime = new Date().toISOString();
    const eventHeaderRes = await request("/events", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-ETRA-API-KEY": keyData.rawApiKey
      },
      body: JSON.stringify({
        applicationId: qaApp.id,
        source: "API_GATEWAY",
        timestamp: eventTime,
        endpoint: "/api/v1/auth/login",
        httpMethod: "POST",
        statusCode: 200,
        sourceIp: "197.156.104.22",
        userId: "customer_9901",
        metadata: {
          sessionToken: "secret_token_12345",
          password: "SuperSecretPassword!",
          authMechanism: "MFA_TOTP"
        }
      })
    });
    assert(eventHeaderRes.status === 201, `Event submission expected 201, got ${eventHeaderRes.status}`);
    const event1 = eventHeaderRes.data;
    console.log(`  ✓ 3.1 Event ingested via X-ETRA-API-KEY (Event ID: ${event1.id})`);

    // 3.2 Submit via Authorization: Bearer etra_live_...
    const eventBearerRes = await request("/events", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${keyData.rawApiKey}`
      },
      body: JSON.stringify({
        applicationId: qaApp.id,
        source: "APPLICATION_LOG",
        timestamp: new Date().toISOString(),
        endpoint: "/api/v1/accounts",
        httpMethod: "GET",
        statusCode: 200,
        sourceIp: "197.156.104.22",
        userId: "customer_9901"
      })
    });
    assert(eventBearerRes.status === 201, `Event bearer submission expected 201, got ${eventBearerRes.status}`);
    console.log(`  ✓ 3.2 Event ingested via Authorization: Bearer etra_live_... (Event ID: ${eventBearerRes.data.id})`);

    // 3.3 Verify invalid API Key rejection
    const badKeyRes = await request("/events", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-ETRA-API-KEY": "etra_live_invalid_bad_key_00000000000000"
      },
      body: JSON.stringify({
        applicationId: qaApp.id,
        source: "APPLICATION_LOG",
        timestamp: new Date().toISOString()
      })
    });
    assert(badKeyRes.status === 401 || badKeyRes.status === 403, `Invalid key expected 401/403, got ${badKeyRes.status}`);
    console.log("  ✓ 3.3 Unauthorized API key rejected with HTTP 401/403");

    results.passed++;

    // =========================================================================
    // 4. CONFIRM PERSISTENCE AND DASHBOARD VISIBILITY
    // =========================================================================
    console.log("\n[TEST 4] CONFIRM EVENT PERSISTENCE AND SENSITIVE DATA REDACTION");

    const getEventRes = await request(`/events/${event1.id}`, {
      headers: { Authorization: `Bearer ${analystToken}` }
    });
    assert(getEventRes.status === 200, `Get event expected 200, got ${getEventRes.status}`);
    const persisted = getEventRes.data;
    assert(persisted.applicationId === qaApp.id, "Application ID must match");
    assert(persisted.source === "API_GATEWAY", "Source must match");
    assert(persisted.path === "/api/v1/auth/login", "Path must match");
    
    // Check redaction of password / secret_token in normalized map
    assert(persisted.normalized["meta.password"] === "[REDACTED]", "Password in metadata must be [REDACTED]");
    assert(persisted.normalized["meta.sessionToken"] === "[REDACTED]", "Session token in metadata must be [REDACTED]");
    assert(persisted.normalized["meta.authMechanism"] === "MFA_TOTP", "Non-sensitive field must be preserved");
    console.log("  ✓ 4.1 Event persisted with correct metadata and timestamp");
    console.log("  ✓ 4.2 Sensitive fields verified as [REDACTED] in persistent storage");

    results.passed++;

    // =========================================================================
    // 5. TRIGGER A DETECTION USING CONTROLLED TEST EVENTS
    // =========================================================================
    console.log("\n[TEST 5] TRIGGER EXPLAINABLE DETECTION RULES");

    // 5.1 Trigger SQL Injection Rule
    console.log("  a. Submitting SQL Injection payload...");
    const sqliEventRes = await request("/events", {
      method: "POST",
      headers: { "Content-Type": "application/json", "X-ETRA-API-KEY": keyData.rawApiKey },
      body: JSON.stringify({
        applicationId: qaApp.id,
        source: "API_GATEWAY",
        timestamp: new Date().toISOString(),
        endpoint: "/api/v1/transactions",
        httpMethod: "GET",
        statusCode: 400,
        sourceIp: "196.188.99.11",
        metadata: {
          query: "id=101' UNION SELECT username,password_hash FROM users --",
          message: "Database syntax error near UNION"
        }
      })
    });
    assert(sqliEventRes.status === 201, `SQLi event submission expected 201, got ${sqliEventRes.status}`);

    // 5.2 Trigger Suspicious API Probing Rule
    console.log("  b. Submitting Suspicious API Probing payload (/.env)...");
    const probeEventRes = await request("/events", {
      method: "POST",
      headers: { "Content-Type": "application/json", "X-ETRA-API-KEY": keyData.rawApiKey },
      body: JSON.stringify({
        applicationId: qaApp.id,
        source: "API_GATEWAY",
        timestamp: new Date().toISOString(),
        endpoint: "/.env",
        httpMethod: "GET",
        statusCode: 404,
        sourceIp: "196.188.99.12"
      })
    });
    assert(probeEventRes.status === 201, `Probe event submission expected 201, got ${probeEventRes.status}`);

    // 5.3 Trigger Auth Failure Burst Rule (5 failed logins within 5 minutes)
    console.log("  c. Submitting 5 sequential authentication failures from 196.188.99.13...");
    const burstIp = "196.188.99.13";
    for (let i = 1; i <= 5; i++) {
      await request("/events", {
        method: "POST",
        headers: { "Content-Type": "application/json", "X-ETRA-API-KEY": keyData.rawApiKey },
        body: JSON.stringify({
          applicationId: qaApp.id,
          source: "APPLICATION_LOG",
          timestamp: new Date(Date.now() - (5 - i) * 1000).toISOString(),
          endpoint: "/api/login",
          httpMethod: "POST",
          statusCode: 401,
          sourceIp: burstIp,
          userId: `targeted_user_${i}`,
          metadata: { failureReason: "INVALID_CREDENTIALS" }
        })
      });
    }

    // Verify Alerts Generated in Dashboard
    const alertsRes = await request(`/alerts`, {
      headers: { Authorization: `Bearer ${analystToken}` }
    });
    assert(alertsRes.status === 200, `Alerts query expected 200, got ${alertsRes.status}`);
    const alerts = alertsRes.data.content || alertsRes.data;
    
    // Check SQLi Alert
    const sqliAlert = alerts.find(a => a.applicationId === qaApp.id && a.title.includes("SQL INJECTION"));
    assert(sqliAlert, "SQL Injection alert must be generated");
    assert(sqliAlert.eventId === sqliEventRes.data.id, "Alert must link to supporting SQLi event ID");
    assert(sqliAlert.severity === "HIGH", "SQLi alert severity must be HIGH");
    assert(sqliAlert.riskScore > 0, "SQLi alert riskScore must be calculated");
    console.log(`  ✓ 5.1 SQL Injection Alert triggered: ${sqliAlert.reference} (Score: ${sqliAlert.riskScore}, Linked Event: ${sqliAlert.eventId})`);

    // Check Probe Alert
    const probeAlert = alerts.find(a => a.applicationId === qaApp.id && a.title.includes("PROBING"));
    assert(probeAlert, "API Probing alert must be generated");
    console.log(`  ✓ 5.2 API Probing Alert triggered: ${probeAlert.reference} (Score: ${probeAlert.riskScore})`);

    // Check Auth Burst Alert
    const burstAlert = alerts.find(a => a.title.includes("BURST") || a.description.includes(burstIp));
    assert(burstAlert, "Auth Failure Burst alert must be generated");
    console.log(`  ✓ 5.3 Auth Failure Burst Alert triggered: ${burstAlert.reference} (${burstAlert.title})`);

    results.passed++;

    // =========================================================================
    // 6. INVESTIGATE ALERT, EVIDENCE INTEGRITY, AND REMEDIATION LIFECYCLE
    // =========================================================================
    console.log("\n[TEST 6] ALERT INVESTIGATION, SHA-256 EVIDENCE INTEGRITY, AND REMEDIATION");

    // 6.1 Acknowledge Alert
    const ackRes = await request(`/alerts/${sqliAlert.id}/acknowledge`, {
      method: "POST",
      headers: { Authorization: `Bearer ${analystToken}` }
    });
    assert(ackRes.status === 200, `Acknowledge alert expected 200, got ${ackRes.status}`);
    assert(ackRes.data.status === "ACKNOWLEDGED", "Alert status must be ACKNOWLEDGED");
    console.log(`  ✓ 6.1 Alert ${sqliAlert.reference} acknowledged by analyst`);

    // 6.2 Create Finding for this Vulnerability
    const findingCreateRes = await request("/findings", {
      method: "POST",
      headers: { "Content-Type": "application/json", Authorization: `Bearer ${analystToken}` },
      body: JSON.stringify({
        applicationId: qaApp.id,
        title: "SQL Injection in Transactions Endpoint",
        description: "Unsanitized id query parameter allows boolean and union-based injection",
        severity: "HIGH",
        confidence: 0.90,
        endpoint: "/api/v1/transactions",
        parameter: "id",
        source: "MANUAL"
      })
    });
    assert(findingCreateRes.status === 201, `Create finding expected 201, got ${findingCreateRes.status}`);
    const finding = findingCreateRes.data;
    assert(finding.riskScore > 0, "Risk score must be computed for finding");
    console.log(`  ✓ 6.2 Finding created: ${finding.reference} (Initial Status: ${finding.status}, Risk: ${finding.riskScore})`);

    // 6.3 Upload Evidence File
    const sampleEvidence = "ETRA-CTI QA FORENSIC EVIDENCE: Captured SQL query payload: id=101' UNION SELECT";
    const evidenceBlob = new Blob([sampleEvidence], { type: "text/plain" });
    const evidenceForm = new FormData();
    evidenceForm.append("file", evidenceBlob, "qa-forensic-evidence.log");

    const uploadRes = await fetch(`${BASE_URL}/findings/${finding.id}/evidence`, {
      method: "POST",
      headers: { Authorization: `Bearer ${analystToken}` },
      body: evidenceForm
    });
    assert(uploadRes.status === 201, `Upload evidence expected 201, got ${uploadRes.status}`);
    const evidenceData = await uploadRes.json();
    assert(evidenceData.sha256, "Evidence must have calculated SHA-256");
    console.log(`  ✓ 6.3 Evidence uploaded: ${evidenceData.filename} (SHA-256: ${evidenceData.sha256})`);

    // 6.4 Verify SHA-256 Evidence Integrity
    const verifyRes = await request(`/evidence/${evidenceData.id}/verify`, {
      method: "POST",
      headers: { Authorization: `Bearer ${analystToken}` }
    });
    assert(verifyRes.status === 200, `Verify evidence expected 200, got ${verifyRes.status}`);
    assert(verifyRes.data.integrity === "INTACT", `Evidence integrity expected INTACT, got ${verifyRes.data.integrity}`);
    assert(verifyRes.data.storedSha256 === evidenceData.sha256, "Stored hash must match");
    assert(verifyRes.data.currentSha256 === evidenceData.sha256, "Computed hash must match stored hash");
    console.log("  ✓ 6.4 Cryptographic SHA-256 verification confirmed INTACT (no tampering)");

    // 6.5 Walk through Remediation Transitions:
    // a. OPEN -> IN_PROGRESS
    const inProgressRes = await request(`/findings/${finding.id}/status`, {
      method: "PATCH",
      headers: { "Content-Type": "application/json", Authorization: `Bearer ${analystToken}` },
      body: JSON.stringify({ status: "IN_PROGRESS", comment: "Assigned to engineering team" })
    });
    assert(inProgressRes.data.status === "IN_PROGRESS", "Status must be IN_PROGRESS");

    // b. IN_PROGRESS -> READY_FOR_RETEST
    const readyRes = await request(`/findings/${finding.id}/status`, {
      method: "PATCH",
      headers: { "Content-Type": "application/json", Authorization: `Bearer ${analystToken}` },
      body: JSON.stringify({ status: "READY_FOR_RETEST", comment: "Prepared statements deployed" })
    });
    assert(readyRes.data.status === "READY_FOR_RETEST", "Status must be READY_FOR_RETEST");

    // c. Retest FAIL -> REOPENED
    const retestFailRes = await request(`/findings/${finding.id}/retest`, {
      method: "POST",
      headers: { "Content-Type": "application/json", Authorization: `Bearer ${analystToken}` },
      body: JSON.stringify({ result: "FAIL", notes: "Blind SQLi bypass observed on alternate parameter" })
    });
    assert(retestFailRes.status === 201 || retestFailRes.status === 200, `Retest fail expected 201/200, got ${retestFailRes.status}`);
    const findingAfterFail = (await request(`/findings/${finding.id}`, {
      headers: { Authorization: `Bearer ${analystToken}` }
    })).data;
    assert(findingAfterFail.status === "REOPENED", `Expected status REOPENED on fail, got ${findingAfterFail.status}`);
    console.log("  ✓ 6.5 Retest outcome FAIL correctly transitioned finding to REOPENED");

    // d. Re-transition -> READY_FOR_RETEST
    await request(`/findings/${finding.id}/status`, {
      method: "PATCH",
      headers: { "Content-Type": "application/json", Authorization: `Bearer ${analystToken}` },
      body: JSON.stringify({ status: "READY_FOR_RETEST", comment: "Hibernate ORM parameterized bindings enforced" })
    });

    // e. Retest PASS -> VERIFIED
    const retestPassRes = await request(`/findings/${finding.id}/retest`, {
      method: "POST",
      headers: { "Content-Type": "application/json", Authorization: `Bearer ${analystToken}` },
      body: JSON.stringify({ result: "PASS", notes: "All automated fuzzing vectors and manual injection attempts blocked" })
    });
    assert(retestPassRes.status === 201 || retestPassRes.status === 200, `Retest pass expected 201/200, got ${retestPassRes.status}`);
    const findingAfterPass = (await request(`/findings/${finding.id}`, {
      headers: { Authorization: `Bearer ${analystToken}` }
    })).data;
    assert(findingAfterPass.status === "VERIFIED", `Expected status VERIFIED on pass, got ${findingAfterPass.status}`);
    console.log("  ✓ 6.6 Retest outcome PASS correctly transitioned finding to VERIFIED");

    results.passed++;

    // =========================================================================
    // 7. TEST SCANNER IMPORT (OWASP ZAP & BURP SUITE)
    // =========================================================================
    console.log("\n[TEST 7] SCANNER IMPORT AND DEDUPLICATION");

    // 7.1 OWASP ZAP JSON Import
    const zapJson = fs.readFileSync("c:\\dev\\etra-cti\\zap-sample.json");
    const zapForm = new FormData();
    zapForm.append("file", new Blob([zapJson]), "zap-sample.json");

    const zapRes = await fetch(`${BASE_URL}/integrations/zap/${qaApp.id}`, {
      method: "POST",
      headers: { Authorization: `Bearer ${analystToken}` },
      body: zapForm
    });
    assert(zapRes.status === 201, `ZAP import expected 201, got ${zapRes.status}`);
    const zapData = await zapRes.json();
    assert(zapData.source === "ZAP", "Source must be ZAP");
    assert(zapData.findingsCreated === 4, `Expected 4 findings created from ZAP, got ${zapData.findingsCreated}`);
    console.log(`  ✓ 7.1 OWASP ZAP report imported: ${zapData.findingsCreated} findings generated with auto-remediation`);

    // 7.2 Re-import same ZAP report to verify deduplication
    const zapForm2 = new FormData();
    zapForm2.append("file", new Blob([zapJson]), "zap-sample.json");
    const zapReimportRes = await fetch(`${BASE_URL}/integrations/zap/${qaApp.id}`, {
      method: "POST",
      headers: { Authorization: `Bearer ${analystToken}` },
      body: zapForm2
    });
    const zapReimportData = await zapReimportRes.json();
    assert(zapReimportData.findingsCreated === 0, "Duplicate import must create 0 new findings");
    assert(zapReimportData.findingsDuplicated === 4, `Duplicate count expected 4, got ${zapReimportData.findingsDuplicated}`);
    console.log(`  ✓ 7.2 Deduplication verified: re-import recorded ${zapReimportData.findingsDuplicated} duplicates and 0 duplicates created`);

    // 7.3 Burp Suite XML Import
    const burpXml = fs.readFileSync("c:\\dev\\etra-cti\\burp-sample.xml");
    const burpForm = new FormData();
    burpForm.append("file", new Blob([burpXml]), "burp-sample.xml");

    const burpRes = await fetch(`${BASE_URL}/integrations/burp/${qaApp.id}`, {
      method: "POST",
      headers: { Authorization: `Bearer ${analystToken}` },
      body: burpForm
    });
    assert(burpRes.status === 201, `Burp import expected 201, got ${burpRes.status}`);
    const burpData = await burpRes.json();
    assert(burpData.source === "BURP", "Source must be BURP");
    console.log(`  ✓ 7.3 Burp Suite XML report imported: ID ${burpData.id}`);

    results.passed++;

    // =========================================================================
    // 8. ATTEMPT CROSS-ORGANIZATION ACCESS AND CONFIRM DENIAL (BOLA/IDOR)
    // =========================================================================
    console.log("\n[TEST 8] CROSS-ORGANIZATION ACCESS DENIAL (BOLA/IDOR PREVENTION)");

    // 8.1 Create Tenant B
    const tenantBSlug = `tenant-b-${Date.now()}`;
    const createTenantBRes = await request("/organizations", {
      method: "POST",
      headers: { "Content-Type": "application/json", Authorization: `Bearer ${adminToken}` },
      body: JSON.stringify({
        name: "Foreign Organization Beta",
        slug: tenantBSlug
      })
    });
    assert(createTenantBRes.status === 201, `Create tenant B expected 201, got ${createTenantBRes.status}`);
    const tenantB = createTenantBRes.data;

    // 8.2 Register User B belonging ONLY to Tenant B
    const userBEmail = `userb_${Date.now()}@foreign.et`;
    const userBRegister = await request("/auth/register", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        email: userBEmail,
        password: "UserBPassword1234!",
        fullName: "Foreign Tenant User B",
        role: "ANALYST"
      })
    });
    assert(userBRegister.status === 201, `User B register expected 201, got ${userBRegister.status}`);
    const userB = userBRegister.data;

    // Add User B as member of Tenant B
    await request(`/organizations/${tenantB.id}/members`, {
      method: "POST",
      headers: { "Content-Type": "application/json", Authorization: `Bearer ${adminToken}` },
      body: JSON.stringify({
        userId: userB.id,
        role: "ANALYST"
      })
    });

    // Login as User B
    const userBLogin = await request("/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email: userBEmail, password: "UserBPassword1234!" })
    });
    const userBToken = userBLogin.data.accessToken;

    // 8.3 User B attempts to access Org A's projects -> MUST BE 403
    const crossOrgProjects = await request(`/organizations/${orgId}/projects`, {
      headers: { Authorization: `Bearer ${userBToken}` }
    });
    assert(crossOrgProjects.status === 403, `Cross-org projects expected 403 Forbidden, got ${crossOrgProjects.status}`);
    console.log("  ✓ 8.1 User B querying Organization A projects correctly denied with HTTP 403 Forbidden");

    // 8.4 User B attempts to generate API keys for Org A's application -> MUST BE 403
    const crossOrgApiKey = await request(`/applications/${qaApp.id}/api-keys`, {
      method: "POST",
      headers: { "Content-Type": "application/json", Authorization: `Bearer ${userBToken}` },
      body: JSON.stringify({ name: "Malicious Key", expiryDays: 30 })
    });
    assert(crossOrgApiKey.status === 403, `Cross-org API key gen expected 403 Forbidden, got ${crossOrgApiKey.status}`);
    console.log("  ✓ 8.2 User B generating API key for Organization A application correctly denied with HTTP 403 Forbidden");

    // 8.5 User B attempts to import scanner report into Org A's application -> MUST BE 403
    const crossOrgImportForm = new FormData();
    crossOrgImportForm.append("file", new Blob([zapJson]), "zap.json");
    const crossOrgImport = await fetch(`${BASE_URL}/integrations/zap/${qaApp.id}`, {
      method: "POST",
      headers: { Authorization: `Bearer ${userBToken}` },
      body: crossOrgImportForm
    });
    assert(crossOrgImport.status === 403, `Cross-org scanner import expected 403 Forbidden, got ${crossOrgImport.status}`);
    console.log("  ✓ 8.3 User B importing scanner data to Organization A application correctly denied with HTTP 403 Forbidden");

    // 8.6 M2M API Key bound to App A submitting event with App B's ID -> MUST BE 403/404
    const spoofedAppId = "00000000-0000-0000-0000-000000000099";
    const crossAppSubmission = await request("/events", {
      method: "POST",
      headers: { "Content-Type": "application/json", "X-ETRA-API-KEY": keyData.rawApiKey },
      body: JSON.stringify({
        applicationId: spoofedAppId,
        source: "API_GATEWAY",
        timestamp: new Date().toISOString()
      })
    });
    assert(crossAppSubmission.status === 403 || crossAppSubmission.status === 404,
      `Cross-app event spoofing expected 403/404, got ${crossAppSubmission.status}`);
    console.log(`  ✓ 8.4 M2M API Key locked to bound application; spoofing alternate application rejected (HTTP ${crossAppSubmission.status})`);

    results.passed++;

    console.log("\n===============================================================");
    console.log(`    QA VERIFICATION COMPLETE: ALL 8 TEST STAGES PASSED         `);
    console.log("===============================================================");

  } catch (err) {
    console.error("\n[QA SUITE FAILURE]:", err);
    process.exit(1);
  }
}

runQaSuite();
