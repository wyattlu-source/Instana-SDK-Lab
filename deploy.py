import requests
from requests.auth import HTTPDigestAuth
import sys

JBOSS_HOST = "http://10.107.85.67:9990"
USER = "jbossadmin"
PASS = "P@ssw0rd"
WAR = "target/camping-api.war"
APP = "camping-api.war"

auth = HTTPDigestAuth(USER, PASS)

print(f"[1] Uploading {WAR} to JBoss...")
with open(WAR, "rb") as f:
    r = requests.post(
        f"{JBOSS_HOST}/management/add-content",
        auth=auth,
        files={"file": (APP, f, "application/octet-stream")},
        timeout=60
    )
r.raise_for_status()
hash_val = r.json()["result"]["BYTES_VALUE"]
print(f"    hash: {hash_val}")

print("[2] Deploying (full-replace)...")
r2 = requests.post(
    f"{JBOSS_HOST}/management",
    auth=auth,
    json={
        "operation": "full-replace-deployment",
        "name": APP,
        "content": [{"hash": {"BYTES_VALUE": hash_val}}],
        "enabled": True
    },
    timeout=60
)
r2.raise_for_status()
print(f"    result: {r2.json()}")
print("Deploy complete.")
