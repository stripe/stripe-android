"""Native adaptive-pricing follow-up using unchanged SDK and real test backend.

Set ANDROID_SERIAL and ADAPTIVE_BASH_STATE to a private owned directory.
Run serve, prepare FR, then observed tree/tap/shot/snapshot/poll actions.
The backend relay only records creation; requests and responses are unchanged.
Finish with restore and stop serve. See RUN_NOTES.md for actual native steps.
"""
import http.server
import importlib.util
import json
import os
from pathlib import Path
import sys
import urllib.error
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET

sys.dont_write_bytecode = True
os.environ['CUSTOMER_BASH_STATE'] = os.environ['ADAPTIVE_BASH_STATE']
spec = importlib.util.spec_from_file_location('base_native', Path(__file__).parent.parent / 'customer-logic/native_probe.py')
base = importlib.util.module_from_spec(spec)
spec.loader.exec_module(base)
OUT = Path(__file__).parent / 'native'
OUT.mkdir(exist_ok=True)
base.OUT = OUT
STATE, adb, api = base.STATE, base.adb, base.api
api.EVIDENCE = OUT
PREF, PACKAGE = base.PREF, base.PACKAGE


def save(name, data):
    def clean(value):
        if isinstance(value, dict):
            return {k: None if k == 'passive_captcha' else clean(v) for k, v in value.items()}
        if isinstance(value, list):
            return [clean(v) for v in value]
        return value
    (OUT / (name + '.json')).write_text(json.dumps(api.redact(clean(data)), indent=2) + '\n')


class Relay(base.Relay):
    def do_POST(self):
        body = self.rfile.read(int(self.headers['Content-Length']))
        if self.path == '/create_checkout_session':
            save(base.load('case.json') + '-creation', json.loads(body))
        self.forward(body)


def prepare(country):
    assert not (STATE / 'backed-up.json').exists()
    names = [PREF, 'com.stripe.android.PaymentConfiguration.xml', 'DefaultPrefsRepository.xml']
    present = []
    for name in names:
        result = adb('shell', 'run-as', PACKAGE, 'cat', 'shared_prefs/' + name, check=False)
        if result.returncode == 0:
            path = STATE / ('original-' + name)
            path.write_bytes(result.stdout)
            path.chmod(0o600)
            present.append(name)
    api.private('backed-up.json', {'names': names, 'present': present})
    key = api.request(api.BASE + '/publishable_key?merchant=US')['publishable_key']
    assert key.startswith('pk_test_')
    api.private('key.json', {'publishable_key': key})
    customer = api.backend('create_customer', {'email': 'test+location_' + country + '@example.com'})['id']
    pm = api.request('https://api.stripe.com/v1/payment_methods', urllib.parse.urlencode({
        'type': 'card', 'card[token]': 'tok_visa', 'allow_redisplay': 'always',
        'billing_details[name]': 'Adaptive Pricing Test', 'billing_details[address][country]': country,
    }).encode(), {'Authorization': 'Bearer ' + key})
    assert pm['livemode'] is False
    api.backend('attach_payment_method', {'customer': customer}, payment_method_id=pm['id'])
    api.private('fixture.json', {'customer': customer, 'payment_method': pm['id']})
    save('fixture', {'customer': customer, 'payment_method': pm['id'], 'card': pm['card'], 'country': country})
    values = {'session.backend_url': 'http://127.0.0.1:' + str(base.load('port.json')),
              'session.customer': 'returning', 'session.customer_id': customer,
              'session.customer_email': 'test+location_' + country + '@example.com',
              'session.adaptive_pricing_country': country,
              'controller.currency_selector.should_set_configuration': 'true',
              'session.merchant': 'US', 'session.currency': 'usd', 'session.automatic_tax': 'false',
              'session.automatic_payment_methods': 'false', 'session.payment_method_types': 'card',
              'session.payment_method_save': 'true', 'session.payment_method_remove': 'false',
              'session.billing_address_collection': 'false', 'session.shipping_address_collection': 'false',
              'payment.link.display': 'Never', 'payment.google_pay.display': 'Never'}
    root = ET.Element('map')
    original = STATE / ('original-' + PREF)
    if original.exists():
        root = ET.fromstring(original.read_bytes())
    item = root.find("string[@name='settings_v1']")
    if item is None:
        item = ET.SubElement(root, 'string', name='settings_v1')
    item.text = json.dumps(values)
    api.private('case.json', 'france')
    adb('shell', 'am', 'force-stop', '--user', '0', PACKAGE)
    base.write_pref(PREF, ET.tostring(root))
    adb('shell', 'am', 'start', '--user', '0', '-n', PACKAGE + '/.MainActivity')
    print('Fresh backups and synthetic saved Visa prepared; initial country ' + country)


def tree():
    adb('shell', 'uiautomator', 'dump', '/sdcard/adaptive-native-ui.xml')
    data = adb('exec-out', 'cat', '/sdcard/adaptive-native-ui.xml').stdout
    (STATE / 'ui.xml').write_bytes(data)
    return ET.fromstring(data)

base.tree = tree


def snapshot(name, poll=False):
    assert base.load(base.load('case.json') + '-session.json') == base.load('session.json'), 'Create the named native case first'
    result = base.client('poll' if poll else 'retrieve')
    save(name, result)
    fields = ('status', 'confirmation_status', 'payment_status', 'presentment_currency', 'total_summary')
    print(json.dumps({k: result.get(k) for k in fields}))


if __name__ == '__main__':
    action = sys.argv[1]
    if action == 'serve':
        server = http.server.ThreadingHTTPServer(('127.0.0.1', 0), Relay)
        api.private('port.json', server.server_port)
        adb('reverse', 'tcp:' + str(server.server_port), 'tcp:' + str(server.server_port))
        print('Unmodified backend relay ready', flush=True)
        try:
            server.serve_forever()
        finally:
            server.server_close()
    elif action == 'prepare':
        prepare(sys.argv[2])
    elif action == 'case':
        api.private('case.json', sys.argv[2])
    elif action == 'tree':
        for node in tree().iter('node'):
            if node.get('text') or node.get('content-desc') or node.get('class') == 'android.widget.EditText':
                print(json.dumps({key: node.get(key) for key in ('text', 'content-desc', 'class', 'bounds', 'enabled', 'checked', 'selected')}))
    elif action == 'tap':
        base.tap(sys.argv[2])
    elif action == 'shot':
        root = tree()
        assert all(n.get('package') == PACKAGE for n in root.iter('node')), 'Dismiss system UI first'
        (OUT / (sys.argv[2] + '.xml')).write_bytes(ET.tostring(root))
        (OUT / (sys.argv[2] + '.png')).write_bytes(adb('exec-out', 'screencap', '-p').stdout)
    elif action in ('snapshot', 'poll'):
        snapshot(sys.argv[2], action == 'poll')
    elif action == 'restore':
        adb('shell', 'am', 'force-stop', '--user', '0', PACKAGE)
        backed = base.load('backed-up.json')
        for name in backed['names']:
            if name in backed['present']:
                original = (STATE / ('original-' + name)).read_bytes()
                base.write_pref(name, original)
                assert adb('shell', 'run-as', PACKAGE, 'cat', 'shared_prefs/' + name).stdout == original
            else:
                adb('shell', 'run-as', PACKAGE, 'rm', '-f', 'shared_prefs/' + name)
        adb('reverse', '--remove', 'tcp:' + str(base.load('port.json')))
        adb('shell', 'rm', '-f', '/sdcard/adaptive-native-ui.xml')
        adb('shell', 'am', 'start', '--user', '0', '-n', PACKAGE + '/.MainActivity')
        print('Fresh preferences restored exactly, own mapping/XML removed; main menu launched')
    else:
        raise RuntimeError('Unknown action')
