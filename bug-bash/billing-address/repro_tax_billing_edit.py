"""Live-device regression; requires a debug example app and an unlocked physical device.

Build/install from the repository root:
  ./gradlew :paymentsheet-example:assembleBaseDebug \
    -PSTRIPE_PAYMENTSHEET_EXAMPLE_USES_CLEARTEXT_TRAFFIC=true -PDIRECTLY_TO_PLAYGROUND=false
  adb -s DEVICE install -r paymentsheet-example/build/outputs/apk/base/debug/paymentsheet-example-base-debug.apk
Run:
  ANDROID_SERIAL=DEVICE python3 bug-bash/billing-address/repro_tax_billing_edit.py

Creates test-mode Stripe objects. All Stripe responses are real. A loopback relay
adds customer_update[address]=auto to the playground's session creation request.
The test restores playground preferences and removes its adb reverse mapping.
"""

import http.server
import json
import os
from pathlib import Path
import re
import subprocess
import threading
import time
import unittest
import urllib.error
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET


BASE = "https://stp-mobile-playground-backend-v7.stripedemos.com"
PACKAGE = "com.stripe.android.paymentsheet.example"
PREFERENCES = "shared_prefs/CheckoutElementsControllerPlaygroundSettings.xml"


def request(url, data=None, headers=None):
    req = urllib.request.Request(url, data=data, headers=headers or {})
    try:
        with urllib.request.urlopen(req, timeout=45) as response:
            return json.load(response)
    except urllib.error.HTTPError as error:
        raise RuntimeError(error.read().decode()) from None


def backend(endpoint, params, **extra):
    return request(
        BASE + "/" + endpoint,
        json.dumps({"merchant": "us_tax", "request_params": params, **extra}).encode(),
        {"Content-Type": "application/json"},
    )


class Relay(http.server.BaseHTTPRequestHandler):
    def log_message(self, *_args):
        pass

    def do_GET(self):
        self.forward(None)

    def do_POST(self):
        body = json.loads(self.rfile.read(int(self.headers["Content-Length"])))
        if self.path == "/create_checkout_session":
            body["request_params"].setdefault("customer_update", {})["address"] = "auto"
        self.forward(json.dumps(body).encode())

    def forward(self, body):
        req = urllib.request.Request(
            BASE + self.path, data=body, headers={"Content-Type": "application/json"}
        )
        try:
            with urllib.request.urlopen(req, timeout=45) as response:
                data, status = response.read(), response.status
        except urllib.error.HTTPError as error:
            data, status = error.read(), error.code
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)


class BillingTaxEditTest(unittest.TestCase):
    def adb(self, *args, data=None, check=True):
        return subprocess.run(
            ["adb", "-s", self.serial, *args], input=data, check=check,
            stdout=subprocess.PIPE, stderr=subprocess.PIPE,
        )

    def setUp(self):
        self.serial = os.environ["ANDROID_SERIAL"]
        self.assertFalse(self.serial.startswith("emulator-"), "Use the authorized physical device")
        self.previous = self.adb("shell", "run-as", PACKAGE, "cat", PREFERENCES, check=False)
        self.server = http.server.ThreadingHTTPServer(("127.0.0.1", 0), Relay)
        self.port = self.server.server_port
        threading.Thread(target=self.server.serve_forever, daemon=True).start()
        self.addCleanup(self.restore)
        self.adb("reverse", f"tcp:{self.port}", f"tcp:{self.port}")

    def restore(self):
        self.adb("shell", "am", "force-stop", "--user", "0", PACKAGE)
        if self.previous.returncode == 0:
            self.write_preferences(self.previous.stdout)
        else:
            self.adb("shell", "run-as", PACKAGE, "rm", "-f", PREFERENCES)
        self.adb("reverse", "--remove", f"tcp:{self.port}", check=False)
        self.server.shutdown()
        self.server.server_close()

    def write_preferences(self, contents):
        self.adb(
            "shell", "run-as", PACKAGE, "sh", "-c", f"'cat > {PREFERENCES}'", data=contents
        )

    def tree(self):
        self.adb("shell", "uiautomator", "dump", "/sdcard/billing-tax-test.xml")
        return ET.fromstring(self.adb("exec-out", "cat", "/sdcard/billing-tax-test.xml").stdout)

    def wait_for(self, predicate, timeout=40):
        deadline = time.monotonic() + timeout
        while time.monotonic() < deadline:
            root = self.tree()
            if predicate(root):
                return root
        self.fail("UI condition timed out")

    @staticmethod
    def find(root, label):
        return next((n for n in root.iter("node") if n.get("text") == label), None)

    def tap(self, node):
        self.assertIsNotNone(node)
        x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds")))
        self.adb("shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))

    def click(self, label):
        root = self.wait_for(lambda tree: self.find(tree, label) is not None)
        self.tap(self.find(root, label))

    def select_us(self):
        for _attempt in range(22):
            root = self.tree()
            country = next(
                (n for n in root.iter("node") if n.get("text", "").endswith(" United States")), None
            )
            if country is not None:
                self.tap(country)
                return
            scroll = next(n for n in root.iter("node") if n.get("scrollable") == "true")
            x1, y1, x2, y2 = map(int, re.findall(r"\d+", scroll.get("bounds")))
            self.adb("shell", "input", "swipe", str((x1 + x2) // 2), str(y1 + 100),
                     str((x1 + x2) // 2), str(y2 - 100), "200")
        self.fail("US country option not found")

    def test_saved_card_country_change_collects_us_tax_address(self):
        key = request(BASE + "/publishable_key?merchant=us_tax")["publishable_key"]
        self.assertTrue(key.startswith("pk_test_"))
        customer = backend("create_customer", {"email": "billing-bug-bash@example.com"})["id"]
        payment = request(
            "https://api.stripe.com/v1/payment_methods",
            urllib.parse.urlencode({
                "type": "card", "card[token]": "tok_visa", "allow_redisplay": "always",
                "billing_details[address][country]": "FR",
            }).encode(),
            {"Authorization": "Bearer " + key},
        )
        backend("attach_payment_method", {"customer": customer}, payment_method_id=payment["id"])
        values = {
            "session.backend_url": f"http://127.0.0.1:{self.port}",
            "session.customer": "returning", "session.customer_id": customer,
            "session.merchant": "us_tax", "session.automatic_tax": "true",
            "session.automatic_payment_methods": "false", "session.payment_method_types": "card",
            "session.billing_address_collection": "false", "payment.link.display": "Never",
            "payment.google_pay.display": "Never",
        }
        preferences = ET.Element("map")
        ET.SubElement(preferences, "string", name="settings_v1").text = json.dumps(values)
        self.adb("shell", "am", "force-stop", "--user", "0", PACKAGE)
        self.write_preferences(ET.tostring(preferences))
        self.adb("shell", "am", "start", "--user", "0", "-n", PACKAGE + "/.MainActivity")
        self.click("CheckoutElementsController Example")
        self.click("Create Checkout Session")
        self.click("Edit")
        self.click("France")
        self.select_us()
        root = self.wait_for(lambda tree: self.find(tree, "ZIP Code") is not None)
        labels = {n.get("text") for n in root.iter("node")}
        missing = {"City", "State"} - labels
        if not ("Address" in labels or "Address line 1" in labels):
            missing.add("Address")

        # Exercise the accepted incomplete edit to record the customer-facing consequence.
        if missing:
            self.tap(self.find(root, "ZIP Code"))
            self.adb("shell", "input", "text", "94103")
            self.adb("shell", "input", "keyevent", "4")
            self.click("Save")
            root = self.wait_for(lambda tree: self.find(tree, "No payment method selected") is not None)
            self.assertIsNone(self.find(root, "Edit"))
            evidence = Path(__file__).parent
            (evidence / "regression-card-disappeared.xml").write_bytes(ET.tostring(root))
            self.click("Confirm")
            self.wait_for(lambda tree: self.find(tree, "Confirmation failed: Something went wrong") is not None)

        self.assertFalse(missing, "Saved-card editor omitted required US tax fields: " + ", ".join(sorted(missing)))


if __name__ == "__main__":
    unittest.main(verbosity=2)
