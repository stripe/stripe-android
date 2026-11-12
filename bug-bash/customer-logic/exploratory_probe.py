"""Customer-logic live-device probes; see RUN_NOTES.md for the exercised cases.

Run with ANDROID_SERIAL set to the physical device and CUSTOMER_BASH_STATE set
to a fresh private scratch directory. Build the example with cleartext enabled.
`serve` forwards the real playground backend unchanged; `restore` restores the
original preference file. Stop the relay with Ctrl-C after restoring.
"""

import http.server
import json
import os
from pathlib import Path
import re
import shlex
import subprocess
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET


BASE = "https://stp-mobile-playground-backend-v7.stripedemos.com"
PACKAGE = "com.stripe.android.paymentsheet.example"
PREF = "shared_prefs/CheckoutElementsControllerPlaygroundSettings.xml"
SERIAL = os.environ["ANDROID_SERIAL"]
assert not SERIAL.startswith("emulator-")
STATE = Path(os.environ["CUSTOMER_BASH_STATE"])
STATE.mkdir(mode=0o700, parents=True, exist_ok=True)
EVIDENCE = Path(__file__).resolve().parent


def private(name, value):
    path = STATE / name
    path.write_text(json.dumps(value))
    path.chmod(0o600)


def request(url, data=None, headers=None):
    try:
        with urllib.request.urlopen(urllib.request.Request(url, data=data, headers=headers or {}), timeout=45) as r:
            return json.load(r)
    except urllib.error.HTTPError as e:
        error = json.loads(e.read())
        raise RuntimeError(redact(error)) from None


def redact(value):
    if isinstance(value, dict):
        return {k: redact(v) for k, v in value.items()
                if not any(s in k.lower() for s in ("secret", "key", "token"))}
    if isinstance(value, list):
        return [redact(v) for v in value]
    if isinstance(value, str):
        return re.sub(r"(?:pk|sk)_(?:test|live)_\w+|\w+_secret_\w+", "[REDACTED]", value)
    return value


def backend(endpoint, params, merchant="US", **extra):
    return request(BASE + "/" + endpoint,
                   json.dumps({"merchant": merchant, "request_params": params, **extra}).encode(),
                   {"Content-Type": "application/json"})


class Relay(http.server.BaseHTTPRequestHandler):
    def log_message(self, *_args):
        pass

    def do_GET(self):
        self.forward(None)

    def do_POST(self):
        self.forward(self.rfile.read(int(self.headers["Content-Length"])))

    def forward(self, body):
        req = urllib.request.Request(BASE + self.path, data=body, headers={"Content-Type": "application/json"})
        try:
            with urllib.request.urlopen(req, timeout=45) as response:
                data, status = response.read(), response.status
        except urllib.error.HTTPError as error:
            data, status = error.read(), error.code
        parsed = json.loads(data)
        if self.path.startswith("/publishable_key") and status == 200:
            assert parsed["publishable_key"].startswith("pk_test_")
            private("key.json", parsed)
        if self.path == "/create_customer" and status == 200:
            private("created-customer.json", parsed)
        if self.path == "/create_checkout_session":
            private("session-request.json", json.loads(body))
            private("session-response.json", parsed)
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)


def adb(*args, data=None, check=True):
    return subprocess.run(["adb", "-s", SERIAL, *args], input=data, check=check,
                          stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=30)


def tree():
    adb("shell", "input", "keyevent", "KEYCODE_WAKEUP")
    adb("shell", "uiautomator", "dump", "/sdcard/customer-logic-bash.xml")
    return ET.fromstring(adb("exec-out", "cat", "/sdcard/customer-logic-bash.xml").stdout)


def labels(root):
    return [(n.get("text") or n.get("content-desc"), n.get("class"), n.get("bounds"))
            for n in root.iter("node") if n.get("text") or n.get("content-desc")]


def find(root, label):
    return next((n for n in root.iter("node") if label in (n.get("text"), n.get("content-desc"))), None)


def wait(label, timeout=40):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        root = tree()
        if find(root, label) is not None:
            return root
    raise RuntimeError("Timed out waiting for " + label + ": " + str(labels(root)))


def tap(label):
    node = find(wait(label), label)
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds")))
    adb("shell", "input", "tap", str((x1+x2)//2), str((y1+y2)//2))


def write_preferences(contents):
    adb("shell", "run-as", PACKAGE, "sh", "-c", "'cat > " + PREF + "'", data=contents)


def setup(config):
    if not (STATE / "backed-up").exists():
        old = adb("shell", "run-as", PACKAGE, "cat", PREF, check=False)
        if old.returncode == 0:
            (STATE / "original.xml").write_bytes(old.stdout)
        (STATE / "backed-up").touch()
    adb("shell", "am", "force-stop", "--user", "0", PACKAGE)
    values = {
        "session.backend_url": "http://127.0.0.1:" + (STATE / "port").read_text(),
        "session.customer": "guest", "session.merchant": "US",
        "session.customer_email": "customer-bash@example.com",
        "session.automatic_payment_methods": "false", "session.payment_method_types": "card",
        "session.payment_method_save": "false", "payment.link.display": "Never",
        "payment.google_pay.display": "Never",
    }
    values.update(config)
    prefs = ET.Element("map")
    if (STATE / "original.xml").exists():
        prefs = ET.fromstring((STATE / "original.xml").read_bytes())
    item = prefs.find("string[@name='settings_v1']")
    if item is None:
        item = ET.SubElement(prefs, "string", name="settings_v1")
    item.text = json.dumps(values)
    write_preferences(ET.tostring(prefs))
    private("active-settings.json", values)
    adb("shell", "am", "start", "--user", "0", "-n", PACKAGE + "/.MainActivity")


def snapshot(name):
    session = json.loads((STATE / "session-response.json").read_text())
    secret = session["client_secret"]
    key = json.loads((STATE / "key.json").read_text())["publishable_key"]
    params = {"client_secret": secret, "locale": "en-US", "mobile_session_id": "customer-bug-bash",
              "mobile_app_id": PACKAGE}
    result = request("https://api.stripe.com/v1/checkout_client/" + secret.split("_secret_")[0] +
                     "/retrieve?" + urllib.parse.urlencode(params), headers={"Authorization": "Bearer " + key})
    private("latest.json", result)
    selected = {k: result.get(k) for k in ("id", "status", "customer", "customer_email", "customer_creation",
                "saved_payment_method_options", "collected_information", "total_summary", "payment_status")}
    selected["creation_request"] = json.loads((STATE / "session-request.json").read_text())["request_params"]
    (EVIDENCE / (name + ".json")).write_text(json.dumps(redact(selected), indent=2) + "\n")
    print(json.dumps(redact(selected), indent=2))


def provision(name, token):
    key = request(BASE + "/publishable_key?merchant=US")["publishable_key"]
    assert key.startswith("pk_test_")
    customer = backend("create_customer", {"email": "customer-" + name + "@example.com"})["id"]
    pm = request("https://api.stripe.com/v1/payment_methods", urllib.parse.urlencode({
        "type": "card", "card[token]": token, "allow_redisplay": "always",
        "billing_details[name]": "Customer " + name,
        "billing_details[address][country]": "US", "billing_details[address][postal_code]": "94103",
    }).encode(), {"Authorization": "Bearer " + key})
    backend("attach_payment_method", {"customer": customer}, payment_method_id=pm["id"])
    private(name + ".json", {"session.customer": "returning", "session.customer_id": customer})
    print("Created test customer " + name + " with " + pm["card"]["brand"] + " " + pm["card"]["last4"])


def restore():
    adb("shell", "am", "force-stop", "--user", "0", PACKAGE)
    original = STATE / "original.xml"
    if original.exists():
        write_preferences(original.read_bytes())
    else:
        adb("shell", "run-as", PACKAGE, "rm", "-f", PREF)
    adb("shell", "rm", "-f", "/sdcard/customer-logic-bash.xml")
    adb("shell", "am", "start", "--user", "0", "-n", PACKAGE + "/.MainActivity")
    print("Original preferences restored; main activity launch requested")


def backend_controls():
    key = request(BASE + "/publishable_key?merchant=US")["publishable_key"]
    assert key.startswith("pk_test_")
    private("key.json", {"publishable_key": key})
    base_params = {
        "ui_mode": "mobile_elements", "currency": "usd", "allowed_payment_method_types": ["card"],
        "items": [{"type": "one_time_price", "one_time_price": {"items": [{
            "price_data": {"currency": "usd", "unit_amount": 12000, "tax_behavior": "exclusive",
                           "product_data": {"name": "Customer logic control"}}, "quantity": 1,
        }]}}],
    }
    results = []

    def run(name, additions, expected_customer=None):
        params = {**base_params, **additions}
        session = backend("create_checkout_session", params, stripe_version="2026-08-26.preview")
        secret = session["client_secret"]
        response = request("https://api.stripe.com/v1/checkout_client/" + secret.split("_secret_")[0] + "/init",
                           urllib.parse.urlencode({"client_secret": secret, "locale": "en-US",
                            "mobile_session_id": "customer-bug-bash", "mobile_app_id": PACKAGE,
                            "adaptive_pricing[allowed]": "false"}).encode(),
                           {"Authorization": "Bearer " + key})
        if expected_customer is not None:
            assert response["customer"]["id"] == expected_customer
        private(name + "-session.json", session)
        selected = {k: response.get(k) for k in ("id", "status", "customer", "customer_email",
                    "customer_creation", "saved_payment_method_options", "payment_status")}
        results.append({"case": name, "creation_parameters": additions, "response": redact(selected)})
        (EVIDENCE / "backend-controls.json").write_text(json.dumps(results, indent=2) + "\n")
        print(name + ": customer_present=" + str(response.get("customer") is not None) +
              ", customer_email=" + str(response.get("customer_email")), flush=True)
        return response

    guest = run("guest_no_customer", {"customer_email": "guest-customer-bash@example.com"})
    assert guest.get("customer") is None
    assert guest["customer_email"] == "guest-customer-bash@example.com"
    run("guest_create_always", {"customer_email": "guest-save-bash@example.com", "customer_creation": "always",
        "saved_payment_method_options": {"payment_method_save": "enabled", "payment_method_remove": "enabled"}})
    customer = backend("create_customer", {"email": "new-customer-bash@example.com"})["id"]
    private("new.json", {"session.customer": "returning", "session.customer_id": customer})
    fresh = run("new_explicit_customer", {"customer": customer,
        "saved_payment_method_options": {"payment_method_save": "enabled", "payment_method_remove": "enabled"}}, customer)
    assert (fresh.get("customer_email") or fresh["customer"].get("email")) == "new-customer-bash@example.com"
    assert fresh["customer"]["payment_methods"] == []
    for name, token in (("alpha", "tok_visa"), ("beta", "tok_mastercard")):
        provision(name, token)
        cid = json.loads((STATE / (name + ".json")).read_text())["session.customer_id"]
        r = run("returning_" + name, {"customer": cid,
            "saved_payment_method_options": {"payment_method_save": "disabled", "payment_method_remove": "enabled"}}, cid)
        assert len(r["customer"]["payment_methods"]) == 1
        assert (r.get("customer_email") or r["customer"].get("email")) == "customer-" + name + "@example.com"
    assert (results[-1]["response"]["customer"]["payment_methods"][0]["id"] !=
            results[-2]["response"]["customer"]["payment_methods"][0]["id"])
    blank = backend("create_customer", {})["id"]
    r = run("customer_without_email", {"customer": blank}, blank)
    assert r["customer"].get("email") is None
    (EVIDENCE / "backend-controls.json").write_text(json.dumps(results, indent=2) + "\n")
    print("Six backend controls passed. This does not establish native SDK UI behavior.")


if __name__ == "__main__":
    command = sys.argv[1]
    if command == "serve":
        server = http.server.ThreadingHTTPServer(("127.0.0.1", 0), Relay)
        port = str(server.server_port)
        (STATE / "port").write_text(port)
        adb("reverse", "tcp:" + port, "tcp:" + port)
        print("Relay ready on port " + port, flush=True)
        try:
            server.serve_forever()
        except KeyboardInterrupt:
            pass
        finally:
            adb("reverse", "--remove", "tcp:" + port, check=False)
            server.server_close()
    elif command == "start":
        setup(json.loads(Path(sys.argv[2]).read_text()) if len(sys.argv) > 2 else {})
        tap("CheckoutElementsController Example")
        tap("Create Checkout Session")
        print(labels(wait("Confirm")))
    elif command == "tree":
        print(labels(tree()))
    elif command == "tap":
        tap(sys.argv[2])
    elif command == "enter":
        adb("shell", "input", "text", shlex.quote(sys.argv[2].replace(" ", "%s")))
    elif command == "snapshot":
        snapshot(sys.argv[2])
    elif command == "capture":
        root = tree()
        assert all(n.get("package") == PACKAGE for n in root.iter("node")), "Dismiss system UI before evidence capture"
        (EVIDENCE / (sys.argv[2] + ".xml")).write_bytes(ET.tostring(root))
        (EVIDENCE / (sys.argv[2] + ".png")).write_bytes(adb("exec-out", "screencap", "-p").stdout)
    elif command == "provision":
        provision(sys.argv[2], sys.argv[3])
    elif command == "restore":
        restore()
    elif command == "backend-controls":
        backend_controls()
