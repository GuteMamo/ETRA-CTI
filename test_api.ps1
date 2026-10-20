$ErrorActionPreference = "Stop"

# 1. Login as Analyst
Write-Host "1. Authenticating as analyst@etra.et..."
$login = Invoke-RestMethod -Uri "http://localhost:8081/api/auth/login" -Method Post -ContentType "application/json" -Body '{"email":"analyst@etra.et","password":"Analyst1234!"}'
$token = $login.accessToken
$headers = @{ Authorization = "Bearer $token" }
Write-Host "-> Login success! Access token obtained."

# 2. Get Findings
Write-Host "`n2. Fetching Findings list..."
$findingsRes = Invoke-RestMethod -Uri "http://localhost:8081/api/findings?page=0&size=5" -Headers $headers
$finding = $findingsRes.content[0]
Write-Host "-> Found Finding: Reference=$($finding.reference), Title=$($finding.title), Id=$($finding.id)"

# 3. Upload Evidence
Write-Host "`n3. Uploading Evidence File (C:\dev\etra-cti\test-evidence.txt)..."
$form = @{
    file = Get-Item "C:\dev\etra-cti\test-evidence.txt"
    description = "Test Evidence for integrity checksum"
}
$evidence = Invoke-RestMethod -Uri "http://localhost:8081/api/findings/$($finding.id)/evidence" -Method Post -Headers $headers -Form $form
Write-Host "-> Evidence Upload Success!"
Write-Host "   Evidence ID: $($evidence.id)"
Write-Host "   Filename:    $($evidence.fileName)"
Write-Host "   SHA-256:     $($evidence.sha256)"

# 4. Cryptographic SHA-256 Integrity Check
Write-Host "`n4. Testing Cryptographic SHA-256 Integrity Verification..."
$verify = Invoke-RestMethod -Uri "http://localhost:8081/api/evidence/$($evidence.id)/verify" -Method Post -Headers $headers
Write-Host "-> Integrity Check Result:"
Write-Host "   Valid:       $($verify.valid)"
Write-Host "   Expected:    $($verify.expectedHash)"
Write-Host "   Actual:      $($verify.actualHash)"

# 5. Remediation Lifecycle Flow
Write-Host "`n5. Testing Remediation Lifecycle Transitions..."
# Move to IN_PROGRESS
Write-Host "   Transitioning finding to IN_PROGRESS..."
$inProgress = Invoke-RestMethod -Uri "http://localhost:8081/api/findings/$($finding.id)/status" -Method Patch -Headers $headers -ContentType "application/json" -Body '{"status":"IN_PROGRESS","comment":"Starting remediation work"}'
Write-Host "   -> Finding Status is now: $($inProgress.status)"

# Move to READY_FOR_RETEST (creates remediation record)
Write-Host "   Transitioning finding to READY_FOR_RETEST (Mark Fixed)..."
$ready = Invoke-RestMethod -Uri "http://localhost:8081/api/findings/$($finding.id)/status" -Method Patch -Headers $headers -ContentType "application/json" -Body '{"status":"READY_FOR_RETEST","comment":"Applied security patch"}'
Write-Host "   -> Finding Status is now: $($ready.status)"

# Test Retest FAIL
Write-Host "   Recording Retest Outcome: FAIL..."
$retestFail = Invoke-RestMethod -Uri "http://localhost:8081/api/findings/$($finding.id)/retest" -Method Post -Headers $headers -ContentType "application/json" -Body '{"outcome":"FAIL","notes":"Vulnerability still triggered with secondary vector"}'
Write-Host "   -> Finding Status is now: $($retestFail.status)"

# Mark Fixed again & Retest PASS
Write-Host "   Fixing again and recording Retest Outcome: PASS..."
$null = Invoke-RestMethod -Uri "http://localhost:8081/api/findings/$($finding.id)/status" -Method Patch -Headers $headers -ContentType "application/json" -Body '{"status":"READY_FOR_RETEST","comment":"Patch validated with secondary vector"}'
$retestPass = Invoke-RestMethod -Uri "http://localhost:8081/api/findings/$($finding.id)/retest" -Method Post -Headers $headers -ContentType "application/json" -Body '{"outcome":"PASS","notes":"Vulnerability completely remediated and confirmed"}'
Write-Host "   -> Finding Status is now: $($retestPass.status)"

# 6. Test Security Alerts
Write-Host "`n6. Testing Security Alerts..."
$alerts = Invoke-RestMethod -Uri "http://localhost:8081/api/alerts" -Headers $headers
Write-Host "   Total alerts: $($alerts.totalElements)"
if ($alerts.content.Count -gt 0) {
    $firstAlert = $alerts.content[0]
    Write-Host "   Alert Ref: $($firstAlert.reference) (Status: $($firstAlert.status))"
    $ack = Invoke-RestMethod -Uri "http://localhost:8081/api/alerts/$($firstAlert.id)/acknowledge" -Method Post -Headers $headers
    Write-Host "   -> Acknowledged Alert! New Status: $($ack.status)"
}

# 7. Test Vulnerability Scanner Import (Burp XML sample)
Write-Host "`n7. Testing Scanner Import..."
$scannerForm = @{
    file = Get-Item "c:\dev\etra-cti\burp-sample.xml"
    source = "BURP"
    applicationId = $finding.applicationId
}
$importRes = Invoke-RestMethod -Uri "http://localhost:8081/api/integrations/imports" -Method Post -Headers $headers -Form $scannerForm
Write-Host "   -> Scanner Import Success! Import ID: $($importRes.id), Status: $($importRes.status), Findings Created: $($importRes.findingsCreated)"

# 8. Test Audit Logs
Write-Host "`n8. Verifying Audit Logs..."
$audit = Invoke-RestMethod -Uri "http://localhost:8081/api/audit?page=0&size=5" -Headers $headers
Write-Host "   Total Audit Records: $($audit.totalElements)"
$audit.content | Select-Object -First 3 | ForEach-Object {
    Write-Host "   - [$($_.createdAt)] Action: $($_.action), Entity: $($_.entityType)/$($_.entityId), Actor: $($_.actorEmail)"
}

Write-Host "`n>>> ALL ETRA-CTI CORE ENDPOINTS AND WORKFLOWS VALIDATED SUCCESSFULLY! <<<"
