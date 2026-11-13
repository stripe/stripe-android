#!/usr/bin/env python3
"""Native Pixel helper for this bug bash; uses fresh UI hierarchy for each tap."""
import json
import pathlib
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

SERIAL = "2B211JEGR05252"
PACKAGE = "com.stripe.android.paymentsheet.example"
SCRATCH = pathlib.Path("/tmp/checkout-tax")
SCRATCH.mkdir(exist_ok=True)

def adb(*args, **kw):
    return subprocess.check_output(["adb", "-s", SERIAL, *args], **kw)

def nodes():
    adb("shell", "uiautomator", "dump", "/sdcard/checkout-tax.xml")
    raw = adb("exec-out", "cat", "/sdcard/checkout-tax.xml")
    return ET.fromstring(raw).iter("node")

command, *args = sys.argv[1:]
if command == "dump":
    observed = list(nodes())
    if any(n.get("package", "").startswith(("com.google.android.gms", "com.google.android.apps.walletnfcrel")) for n in observed) or any(n.get("text") == "Autofill payment info" for n in observed):
        print("External wallet/autofill UI detected. Contents suppressed; use privacy-safe wallet summary or return.")
        sys.exit(0)
    for n in observed:
        a = dict(n.attrib)
        if 'Google Pay' in a.get('content-desc', ''):
            a['content-desc'] = re.sub(r'Google Pay.*', 'Google Pay [wallet details suppressed]', a['content-desc'])
        if '•' in a.get('text', ''):
            a['text'] = '[wallet details suppressed]'
        if a.get("text") or a.get("content-desc") or a.get("class", "").endswith("EditText"):
            print(json.dumps({k: a[k] for k in ("text", "content-desc", "class", "enabled", "clickable", "bounds") if k in a}))
elif command == "tap":
    label = args[0]
    matches = [n for n in nodes() if label in (n.get("text"), n.get("content-desc"))]
    if len(matches) != 1:
        raise ValueError(f"Expected one match for {label!r}, got {len(matches)}")
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", matches[0].get("bounds")))
    adb("shell", "input", "tap", str((x1+x2)//2), str((y1+y2)//2))
elif command == "tap-prefix":
    matches = [n for n in nodes() if n.get('content-desc', '').startswith(args[0])]
    if len(matches) != 1: raise ValueError(f'Expected one prefix match, got {len(matches)}')
    x1,y1,x2,y2=map(int,re.findall(r'\d+',matches[0].get('bounds')))
    adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2))
elif command == "xy":
    adb("shell", "input", "tap", *args)
elif command == "text":
    adb("shell", "input", "text", args[0].replace(" ", "%s"))
elif command == "replace":
    adb("shell", "input", "keycombination", "KEYCODE_CTRL_LEFT", "KEYCODE_A")
    time.sleep(0.3)
    adb("shell", "input", "keyevent", "KEYCODE_DEL")
    time.sleep(0.3)
    if args[0]:
        adb("shell", "input", "text", args[0].replace(" ", "%s"))
elif command == "key":
    adb("shell", "input", "keyevent", *args)
elif command == "swipe":
    adb("shell", "input", "swipe", *args)
elif command == "shot":
    observed=list(nodes())
    if any(n.get('package','').startswith('com.google.android.gms') for n in observed):
        raise ValueError('Refusing screenshot of external wallet UI')
    if any('Google Pay' in n.get('content-desc','') for n in observed):
        raise ValueError('Disable wallet buttons before capturing email/lifecycle screenshots')
    pathlib.Path(args[0]).write_bytes(adb('exec-out','screencap','-p'))
elif command == "settings":
    values = json.loads(pathlib.Path(args[0]).read_text())
    adb("shell", "am", "force-stop", PACKAGE)
    original = SCRATCH / "original-settings.xml"
    root = ET.fromstring(original.read_bytes())
    entry = root.find("./string[@name='settings_v1']")
    if entry is None:
        entry = ET.SubElement(root, "string", name="settings_v1")
    # Use only the explicit test configuration plus normal source defaults.
    entry.text = json.dumps(values)
    payload = ET.tostring(root, encoding="utf-8", xml_declaration=True)
    subprocess.run(["adb", "-s", SERIAL, "shell", "run-as", PACKAGE,
                    "sh", "-c", "'cat > shared_prefs/CheckoutElementsControllerPlaygroundSettings.xml'"],
                   input=payload, check=True)
elif command == "launch":
    adb("shell", "am", "start", "--user", "0", "-n", PACKAGE+"/.MainActivity")
elif command == "restore":
    adb("shell", "am", "force-stop", PACKAGE)
    subprocess.run(["adb", "-s", SERIAL, "shell", "run-as", PACKAGE,
                    "sh", "-c", "'cat > shared_prefs/CheckoutElementsControllerPlaygroundSettings.xml'"],
                   input=(SCRATCH/"original-settings.xml").read_bytes(), check=True)
    adb("shell", "rm", "-f", "/sdcard/checkout-tax.xml")
else:
    raise ValueError(command)
