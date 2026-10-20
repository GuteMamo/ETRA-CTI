#!/usr/bin/env node
/**
 * ETRA-CTI External Application Security Event Integration Example (Node.js)
 * 
 * Demonstrates how an external application (web app, microservice, API gateway)
 * sends security events to the ETRA-CTI platform using an Application API Key.
 *
 * Usage:
 *   node examples/server_integration.mjs
 */

const ETRA_BASE_URL = process.env.ETRA_URL || "http://localhost:8081/api";
const APPLICATION_ID = process.env.ETRA_APP_ID;
const API_KEY = process.env.ETRA_API_KEY;

if (!APPLICATION_ID || !API_KEY) {
  console.log(`
[!] Configuration missing.
    Please set environment variables:
      ETRA_APP_ID  - The UUID of your registered application in ETRA-CTI
      ETRA_API_KEY - The raw API key obtained from ETRA-CTI (e.g. etra_live_...)

    Example:
      $env:ETRA_APP_ID="<your-app-id>"
      $env:ETRA_API_KEY="<your-api-key>"
      node examples/server_integration.mjs
`);
  process.exit(1);
}

async function sendSecurityEvent(eventData) {
  const payload = {
    applicationId: APPLICATION_ID,
    source: eventData.source || "APPLICATION_LOG",
    timestamp: new Date().toISOString(),
    sourceIp: eventData.sourceIp || "127.0.0.1",
    method: eventData.method || "POST",
    path: eventData.path || "/api/login",
    statusCode: eventData.statusCode || 401,
    userId: eventData.userId || "anonymous",
    metadata: eventData.metadata || {}
  };

  console.log(`[*] Submitting security event for app ${APPLICATION_ID}...`);

  const response = await fetch(`${ETRA_BASE_URL}/events`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "X-ETRA-API-KEY": API_KEY
    },
    body: JSON.stringify(payload)
  });

  if (!response.ok) {
    const errorBody = await response.text();
    throw new Error(`ETRA ingestion failed (HTTP ${response.status}): ${errorBody}`);
  }

  const result = await response.json();
  console.log(`[+] Security event accepted by ETRA-CTI!`);
  console.log(`    Event ID:   ${result.id}`);
  console.log(`    Timestamp:  ${result.eventTime}`);
  console.log(`    Normalized:`, JSON.stringify(result.normalized, null, 2));
  return result;
}

// Demo event: suspicious SQL pattern
sendSecurityEvent({
  source: "API_GATEWAY",
  sourceIp: "203.0.113.195",
  method: "POST",
  path: "/api/search?q=1' UNION SELECT username,password FROM users--",
  statusCode: 200,
  userId: "user_session_481",
  metadata: {
    userAgent: "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
    requestDurationMs: 42
  }
}).catch(err => {
  console.error("[-] Error:", err.message);
  process.exit(1);
});
