-- V14__enhance_findings_remediation.sql

-- Add tool display name to findings
ALTER TABLE findings ADD COLUMN IF NOT EXISTS source_display VARCHAR(100);

-- Add auto-generated remediation recommendation
ALTER TABLE remediations ADD COLUMN IF NOT EXISTS auto_generated BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE remediations ADD COLUMN IF NOT EXISTS recommendation TEXT;
ALTER TABLE remediations ADD COLUMN IF NOT EXISTS recommendation_source VARCHAR(100);

-- Knowledge base for automated remediation suggestions
CREATE TABLE IF NOT EXISTS remediation_knowledge (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    match_keyword   VARCHAR(255) NOT NULL,
    category        VARCHAR(80) NOT NULL,
    title           VARCHAR(255) NOT NULL,
    recommendation  TEXT NOT NULL,
    cwe             VARCHAR(20),
    owasp           VARCHAR(80),
    priority        VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (match_keyword)
);

CREATE INDEX idx_remediation_kb_keyword ON remediation_knowledge(match_keyword);

-- Seed the knowledge base
INSERT INTO remediation_knowledge (match_keyword, category, title, recommendation, cwe, owasp, priority) VALUES
('sql injection', 'INJECTION', 'SQL Injection',
 'Use parameterized queries (prepared statements) for all database access. Never concatenate user input into SQL strings. Validate and whitelist all input parameters. Consider using an ORM. Add a WAF rule as defense-in-depth. Add a regression test that sends a known SQLi payload and asserts a 400 response.',
 'CWE-89', 'A03:2021', 'HIGH'),

('sqli', 'INJECTION', 'SQL Injection',
 'Use parameterized queries (prepared statements) for all database access. Never concatenate user input into SQL strings. Validate and whitelist all input parameters. Consider using an ORM. Add a WAF rule as defense-in-depth.',
 'CWE-89', 'A03:2021', 'HIGH'),

('cross-site scripting', 'XSS', 'Cross-Site Scripting (XSS)',
 'Encode all user-controllable output using context-aware escaping (HTML, attribute, URL, JavaScript). Apply a strict Content-Security-Policy header. Use frameworks that auto-escape by default. Sanitize rich-text input with a whitelist library like DOMPurify.',
 'CWE-79', 'A03:2021', 'HIGH'),

('xss', 'XSS', 'Cross-Site Scripting (XSS)',
 'Encode all user-controllable output using context-aware escaping. Apply a strict Content-Security-Policy header. Sanitize rich-text input with a whitelist library.',
 'CWE-79', 'A03:2021', 'HIGH'),

('cookie without httponly', 'CONFIGURATION', 'Cookie Missing HttpOnly Flag',
 'Set the HttpOnly attribute on all session cookies. Also set Secure and SameSite=Strict. This prevents client-side script access to session identifiers.',
 'CWE-1004', 'A05:2021', 'MEDIUM'),

('httponly', 'CONFIGURATION', 'Cookie Missing HttpOnly Flag',
 'Set HttpOnly, Secure, and SameSite attributes on all session cookies.',
 'CWE-1004', 'A05:2021', 'MEDIUM'),

('x-content-type-options', 'CONFIGURATION', 'Missing Security Header',
 'Add the X-Content-Type-Options: nosniff header to all HTTP responses. Also add X-Frame-Options: DENY and Strict-Transport-Security.',
 'CWE-693', 'A05:2021', 'MEDIUM'),

('information disclosure', 'DISCLOSURE', 'Information Disclosure',
 'Suppress detailed error messages in production. Log full stack traces server-side, but return generic messages to clients. Disable directory listing and version banners.',
 'CWE-200', 'A01:2021', 'MEDIUM'),

('path traversal', 'INJECTION', 'Path Traversal',
 'Validate and canonicalize all file paths. Whitelist allowed file names. Never use user input directly in file system paths. Use a safe path joining function.',
 'CWE-22', 'A01:2021', 'HIGH'),

('command injection', 'INJECTION', 'Command Injection',
 'Never pass user input to shell commands. Use safe APIs instead of system()/exec(). If shell execution is unavoidable, use strict whitelisting and escape arguments.',
 'CWE-78', 'A03:2021', 'CRITICAL'),

('ssrf', 'INJECTION', 'Server-Side Request Forgery (SSRF)',
 'Whitelist allowed outbound hosts. Block private IP ranges (10/8, 172.16/12, 192.168/16, 169.254/16). Disable unnecessary URL schemes (file://, gopher://).',
 'CWE-918', 'A10:2021', 'HIGH'),

('hardcoded api key', 'SECRETS', 'Hardcoded Credentials',
 'Move all secrets to environment variables or a secrets manager (Vault, AWS Secrets Manager). Rotate any exposed keys immediately. Add the file to .gitignore and remove from git history.',
 'CWE-798', 'A07:2021', 'CRITICAL'),

('hardcoded', 'SECRETS', 'Hardcoded Credentials',
 'Move secrets to environment variables or a secrets manager. Rotate exposed credentials.',
 'CWE-798', 'A07:2021', 'HIGH'),

('weak crypto', 'CRYPTO', 'Weak Cryptographic Algorithm',
 'Replace weak algorithms (DES, MD5, SHA1, RC4) with modern ones (AES-256-GCM, SHA-256, SHA-3). Use TLS 1.2+ for transport.',
 'CWE-327', 'A02:2021', 'HIGH'),

('app is not signed', 'MOBILE', 'Unsigned Mobile Application',
 'Sign the APK/IPA with a valid production certificate. Unsigned apps can be repackaged with malicious code. Enable Play Integrity / App Attest.',
 'CWE-347', 'M7:2016', 'HIGH'),

('exported activity', 'MOBILE', 'Exported Android Component',
 'Add android:exported="false" to all components that do not need external access. If export is required, protect with a signature-level permission.',
 'CWE-926', 'M1:2016', 'HIGH'),

('debuggable', 'MOBILE', 'Debuggable Application',
 'Set android:debuggable="false" in production builds. Remove android:debuggable from the manifest entirely; the build system sets it based on the build type.',
 'CWE-489', 'M1:2016', 'MEDIUM'),

('backup allowed', 'MOBILE', 'Application Backup Enabled',
 'Set android:allowBackup="false" unless backups are an explicit feature. Prevents adb backup extraction of app data.',
 'CWE-312', 'M2:2016', 'MEDIUM'),

('missing anti-clickjacking', 'CONFIGURATION', 'Missing Anti-Clickjacking Header',
 'Set the X-Frame-Options: DENY header (or Content-Security-Policy: frame-ancestors ''none''). Prevents the page from being embedded in an attacker-controlled iframe.',
 'CWE-1021', 'A05:2021', 'MEDIUM'),

('missing security headers', 'CONFIGURATION', 'Missing Security Headers',
 'Add these response headers: X-Frame-Options, X-Content-Type-Options, Strict-Transport-Security, Content-Security-Policy, Referrer-Policy.',
 'CWE-693', 'A05:2021', 'MEDIUM'),

('default', 'GENERIC', 'Security Issue',
 'Review the finding details, reproduce the vulnerability, apply the standard fix for this category, and add a regression test.',
 NULL, NULL, 'MEDIUM')
ON CONFLICT (match_keyword) DO NOTHING;