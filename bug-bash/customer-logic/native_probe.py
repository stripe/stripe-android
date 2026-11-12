"""Customer native follow-up helper; real test backend, unchanged installed SDK.

Set ANDROID_SERIAL and CUSTOMER_BASH_STATE to an owned private directory.
Run serve, prepare, and observed UI actions; restore before stopping serve.
Only initial settings are injected. Later customer changes use native settings.
"""
import http.server
import importlib.util
import json
import os
from pathlib import Path
import re
import shlex
import sys
import urllib.error
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET

sys.dont_write_bytecode = True
spec = importlib.util.spec_from_file_location("prior", Path(__file__).with_name("exploratory_probe.py"))
api = importlib.util.module_from_spec(spec)
spec.loader.exec_module(api)
STATE, adb = api.STATE, api.adb
OUT = Path(__file__).with_name("native")
OUT.mkdir(exist_ok=True)
PREF = "CheckoutElementsControllerPlaygroundSettings.xml"
PACKAGE = api.PACKAGE


def load(name):
    return json.loads((STATE / name).read_text())


def save(name, value):
    (OUT / (name + ".json")).write_text(json.dumps(api.redact(value), indent=2) + "\n")


class Relay(http.server.BaseHTTPRequestHandler):
    def log_message(self, *_args):
        pass

    def do_GET(self):
        self.forward(None)

    def do_POST(self):
        body = json.loads(self.rfile.read(int(self.headers["Content-Length"])))
        if self.path == "/create_checkout_session":
            body["request_params"]["adaptive_pricing"] = {"enabled": False}
            save(load("case.json") + "-creation", body)
        self.forward(json.dumps(body).encode())

    def forward(self, body):
        request = urllib.request.Request(api.BASE + self.path, data=body,
                                         headers={"Content-Type": "application/json"})
        try:
            with urllib.request.urlopen(request, timeout=45) as response:
                data, status = response.read(), response.status
        except urllib.error.HTTPError as error:
            data, status = error.read(), error.code
        parsed = json.loads(data)
        if status == 200:
            if self.path.startswith("/publishable_key"):
                assert parsed["publishable_key"].startswith("pk_test_")
                api.private("key.json", parsed)
            elif self.path == "/create_checkout_session":
                api.private("session.json", parsed)
                api.private(load("case.json") + "-session.json", parsed)
            elif self.path == "/create_customer":
                api.private("new-customer.json", parsed)
                save(load("case.json") + "-created-customer", parsed)
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)


def write_pref(name, data):
    adb("shell", "run-as", PACKAGE, "sh", "-c", "'cat > shared_prefs/" + name + "'", data=data)


def prepare():
    assert not (STATE / "backed-up.json").exists(), "Do not replace an active run backup"
    names = [PREF, "com.stripe.android.PaymentConfiguration.xml", "DefaultPrefsRepository.xml"]
    present = []
    for name in names:
        result = adb("shell", "run-as", PACKAGE, "cat", "shared_prefs/" + name, check=False)
        if result.returncode == 0:
            path = STATE / ("original-" + name)
            path.write_bytes(result.stdout)
            path.chmod(0o600)
            present.append(name)
    api.private("backed-up.json", {"names": names, "present": present})
    key = api.request(api.BASE + "/publishable_key?merchant=US")["publishable_key"]
    assert key.startswith("pk_test_")
    api.private("key.json", {"publishable_key": key})
    values = {"session.backend_url": "http://127.0.0.1:" + str(load("port.json")),
              "session.customer": "guest", "session.customer_email": "native-customer@example.com",
              "session.merchant": "US", "session.currency": "usd", "session.automatic_tax": "false",
              "session.automatic_payment_methods": "false", "session.payment_method_types": "card",
              "session.payment_method_save": "true", "session.payment_method_remove": "true",
              "session.billing_address_collection": "false", "session.shipping_address_collection": "false",
              "payment.link.display": "Never", "payment.google_pay.display": "Never"}
    prefs = ET.Element("map")
    original = STATE / ("original-" + PREF)
    if original.exists():
        prefs = ET.fromstring(original.read_bytes())
    item = prefs.find("string[@name='settings_v1']")
    if item is None:
        item = ET.SubElement(prefs, "string", name="settings_v1")
    item.text = json.dumps(values)
    api.private("case.json", "guest")
    adb("shell", "am", "force-stop", "--user", "0", PACKAGE)
    write_pref(PREF, ET.tostring(prefs))
    adb("shell", "am", "start", "--user", "0", "-n", PACKAGE + "/.MainActivity")
    print("Fresh backups made; guest prepared; open CheckoutElementsController Example")


def tree():
    adb("shell", "uiautomator", "dump", "/sdcard/customer-native.xml")
    data = adb("exec-out", "cat", "/sdcard/customer-native.xml").stdout
    (STATE / "ui.xml").write_bytes(data)
    return ET.fromstring(data)


def tap(label):
    node = next((n for n in tree().iter("node") if label in (n.get("text"), n.get("content-desc"))), None)
    assert node is not None, "No observed node: " + label
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds")))
    adb("shell", "input", "tap", str((x1+x2)//2), str((y1+y2)//2))


def client(action):
    secret = load("session.json")["client_secret"]
    key = load("key.json")["publishable_key"]
    params = {"client_secret": secret, "locale": "en-US", "mobile_session_id": "customer-native-bash",
              "mobile_app_id": PACKAGE}
    if action == "poll":
        params = {"client_secret": secret}
    return api.request("https://api.stripe.com/v1/checkout_client/" + secret.split("_secret_")[0] +
                       "/" + action + "?" + urllib.parse.urlencode(params),
                       headers={"Authorization": "Bearer " + key})


def snapshot(name, action="retrieve"):
    result = client(action)
    save(name, result)
    print(json.dumps(api.redact({k: result.get(k) for k in
          ("status", "customer", "customer_email", "payment_status", "payment_intent", "payment_intent_status")})))


def prefs(name):
    result = adb("shell", "run-as", PACKAGE, "cat", "shared_prefs/" + PREF)
    root = ET.fromstring(result.stdout)
    values = json.loads(root.find("string[@name='settings_v1']").text)
    retained = {k: v for k, v in values.items() if k in ("session.customer", "session.customer_id", "session.customer_email")}
    retained["returning_customer_id"] = root.findtext("string[@name='returning_customer_id']")
    save(name, retained)
    print(json.dumps(retained))


def restore():
    adb("shell", "am", "force-stop", "--user", "0", PACKAGE)
    backed = load("backed-up.json")
    for name in backed["names"]:
        if name in backed["present"]:
            original = (STATE / ("original-" + name)).read_bytes()
            write_pref(name, original)
            assert adb("shell", "run-as", PACKAGE, "cat", "shared_prefs/" + name).stdout == original
        else:
            adb("shell", "run-as", PACKAGE, "rm", "-f", "shared_prefs/" + name)
    adb("reverse", "--remove", "tcp:" + str(load("port.json")), check=False)
    adb("shell", "rm", "-f", "/sdcard/customer-native.xml")
    adb("shell", "am", "start", "--user", "0", "-n", PACKAGE + "/.MainActivity")
    print("Fresh preference backups restored exactly; own mapping/XML removed; main menu launched")


if __name__ == "__main__":
    action = sys.argv[1]
    if action == "serve":
        server = http.server.ThreadingHTTPServer(("127.0.0.1", 0), Relay)
        api.private("port.json", server.server_port)
        adb("reverse", "tcp:" + str(server.server_port), "tcp:" + str(server.server_port))
        print("Customer relay ready", flush=True)
        try:
            server.serve_forever()
        finally:
            server.server_close()
    elif action == "prepare":
        prepare()
    elif action == "case":
        api.private("case.json", sys.argv[2])
    elif action == "tree":
        for node in tree().iter("node"):
            if node.get("text") or node.get("content-desc") or node.get("class") == "android.widget.EditText":
                print(json.dumps({key: node.get(key) for key in ("text", "content-desc", "class", "bounds", "enabled", "checked", "selected")}))
    elif action == "tap":
        tap(sys.argv[2])
    elif action == "enter":
        adb("shell", "input", "text", shlex.quote(sys.argv[2].replace(" ", "%s")))
    elif action == "enter-customer":
        customer = load("new-customer.json")["id"] if sys.argv[2] == "new" else load(sys.argv[2] + ".json")["session.customer_id"]
        adb("shell", "input", "text", shlex.quote(customer))
    elif action == "shot":
        root = tree()
        assert all(n.get("package") == PACKAGE for n in root.iter("node")), "Dismiss system UI first"
        (OUT / (sys.argv[2] + ".xml")).write_bytes(ET.tostring(root))
        (OUT / (sys.argv[2] + ".png")).write_bytes(adb("exec-out", "screencap", "-p").stdout)
    elif action == "snapshot":
        snapshot(sys.argv[2])
    elif action == "poll":
        snapshot(sys.argv[2], "poll")
    elif action == "prefs":
        prefs(sys.argv[2])
    elif action == "provision":
        api.provision(sys.argv[2], sys.argv[3])
    elif action == "restore":
        restore()
    else:
        raise RuntimeError("Unknown action")
