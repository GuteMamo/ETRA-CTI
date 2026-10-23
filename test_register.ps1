$ErrorActionPreference = "Stop"

function Test-Endpoint {
    Write-Host "=== TESTING REGISTER APP & KEY GENERATION FUNCTIONALITY ==="

    # 1. Login as Admin
    Write-Host "`n1. Logging in as admin@etra.et..."
    $login = Invoke-RestMethod -Uri "http://localhost:8081/api/auth/login" -Method Post -ContentType "application/json" -Body '{"email":"admin@etra.et","password":"Admin1234!"}'
    $token = $login.accessToken
    $headers = @{ Authorization = "Bearer $token"; "Content-Type" = "application/json" }
    Write-Host "   -> Admin Login Success! Token acquired."

    # 2. Get list of projects
    Write-Host "`n2. Listing projects..."
    $projects = Invoke-RestMethod -Uri "http://localhost:8081/api/projects" -Headers $headers
    Write-Host "   -> Total projects available: $($projects.Count)"
    if ($projects.Count -eq 0) {
        throw "No projects found!"
    }
    $firstProject = $projects[0]
    Write-Host "   -> Using project: $($firstProject.name) ($($firstProject.id))"

    # 3. Register a new application via /api/projects/{projectId}/applications
    $appName = "Test App Automation $(Get-Random)"
    Write-Host "`n3. Registering new application '$appName' via /api/projects/{id}/applications..."
    $appBody = @{
        name = $appName
        type = "API"
        environment = "PRODUCTION"
        criticality = "HIGH"
        baseUrl = "https://automation-test.etra.et"
    } | ConvertTo-Json

    $createdApp = Invoke-RestMethod -Uri "http://localhost:8081/api/projects/$($firstProject.id)/applications" -Method Post -Headers $headers -Body $appBody
    Write-Host "   -> Application Created Successfully!"
    Write-Host "      App ID:          $($createdApp.id)"
    Write-Host "      App Name:        $($createdApp.name)"
    Write-Host "      App Project ID:  $($createdApp.projectId)"
    Write-Host "      App Environment: $($createdApp.environment)"
    Write-Host "      App Criticality: $($createdApp.criticality)"

    # 4. Register another application via direct /api/applications endpoint
    $directAppName = "Direct App $(Get-Random)"
    Write-Host "`n4. Registering application '$directAppName' via direct /api/applications endpoint..."
    $directAppBody = @{
        name = $directAppName
        type = "WEB_API"
        environment = "STAGING"
        criticality = "MEDIUM"
        baseUrl = "https://direct-test.etra.et"
    } | ConvertTo-Json

    $createdDirectApp = Invoke-RestMethod -Uri "http://localhost:8081/api/applications" -Method Post -Headers $headers -Body $directAppBody
    Write-Host "   -> Direct Application Created Successfully!"
    Write-Host "      App ID: $($createdDirectApp.id)"
    Write-Host "      App Name: $($createdDirectApp.name)"

    # 4b. Register application via /api/applications with explicit projectId in JSON body
    $bodyProjectIdApp = "Body ProjectId App $(Get-Random)"
    Write-Host "`n4b. Registering application with explicit projectId in request body..."
    $bodyProjPayload = @{
        name = $bodyProjectIdApp
        type = "MOBILE"
        environment = "DEV"
        criticality = "LOW"
        projectId = $firstProject.id
    } | ConvertTo-Json
    $createdBodyProjApp = Invoke-RestMethod -Uri "http://localhost:8081/api/applications" -Method Post -Headers $headers -Body $bodyProjPayload
    Write-Host "   -> Application with JSON body projectId created successfully!"
    Write-Host "      App ID: $($createdBodyProjApp.id)"
    Write-Host "      Project ID: $($createdBodyProjApp.projectId)"
    if ($createdBodyProjApp.projectId -ne $firstProject.id) {
        throw "Application projectId does not match expected project ID!"
    }

    # 5. Generate API Key for the registered application
    Write-Host "`n5. Generating API Key for application '$($createdApp.name)'..."
    $keyReqBody = @{
        name = "Production Ingest Key $(Get-Random)"
        expiryDays = 30
    } | ConvertTo-Json

    $keyRes = Invoke-RestMethod -Uri "http://localhost:8081/api/applications/$($createdApp.id)/api-keys" -Method Post -Headers $headers -Body $keyReqBody
    Write-Host "   -> API Key Generated Successfully!"
    Write-Host "      Key ID:      $($keyRes.id)"
    Write-Host "      Key Name:    $($keyRes.name)"
    Write-Host "      Key Prefix:  $($keyRes.keyPrefix)"
    Write-Host "      Raw Key:     $($keyRes.rawApiKey)"
    Write-Host "      Status:      $($keyRes.status)"
    Write-Host "      Expires At:  $($keyRes.expiresAt)"

    if (-not $keyRes.rawApiKey.StartsWith("etra_live_")) {
        throw "API key does not start with expected prefix 'etra_live_'!"
    }

    # 6. List API Keys for the application
    Write-Host "`n6. Listing API Keys for application $($createdApp.id)..."
    $keysList = Invoke-RestMethod -Uri "http://localhost:8081/api/applications/$($createdApp.id)/api-keys" -Headers $headers
    Write-Host "   -> Total keys returned: $($keysList.Count)"
    $matchedKey = $keysList | Where-Object { $_.id -eq $keyRes.id }
    if (-not $matchedKey) {
        throw "Newly created key was not found in keys list!"
    }
    Write-Host "   -> Key found in list: Name=$($matchedKey.name), Status=$($matchedKey.status)"

    # 7. Test M2M Ingestion using the generated API Key
    Write-Host "`n7. Testing M2M telemetry ingestion with X-ETRA-API-KEY header..."
    $eventPayload = @{
        applicationId = $createdApp.id
        source = "API_GATEWAY"
        timestamp = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
        sourceIp = "198.51.100.77"
        method = "POST"
        path = "/api/v1/auth/login"
        statusCode = 401
        userIdentifier = "test_user_42"
        metadata = @{
            client = "m2m-test-runner"
            attempt = 1
        }
    } | ConvertTo-Json

    $m2mHeaders = @{
        "X-ETRA-API-KEY" = $keyRes.rawApiKey
        "Content-Type" = "application/json"
    }
    $ingestRes = Invoke-RestMethod -Uri "http://localhost:8081/api/events" -Method Post -Headers $m2mHeaders -Body $eventPayload
    Write-Host "   -> Event ingested successfully!"
    Write-Host "      Event ID: $($ingestRes.id)"
    Write-Host "      App ID:   $($ingestRes.applicationId)"
    Write-Host "      Status:   $($ingestRes.statusCode)"

    # 8. Test Ingestion with Bearer <apiKey> in Authorization header
    Write-Host "`n8. Testing M2M telemetry ingestion with 'Authorization: Bearer <apiKey>'..."
    $bearerM2mHeaders = @{
        "Authorization" = "Bearer $($keyRes.rawApiKey)"
        "Content-Type" = "application/json"
    }
    $ingestBearerRes = Invoke-RestMethod -Uri "http://localhost:8081/api/events" -Method Post -Headers $bearerM2mHeaders -Body $eventPayload
    Write-Host "   -> Event ingested with Bearer token successfully! ID: $($ingestBearerRes.id)"

    # 9. Verify BOLA / IDOR protection: Key from App A cannot submit for App B
    Write-Host "`n9. Verifying BOLA/IDOR protection (Key from App A cannot ingest for App B)..."
    $bolaPayload = @{
        applicationId = $createdDirectApp.id
        source = "API_GATEWAY"
        timestamp = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
        sourceIp = "198.51.100.77"
        method = "POST"
        path = "/api/v1/transfer"
        statusCode = 200
    } | ConvertTo-Json

    try {
        $null = Invoke-RestMethod -Uri "http://localhost:8081/api/events" -Method Post -Headers $m2mHeaders -Body $bolaPayload
        throw "Expected BOLA request to be blocked with 403 Forbidden, but it succeeded!"
    } catch {
        Write-Host "   -> Properly blocked! Error: $($_.Exception.Message)"
    }

    # 10. Revoke the API key
    Write-Host "`n10. Revoking API Key $($keyRes.id)..."
    $revokeRes = Invoke-RestMethod -Uri "http://localhost:8081/api/applications/$($createdApp.id)/api-keys/$($keyRes.id)/revoke" -Method Post -Headers $headers
    Write-Host "   -> Revocation response status: $($revokeRes.status)"

    # 11. Test that revoked key is immediately rejected
    Write-Host "`n11. Testing ingestion with revoked API key..."
    try {
        $null = Invoke-RestMethod -Uri "http://localhost:8081/api/events" -Method Post -Headers $m2mHeaders -Body $eventPayload
        throw "Expected revoked key to be rejected, but it succeeded!"
    } catch {
        Write-Host "   -> Revoked key successfully rejected! Error: $($_.Exception.Message)"
    }

    Write-Host "`n>>> ALL APP REGISTRATION AND KEY GENERATION TESTS PASSED! <<<"
}

Test-Endpoint
