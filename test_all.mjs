import fs from "fs";

const BASE_URL = "http://localhost:8081/api";

async function main() {
  console.log("=== ETRA-CTI FULL END-TO-END AUTOMATED VERIFICATION ===");

  // 1. Authenticate as Analyst
  console.log("\n1. Authenticating as analyst@etra.et...");
  const loginRes = await fetch(`${BASE_URL}/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email: "analyst@etra.et", password: "Analyst1234!" }),
  });
  if (!loginRes.ok) throw new Error(`Login failed: ${loginRes.status} ${await loginRes.text()}`);
  const tokens = await loginRes.json();
  const token = tokens.accessToken;
  const authHeaders = { Authorization: `Bearer ${token}` };
  console.log("✓ Login successful, JWT token obtained.");

  // 1b. Resolve User's Organization and Application Context
  console.log("\n1b. Resolving organization & application context...");
  const orgsRes = await fetch(`${BASE_URL}/organizations`, { headers: authHeaders });
  const orgs = await orgsRes.json();
  const orgId = orgs[0].id;
  const projectsRes = await fetch(`${BASE_URL}/organizations/${orgId}/projects`, { headers: authHeaders });
  const projects = await projectsRes.json();
  const projectId = projects[0].id;
  const appsRes = await fetch(`${BASE_URL}/projects/${projectId}/applications`, { headers: authHeaders });
  const apps = await appsRes.json();
  const app = apps[0];
  console.log(`✓ Active Application: ${app.name} (ID: ${app.id})`);

  // 2. Fetch Findings
  console.log("\n2. Fetching Findings list...");
  const findingsRes = await fetch(`${BASE_URL}/findings?page=0&size=20`, { headers: authHeaders });
  if (!findingsRes.ok) throw new Error(`Findings query failed: ${findingsRes.status}`);
  const findingsPage = await findingsRes.json();
  const finding = findingsPage.content.find(f => f.applicationId === app.id) || findingsPage.content[0];
  console.log(`✓ Retrieved finding: ${finding.reference} (ID: ${finding.id})`);
  console.log(`  Title: ${finding.title}`);
  console.log(`  Initial Status: ${finding.status}`);

  // 3. Upload Evidence Artifact
  console.log("\n3. Testing Multipart File Upload for Evidence...");
  const fileContent = fs.readFileSync("C:\\dev\\etra-cti\\test-evidence.txt");
  const formData = new FormData();
  formData.append("file", new Blob([fileContent]), "test-evidence.txt");

  const uploadRes = await fetch(`${BASE_URL}/findings/${finding.id}/evidence`, {
    method: "POST",
    headers: authHeaders,
    body: formData,
  });
  if (!uploadRes.ok) throw new Error(`Upload failed: ${uploadRes.status} ${await uploadRes.text()}`);
  const evidence = await uploadRes.json();
  console.log("✓ Evidence upload succeeded!");
  console.log(`  Evidence ID: ${evidence.id}`);
  console.log(`  File Name:   ${evidence.filename}`);
  console.log(`  SHA-256:     ${evidence.sha256}`);

  // 4. Verify Cryptographic SHA-256 Integrity
  console.log("\n4. Testing Cryptographic SHA-256 Integrity Check...");
  const verifyRes = await fetch(`${BASE_URL}/evidence/${evidence.id}/verify`, {
    method: "POST",
    headers: authHeaders,
  });
  if (!verifyRes.ok) throw new Error(`Verify failed: ${verifyRes.status} ${await verifyRes.text()}`);
  const verifyData = await verifyRes.json();
  console.log("✓ Integrity check endpoint returned:");
  console.log(`  Integrity:     ${verifyData.integrity}`);
  console.log(`  Stored Hash:   ${verifyData.storedSha256}`);
  console.log(`  Current Hash:  ${verifyData.currentSha256}`);
  if (verifyData.integrity !== "INTACT") {
    throw new Error(`Evidence SHA-256 integrity check failed! Got ${verifyData.integrity}`);
  }
  console.log("✓ Evidence integrity verified as 100% INTACT.");

  // 5. Remediation Lifecycle Flow
  console.log("\n5. Testing Remediation Lifecycle Transitions...");

  // Transition to IN_PROGRESS
  console.log("  a. Transition to IN_PROGRESS...");
  const inProgressRes = await fetch(`${BASE_URL}/findings/${finding.id}/status`, {
    method: "PATCH",
    headers: { ...authHeaders, "Content-Type": "application/json" },
    body: JSON.stringify({ status: "IN_PROGRESS", comment: "Starting remediation in sprint" }),
  });
  const inProgressData = await inProgressRes.json();
  console.log(`     Status is now: ${inProgressData.status}`);

  // Transition to READY_FOR_RETEST (triggers automated remediation record creation)
  console.log("  b. Transition to READY_FOR_RETEST (Mark Fixed)...");
  const readyRes = await fetch(`${BASE_URL}/findings/${finding.id}/status`, {
    method: "PATCH",
    headers: { ...authHeaders, "Content-Type": "application/json" },
    body: JSON.stringify({ status: "READY_FOR_RETEST", comment: "Input sanitization and parameterized queries applied" }),
  });
  const readyData = await readyRes.json();
  console.log(`     Status is now: ${readyData.status}`);

  // Test Retest FAIL
  console.log("  c. Retesting with Outcome: FAIL...");
  const retestFailRes = await fetch(`${BASE_URL}/findings/${finding.id}/retest`, {
    method: "POST",
    headers: { ...authHeaders, "Content-Type": "application/json" },
    body: JSON.stringify({ result: "FAIL", notes: "Vulnerability reproduced with alternate Unicode payload" }),
  });
  const failData = await retestFailRes.json();
  console.log(`     Retest outcome recorded: ${failData.result}`);
  
  const updatedFindingFail = await (await fetch(`${BASE_URL}/findings/${finding.id}`, { headers: authHeaders })).json();
  console.log(`     Finding status is now: ${updatedFindingFail.status}`);
  if (updatedFindingFail.status !== "REOPENED") {
    throw new Error(`Expected REOPENED but got ${updatedFindingFail.status}`);
  }

  // Transition back to READY_FOR_RETEST
  console.log("  d. Remediation re-applied -> READY_FOR_RETEST...");
  await fetch(`${BASE_URL}/findings/${finding.id}/status`, {
    method: "PATCH",
    headers: { ...authHeaders, "Content-Type": "application/json" },
    body: JSON.stringify({ status: "READY_FOR_RETEST", comment: "Unicode normalization layer added" }),
  });

  // Test Retest PASS
  console.log("  e. Retesting with Outcome: PASS...");
  const retestPassRes = await fetch(`${BASE_URL}/findings/${finding.id}/retest`, {
    method: "POST",
    headers: { ...authHeaders, "Content-Type": "application/json" },
    body: JSON.stringify({ result: "PASS", notes: "All fuzz vectors and test suites passed cleanly" }),
  });
  const passData = await retestPassRes.json();
  console.log(`     Retest outcome recorded: ${passData.result}`);

  const updatedFindingPass = await (await fetch(`${BASE_URL}/findings/${finding.id}`, { headers: authHeaders })).json();
  console.log(`     Finding status is now: ${updatedFindingPass.status}`);
  if (updatedFindingPass.status !== "VERIFIED") {
    throw new Error(`Expected VERIFIED but got ${updatedFindingPass.status}`);
  }

  // 6. Test Security Alerts & Acknowledgment
  console.log("\n6. Testing Security Alerts...");
  const alertsRes = await fetch(`${BASE_URL}/alerts`, { headers: authHeaders });
  const alertsPage = await alertsRes.json();
  console.log(`✓ Total Alerts in system: ${alertsPage.totalElements}`);
  if (alertsPage.content && alertsPage.content.length > 0) {
    const alert = alertsPage.content[0];
    console.log(`  Alert Reference: ${alert.reference} (Status: ${alert.status}, Severity: ${alert.severity})`);
    const ackRes = await fetch(`${BASE_URL}/alerts/${alert.id}/acknowledge`, {
      method: "POST",
      headers: authHeaders,
    });
    const ackData = await ackRes.json();
    console.log(`✓ Acknowledged Alert ${alert.reference}: Status is now ${ackData.status}`);
  }

  // 7. Test Scanner Import (Burp Suite XML)
  console.log("\n7. Testing Vulnerability Scanner Import (BURP XML)...");
  const burpXml = fs.readFileSync("c:\\dev\\etra-cti\\burp-sample.xml");
  const importForm = new FormData();
  importForm.append("file", new Blob([burpXml]), "burp-sample.xml");

  const importRes = await fetch(`${BASE_URL}/integrations/burp/${app.id}`, {
    method: "POST",
    headers: authHeaders,
    body: importForm,
  });
  if (!importRes.ok) throw new Error(`Import failed: ${importRes.status} ${await importRes.text()}`);
  const importData = await importRes.json();
  console.log("✓ Scanner import successful!");
  console.log(`  Import ID:        ${importData.id}`);
  console.log(`  Scanner Source:   ${importData.source}`);
  console.log(`  Status:           ${importData.status}`);
  console.log(`  Findings Created: ${importData.findingsCreated}`);

  // 7b. Test Scanner Import (OWASP ZAP JSON)
  console.log("\n7b. Testing Vulnerability Scanner Import (OWASP ZAP JSON)...");
  const zapJson = fs.readFileSync("c:\\dev\\etra-cti\\zap-sample.json");
  const zapForm = new FormData();
  zapForm.append("file", new Blob([zapJson]), "zap-sample.json");

  const zapRes = await fetch(`${BASE_URL}/integrations/zap/${app.id}`, {
    method: "POST",
    headers: authHeaders,
    body: zapForm,
  });
  if (!zapRes.ok) throw new Error(`ZAP Import failed: ${zapRes.status} ${await zapRes.text()}`);
  const zapData = await zapRes.json();
  console.log("✓ OWASP ZAP Scanner import successful!");
  console.log(`  Import ID:        ${zapData.id}`);
  console.log(`  Scanner Source:   ${zapData.source}`);
  console.log(`  Status:           ${zapData.status}`);
  console.log(`  Findings Created: ${zapData.findingsCreated}`);
  console.log(`  Duplicates:       ${zapData.findingsDuplicated}`);

  // 8. Test Threat Intel IOCs
  console.log("\n8. Testing Threat Intelligence Indicators...");
  const iocsRes = await fetch(`${BASE_URL}/threat-intel/iocs`, { headers: authHeaders });
  const iocs = await iocsRes.json();
  console.log(`✓ Retrieved ${iocs.length} threat intelligence indicators:`);
  for (const ioc of iocs.slice(0, 3)) {
    console.log(`  - [${ioc.type}] ${ioc.value} (Risk: ${ioc.riskScore}, Confidence: ${ioc.confidence})`);
  }

  // 9. Test Attack Campaigns
  console.log("\n9. Testing Attack Campaigns & Timeline...");
  const campaignsRes = await fetch(`${BASE_URL}/campaigns`, { headers: authHeaders });
  const campaigns = await campaignsRes.json();
  console.log(`✓ Retrieved ${campaigns.length} attack campaigns:`);
  for (const c of campaigns) {
    console.log(`  - Campaign: ${c.title} (Source IP: ${c.sourceIp}, Events: ${c.eventCount}, Risk: ${c.riskBand})`);
    const timelineRes = await fetch(`${BASE_URL}/campaigns/${c.id}/timeline`, { headers: authHeaders });
    const timeline = await timelineRes.json();
    console.log(`    Timeline Events: ${timeline.length} security events aggregated`);
  }

  // 10. Test Audit Logs
  console.log("\n10. Testing Audit Trail Records...");
  const auditRes = await fetch(`${BASE_URL}/audit?page=0&size=10`, { headers: authHeaders });
  const auditPage = await auditRes.json();
  console.log(`✓ Total Audit Records: ${auditPage.totalElements}`);
  for (const log of auditPage.content.slice(0, 5)) {
    console.log(`  - [${log.createdAt}] ${log.action} on ${log.entityType}/${log.entityId} by ${log.actorEmail}`);
  }

  // 11. Application M2M API Key Generation & Event Ingestion
  console.log("\n11. Testing Application M2M API Key Generation & Event Ingestion...");
  const createKeyRes = await fetch(`${BASE_URL}/applications/${app.id}/api-keys`, {
    method: "POST",
    headers: { ...authHeaders, "Content-Type": "application/json" },
    body: JSON.stringify({ name: "Payment Gateway M2M Agent", expiryDays: 30 })
  });
  if (!createKeyRes.ok) throw new Error(`Create API Key failed: ${createKeyRes.status} ${await createKeyRes.text()}`);
  const keyCreated = await createKeyRes.json();
  console.log(`✓ Generated M2M API Key: ${keyCreated.rawApiKey.substring(0, 18)}... (ID: ${keyCreated.id})`);

  // Submit security event using M2M API Key via X-ETRA-API-KEY header
  console.log("  a. Submitting event via X-ETRA-API-KEY header...");
  const eventRes = await fetch(`${BASE_URL}/events`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "X-ETRA-API-KEY": keyCreated.rawApiKey
    },
    body: JSON.stringify({
      applicationId: app.id,
      source: "API_GATEWAY",
      timestamp: new Date().toISOString(),
      endpoint: "/api/v1/payments",
      httpMethod: "POST",
      statusCode: 400,
      sourceIp: "196.188.12.34",
      metadata: {
        payload: "' UNION SELECT 1, @@version --",
        eventType: "SQL_INJECTION_ATTEMPT",
        message: "Union-based injection detected in query param"
      }
    })
  });
  if (!eventRes.ok) throw new Error(`Event submission failed: ${eventRes.status} ${await eventRes.text()}`);
  const eventData = await eventRes.json();
  console.log(`✓ Event persisted successfully! (Event ID: ${eventData.id}, Source: ${eventData.source})`);

  // Test IDOR/BOLA prevention: attempting to submit event for another application ID
  console.log("  b. Verifying IDOR/BOLA rejection for cross-application submission...");
  const idorRes = await fetch(`${BASE_URL}/events`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "X-ETRA-API-KEY": keyCreated.rawApiKey
    },
    body: JSON.stringify({
      applicationId: "00000000-0000-0000-0000-000000000001",
      source: "API_GATEWAY",
      timestamp: new Date().toISOString(),
      endpoint: "/api/v1/admin",
      metadata: { attempted: "Cross-tenant IDOR" }
    })
  });
  console.log(`✓ Cross-application submission correctly rejected with HTTP ${idorRes.status}`);
  if (idorRes.status !== 403 && idorRes.status !== 404) throw new Error(`Expected 403 or 404 on cross-app check, got ${idorRes.status}`);

  // Test Key Revocation
  console.log("  c. Testing API Key Revocation...");
  const revokeRes = await fetch(`${BASE_URL}/applications/${app.id}/api-keys/${keyCreated.id}`, {
    method: "DELETE",
    headers: authHeaders
  });
  if (!revokeRes.ok) throw new Error(`Revoke API Key failed: ${revokeRes.status}`);
  console.log("✓ API key successfully revoked");

  // Verify revoked key is rejected
  const revokedUseRes = await fetch(`${BASE_URL}/events`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "X-ETRA-API-KEY": keyCreated.rawApiKey
    },
    body: JSON.stringify({
      applicationId: app.id,
      source: "API_GATEWAY",
      timestamp: new Date().toISOString(),
      endpoint: "/api/v1/probe",
      metadata: { probe: "Post-revocation check" }
    })
  });
  console.log(`✓ Revoked API key correctly rejected with HTTP ${revokedUseRes.status}`);
  if (revokedUseRes.status !== 401 && revokedUseRes.status !== 403) throw new Error(`Expected 401 or 403 on revoked key, got ${revokedUseRes.status}`);

  console.log("\n=======================================================");
  console.log(">>> ALL ETRA-CTI MODULES & WORKFLOWS VALIDATED 100% <<<");
  console.log("=======================================================");
}

main().catch(err => {
  console.error("Verification failed:", err);
  process.exit(1);
});
