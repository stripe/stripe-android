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
SCRATCH = pathlib.Path("/tmp/checkout-standard")
SCRATCH.mkdir(exist_ok=True)

def adb(*args, **kw):
    return subprocess.check_output(["adb", "-s", SERIAL, *args], **kw)

def nodes():
    adb("shell", "uiautomator", "dump", "/sdcard/checkout-standard.xml")
    raw = adb("exec-out", "cat", "/sdcard/checkout-standard.xml")
    return ET.fromstring(raw).iter("node")

command, *args = sys.argv[1:]
if command == "dump":
    observed = list(nodes())
    if any(n.get("package", "").startswith(("com.google.android.gms", "com.google.android.apps.walletnfcrel")) for n in observed) or any(n.get("text") == "Autofill payment info" for n in observed):
        print("External wallet/autofill UI detected. Contents suppressed; use privacy-safe wallet summary or return.")
        sys.exit(0)
    for n in observed:
        a = dict(n.attrib)
        if a.get('text','').startswith('{'): continue
        for field in ('text','content-desc'):
            if 'http' in a.get(field,'') or '_secret_' in a.get(field,'') or a.get('resource-id','').endswith('url_bar'):
                a[field] = '[URL/secret suppressed]'
        if 'Google Pay' in a.get('content-desc', ''):
            a['content-desc'] = re.sub(r'Google Pay.*', 'Google Pay [wallet details suppressed]', a['content-desc'])
        if '•' in a.get('text', ''):
            a['text'] = '[wallet details suppressed]'
        if a.get("text") or a.get("content-desc") or a.get("class", "").endswith("EditText"):
            print(json.dumps({k: a[k] for k in ("text", "content-desc", "class", "enabled", "clickable", "bounds") if k in a}))
elif command == "summary":
    from PIL import Image
    import io
    observed=list(nodes())
    if any(n.get('package','').startswith('com.google.android.gms') for n in observed):
        raise ValueError('Return to merchant UI before summary')
    im=Image.open(io.BytesIO(adb('exec-out','screencap','-p')))
    result=[]
    for n in observed:
        d=n.get('content-desc',''); t=n.get('text','')
        label = 'google_pay' if 'Google Pay' in d else 'link' if d=='Pay with Link' else t if t in ('Card','Payment complete!','Confirmation canceled','No shipping address') else None
        if label:
            b=list(map(int,re.findall(r'\d+',n.get('bounds'))))
            result.append({'label':label,'bounds':b,'background_rgb':im.getpixel((b[0]+8,(b[1]+b[3])//2)) if label=='google_pay' else None})
    print(json.dumps(result,indent=2))
elif command == "tap":
    label = args[0]
    matches = [n for n in nodes() if label in (n.get("text"), n.get("content-desc"))]
    if len(matches) != 1:
        raise ValueError(f"Expected one match for {label!r}, got {len(matches)}")
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", matches[0].get("bounds")))
    adb("shell", "input", "tap", str((x1+x2)//2), str((y1+y2)//2))
elif command == "domain":
    from urllib.parse import urlparse
    for n in nodes():
        if n.get("resource-id", "").endswith("url_bar"):
            t=n.get("text", ""); print(urlparse(t if "://" in t else "https://"+t).hostname)
elif command == "tap-prefix":
    matches = [n for n in nodes() if n.get('content-desc', '').startswith(args[0])]
    if len(matches) != 1: raise ValueError(f'Expected one prefix match, got {len(matches)}')
    x1,y1,x2,y2=map(int,re.findall(r'\d+',matches[0].get('bounds')))
    adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2))
elif command == "wallet-summary":
    observed=list(nodes())
    words=['TEST MODE','Test mode','Test environment','Cancel','Close','Pay','Continue','Test cards','Add card','Payment methods','Something went wrong','Google Pay','TEST','Error']
    for n in observed:
        t=n.get('text','')
        if t in words or re.fullmatch(r'\$[0-9,.]+',t): print(t, n.get('bounds'))
    print('packages:', sorted({n.get('package','') for n in observed}))
elif command == "xy":
    adb("shell", "input", "tap", *args)
elif command == "text":
    adb("shell", "input", "text", args[0].replace(" ", "%s"))
elif command == "replace":
    adb("shell", "input", "keycombination", "KEYCODE_CTRL_LEFT", "KEYCODE_A")
    time.sleep(0.3)
    adb("shell", "input", "keyevent", "KEYCODE_DEL")
    time.sleep(0.3)
    adb("shell", "input", "text", args[0].replace(" ", "%s"))
elif command == "key":
    adb("shell", "input", "keyevent", *args)
elif command == "swipe":
    adb("shell", "input", "swipe", *args)
elif command == "shot":
    observed=list(nodes())
    if any(n.get('package','').startswith('com.google.android.gms') for n in observed) or any(n.get('text')=='Autofill payment info' for n in observed):
        raise ValueError('Refusing wallet/autofill screenshot')
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
    adb("shell", "rm", "-f", "/sdcard/checkout-standard.xml")
else:
    raise ValueError(command)
