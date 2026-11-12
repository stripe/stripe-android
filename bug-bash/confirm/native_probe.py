"""Native Checkout confirm setup/relay, using real backend and unchanged SDK.

Set ANDROID_SERIAL to the authorized physical device. Run `serve` separately,
then `start candidate|control|global-off|returning-CASE`, `tree`, `tap LABEL`,
`enter TEXT`, `shot NAME`, `snapshot NAME`, and finally `restore` and stop serve.
Private credentials/backups live only in /tmp/confirm-native-state until cleanup.
"""
import http.server
import importlib.util
import json
import os
from pathlib import Path
import re
import shlex
import subprocess
import sys
import urllib.error
import urllib.request
import urllib.parse
import xml.etree.ElementTree as ET

sys.dont_write_bytecode = True
OUT = Path(__file__).resolve().parent / "native"
OUT.mkdir(exist_ok=True)
STATE = Path("/tmp/confirm-native-state")
STATE.mkdir(mode=0o700, exist_ok=True)
SERIAL = os.environ["ANDROID_SERIAL"]
assert not SERIAL.startswith("emulator-")
PACKAGE = "com.stripe.android.paymentsheet.example"
PREF = "CheckoutElementsControllerPlaygroundSettings.xml"
spec = importlib.util.spec_from_file_location("api", OUT.parent / "live_probe.py")
api = importlib.util.module_from_spec(spec)
spec.loader.exec_module(api)
api.api.OUT = OUT


def adb(*args, data=None, check=True):
    return subprocess.run(["adb", "-s", SERIAL, *args], input=data, check=check,
                          stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=25)


def private(name, value):
    path = STATE / name
    path.write_text(json.dumps(value))
    path.chmod(0o600)


def load(name):
    return json.loads((STATE / name).read_text())


class Relay(http.server.BaseHTTPRequestHandler):
    def log_message(self, *_args):
        pass

    def do_GET(self):
        self.forward(None)

    def do_POST(self):
        body = json.loads(self.rfile.read(int(self.headers["Content-Length"])))
        if self.path == "/create_checkout_session":
            body["request_params"].update(load("patch.json"))
            api.save(load("case.json") + "-creation", body)
        self.forward(json.dumps(body).encode())

    def forward(self, body):
        req = urllib.request.Request(api.api.BASE + self.path, data=body,
                                     headers={"Content-Type": "application/json"})
        try:
            with urllib.request.urlopen(req, timeout=45) as response:
                data, status = response.read(), response.status
        except urllib.error.HTTPError as error:
            data, status = error.read(), error.code
        parsed = json.loads(data)
        if status == 200:
            if self.path.startswith("/publishable_key"):
                assert parsed["publishable_key"].startswith("pk_test_")
                private("key.json", parsed)
            elif self.path == "/create_checkout_session":
                private("session.json", parsed)
                private(load("case.json") + "-session.json", parsed)
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)


def write_pref(name, data):
    adb("shell", "run-as", PACKAGE, "sh", "-c", "'cat > shared_prefs/" + name + "'", data=data)


def start(case):
    if not (STATE / "backed-up.json").exists():
        names = [PREF, "com.stripe.android.PaymentConfiguration.xml"]
        present = []
        for name in names:
            result = adb("shell", "run-as", PACKAGE, "cat", "shared_prefs/" + name, check=False)
            if result.returncode == 0:
                path = STATE / ("original-" + name)
                path.write_bytes(result.stdout)
                path.chmod(0o600)
                present.append(name)
        private("backed-up.json", {"names": names, "present": present})
    key = api.request(api.api.BASE + "/publishable_key?merchant=US")["publishable_key"]
    assert key.startswith("pk_test_")
    private("key.json", {"publishable_key": key})
    origin = case.removeprefix("returning-")
    returning = case.startswith("returning-")
    if returning:
        customer = load(origin + "-customer.json")
    else:
        customer = api.backend("create_customer", {"email": "native-confirm@example.com"})["id"]
        private(origin + "-customer.json", customer)
    patch = {"adaptive_pricing": {"enabled": False}}
    if case == "candidate":
        patch["payment_method_options"] = {"sepa_debit": {"setup_future_usage": "none"}}
    if case == "global-off":
        patch["payment_settings"] = {"setup_future_usage": "off_session"}
    if returning:
        patch["saved_payment_method_options"] = {"payment_method_save": "enabled", "allow_redisplay_filters": ["always", "limited", "unspecified"]}
    private("patch.json", patch)
    private("case.json", case)
    values = {"session.backend_url": "http://127.0.0.1:" + str(load("port.json")),
              "session.customer": "returning", "session.customer_id": customer,
              "session.merchant": "US", "session.currency": "eur", "session.automatic_tax": "false",
              "session.automatic_payment_methods": "false", "session.payment_method_types": "card,sepa_debit",
              "session.payment_method_save": "true", "session.payment_method_remove": "false",
              "session.billing_address_collection": "false", "session.shipping_address_collection": "false",
              "payment.link.display": "Never", "payment.google_pay.display": "Never"}
    preferences = ET.Element("map")
    original = STATE / ("original-" + PREF)
    if original.exists():
        preferences = ET.fromstring(original.read_bytes())
    item = preferences.find("string[@name='settings_v1']")
    if item is None:
        item = ET.SubElement(preferences, "string", name="settings_v1")
    item.text = json.dumps(values)
    adb("shell", "am", "force-stop", "--user", "0", PACKAGE)
    write_pref(PREF, ET.tostring(preferences))
    adb("shell", "am", "start", "--user", "0", "-n", PACKAGE + "/.MainActivity")
    print("Prepared " + case + "; open CheckoutElementsController Example")


def tree():
    adb("shell", "uiautomator", "dump", "/sdcard/confirm-native.xml")
    data = adb("exec-out", "cat", "/sdcard/confirm-native.xml").stdout
    (STATE / "ui.xml").write_bytes(data)
    return ET.fromstring(data)


def tap(label):
    root = tree()
    node = next((node for node in root.iter("node") if label in (node.get("text"), node.get("content-desc"))), None)
    if node is None:
        raise RuntimeError("No observed node: " + label)
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds")))
    adb("shell", "input", "tap", str((x1+x2)//2), str((y1+y2)//2))


def snapshot(name):
    session = load("session.json")
    key = load("key.json")["publishable_key"]
    response = api.client(session["client_secret"], "retrieve", key)
    api.save(name, response)
    print(json.dumps({"name": name, "status": response["status"],
                      "effective_sfu": response.get("setup_future_usage_for_payment_method_type"),
                      "saved_methods": api.inventory(response)}))


def poll(case):
    session = load(case + "-session.json")
    secret = session["client_secret"]
    url = "https://api.stripe.com/v1/checkout_client/" + secret.split("_secret_")[0] + "/poll?"
    response = api.request(url + urllib.parse.urlencode({"client_secret": secret}),
                           key=load("key.json")["publishable_key"])
    api.save(case + "-poll", response)
    print(json.dumps(response))


def restore():
    adb("shell", "am", "force-stop", "--user", "0", PACKAGE)
    backed = load("backed-up.json")
    for name in backed["names"]:
        if name in backed["present"]:
            write_pref(name, (STATE / ("original-" + name)).read_bytes())
        else:
            adb("shell", "run-as", PACKAGE, "rm", "-f", "shared_prefs/" + name)
    adb("reverse", "--remove", "tcp:" + str(load("port.json")), check=False)
    adb("shell", "rm", "-f", "/sdcard/confirm-native.xml", "/sdcard/confirm-native.mp4")
    adb("shell", "am", "start", "--user", "0", "-n", PACKAGE + "/.MainActivity")
    print("Original preferences restored; own mapping/scratch removed; main menu launched")


if __name__ == "__main__":
    action = sys.argv[1]
    if action == "serve":
        server = http.server.ThreadingHTTPServer(("127.0.0.1", 0), Relay)
        private("port.json", server.server_port)
        adb("reverse", "tcp:" + str(server.server_port), "tcp:" + str(server.server_port))
        print("Backend relay ready on loopback", flush=True)
        try:
            server.serve_forever()
        finally:
            server.server_close()
    elif action == "start":
        start(sys.argv[2])
    elif action == "tree":
        for node in tree().iter("node"):
            if node.get("text") or node.get("content-desc") or node.get("class") == "android.widget.EditText":
                print(json.dumps({key: node.get(key) for key in ("text", "content-desc", "class", "bounds", "enabled", "checked")}))
    elif action == "tap":
        tap(sys.argv[2])
    elif action == "enter":
        adb("shell", "input", "text", shlex.quote(sys.argv[2].replace(" ", "%s")))
    elif action == "shot":
        (OUT / (sys.argv[2] + ".png")).write_bytes(adb("exec-out", "screencap", "-p").stdout)
        (OUT / (sys.argv[2] + ".xml")).write_bytes(ET.tostring(tree()))
    elif action == "snapshot":
        snapshot(sys.argv[2])
    elif action == "poll":
        poll(sys.argv[2])
    elif action == "restore":
        restore()
    else:
        raise RuntimeError("Unknown action")
