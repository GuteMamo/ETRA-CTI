#!/usr/bin/env python3
"""
ETRA-CTI External Application Security Event Integration Example (Python 3)

Demonstrates how an external application (Flask, Django, FastAPI, or background worker)
transmits normalized application security events to the ETRA-CTI platform via API Key.

Usage:
    export ETRA_APP_ID="<application-uuid>"
    export ETRA_API_KEY="etra_live_..."
    python examples/server_integration.py
"""

import os
import sys
import json
import urllib.request
import urllib.error
from datetime import datetime, timezone

ETRA_URL = os.environ.get("ETRA_URL", "http://localhost:8081/api")
APPLICATION_ID = os.environ.get("ETRA_APP_ID")
API_KEY = os.environ.get("ETRA_API_KEY")

if not APPLICATION_ID or not API_KEY:
    print("""
[!] Configuration missing.
    Please set environment variables:
      ETRA_APP_ID  - The UUID of your registered application in ETRA-CTI
      ETRA_API_KEY - The raw API key obtained from ETRA-CTI (e.g. etra_live_...)

    Example:
      $env:ETRA_APP_ID="<your-app-id>"
      $env:ETRA_API_KEY="<your-api-key>"
      python examples/server_integration.py
""")
    sys.exit(1)


def send_security_event(event_data):
    payload = {
        "applicationId": APPLICATION_ID,
        "source": event_data.get("source", "APPLICATION_LOG"),
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "sourceIp": event_data.get("sourceIp", "127.0.0.1"),
        "method": event_data.get("method", "POST"),
        "path": event_data.get("path", "/api/login"),
        "statusCode": event_data.get("statusCode", 401),
        "userId": event_data.get("userId", "anonymous"),
        "metadata": event_data.get("metadata", {})
    }

    req_data = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(
        f"{ETRA_URL}/events",
        data=req_data,
        headers={
            "Content-Type": "application/json",
            "X-ETRA-API-KEY": API_KEY,
            "User-Agent": "ETRA-Python-Integration/1.0"
        },
        method="POST"
    )

    print(f"[*] Sending security event to {ETRA_URL}/events...")
    try:
        with urllib.request.urlopen(req) as resp:
            resp_body = resp.read().decode("utf-8")
            result = json.loads(resp_body)
            print("[+] Security event accepted by ETRA-CTI!")
            print(f"    Event ID:   {result.get('id')}")
            print(f"    Timestamp:  {result.get('eventTime')}")
            print(f"    Normalized: {json.dumps(result.get('normalized'), indent=2)}")
            return result
    except urllib.error.HTTPError as e:
        error_msg = e.read().decode("utf-8", errors="replace")
        print(f"[-] HTTP Error {e.code}: {error_msg}")
        sys.exit(1)
    except urllib.error.URLError as e:
        print(f"[-] Connection Error: {e.reason}")
        sys.exit(1)


if __name__ == "__main__":
    # Test submission: suspicious path probing
    send_security_event({
        "source": "APPLICATION_LOG",
        "sourceIp": "198.51.100.88",
        "method": "GET",
        "path": "/.env",
        "statusCode": 404,
        "userId": "guest_session_102",
        "metadata": {
            "scanner": "Automated Probe",
            "referer": "http://external-scanner.test"
        }
    })
