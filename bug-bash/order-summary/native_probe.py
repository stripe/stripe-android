"""Native order-summary follow-up using unchanged SDK and real test backend.

Set ANDROID_SERIAL and SUMMARY_NATIVE_STATE to a private owned directory.
Run serve, prepare details or prepare tax, then observed UI actions.
The relay injects the previously validated supported cart items at creation; responses are unchanged.
Each prepare restarts the native example with supported playground settings.
It preserves the first fresh preference backup until restore.
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
os.environ['CUSTOMER_BASH_STATE'] = os.environ['SUMMARY_NATIVE_STATE']
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
        body = json.loads(self.rfile.read(int(self.headers['Content-Length'])))
        if self.path == '/create_checkout_session':
            case = base.load('case.json')
            fixture = json.loads((OUT.parent / ('details-creation.json' if case == 'details' else 'inclusive-tax-creation.json')).read_text())
            params = body['request_params']
            params['items'] = fixture['items']
            params['adaptive_pricing'] = {'enabled': False}
            save(case + '-creation', body)
        self.forward(json.dumps(body).encode())


def prepare(case):
    assert case in ('details', 'tax')
    if not (STATE / 'backed-up.json').exists():
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
    merchant = 'US' if case == 'details' else 'us_tax'
    key = api.request(api.BASE + '/publishable_key?merchant=' + merchant)['publishable_key']
    assert key.startswith('pk_test_')
    api.private('key.json', {'publishable_key': key})
    customer = api.backend('create_customer', {'email': 'native-summary@example.com'}, merchant=merchant)['id']
    pm = api.request('https://api.stripe.com/v1/payment_methods', urllib.parse.urlencode({
        'type': 'card', 'card[token]': 'tok_visa', 'allow_redisplay': 'always',
        'billing_details[name]': 'Order Summary Test', 'billing_details[address][country]': 'US',
        'billing_details[address][line1]': '510 Townsend St', 'billing_details[address][city]': 'San Francisco',
        'billing_details[address][state]': 'CA', 'billing_details[address][postal_code]': '94103',
    }).encode(), {'Authorization': 'Bearer ' + key})
    assert pm['livemode'] is False
    api.backend('attach_payment_method', {'customer': customer}, merchant=merchant, payment_method_id=pm['id'])
    save(case + '-fixture', {'customer': customer, 'payment_method': pm['id'], 'billing_details': pm['billing_details']})
    values = {'session.backend_url': 'http://127.0.0.1:' + str(base.load('port.json')),
              'session.customer': 'returning', 'session.customer_id': customer,
              'session.customer_email': '', 'session.merchant': merchant, 'session.currency': 'usd',
              'session.automatic_tax': str(case == 'tax').lower(),
              'session.automatic_payment_methods': 'false', 'session.payment_method_types': 'card',
              'session.payment_method_save': 'false', 'session.payment_method_remove': 'false',
              'session.billing_address_collection': 'false',
              'session.shipping_address_collection': str(case == 'tax').lower(),
              'controller.shipping_address.should_set_configuration': str(case == 'tax').lower(),
              'controller.payment_element.should_set_configuration': 'true',
              'payment.link.display': 'Never', 'payment.google_pay.display': 'Never'}
    if case == 'tax':
        values.update({'controller.defaults.shipping.enabled': 'true',
                       'controller.defaults.shipping.name': 'Summary Native Recipient',
                       'controller.defaults.shipping.address.enabled': 'true',
                       'controller.defaults.shipping.address.country': 'US',
                       'controller.defaults.shipping.address.line1': '510 Townsend St',
                       'controller.defaults.shipping.address.line2': 'Suite 17',
                       'controller.defaults.shipping.address.city': 'San Francisco',
                       'controller.defaults.shipping.address.state': 'CA',
                       'controller.defaults.shipping.address.postal_code': '94103'})
    root = ET.Element('map')
    original = STATE / ('original-' + PREF)
    if original.exists():
        root = ET.fromstring(original.read_bytes())
    item = root.find("string[@name='settings_v1']")
    if item is None:
        item = ET.SubElement(root, 'string', name='settings_v1')
    item.text = json.dumps(values)
    api.private('case.json', case)
    save(case + '-settings', values)
    adb('shell', 'am', 'force-stop', '--user', '0', PACKAGE)
    base.write_pref(PREF, ET.tostring(root))
    adb('shell', 'am', 'start', '--user', '0', '-n', PACKAGE + '/.MainActivity')
    print('Fresh backup retained; test-mode native case prepared: ' + case)


def tree():
    adb('shell', 'uiautomator', 'dump', '/sdcard/summary-native-ui.xml')
    data = adb('exec-out', 'cat', '/sdcard/summary-native-ui.xml').stdout
    (STATE / 'ui.xml').write_bytes(data)
    return ET.fromstring(data)

base.tree = tree


def snapshot(name, poll=False):
    assert base.load(base.load('case.json') + '-session.json') == base.load('session.json'), 'Create the named native case first'
    result = base.client('poll' if poll else 'retrieve')
    save(name, result)
    fields = ('status', 'confirmation_status', 'payment_status', 'automatic_tax', 'total_summary')
    print(json.dumps({k: result.get(k) for k in fields}))


if __name__ == '__main__':
    action = sys.argv[1]
    if action == 'serve':
        server = http.server.ThreadingHTTPServer(('127.0.0.1', 0), Relay)
        api.private('port.json', server.server_port)
        adb('reverse', 'tcp:' + str(server.server_port), 'tcp:' + str(server.server_port))
        print('Order-summary creation relay ready', flush=True)
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
            if node.get('package') != PACKAGE:
                if node.get('text') in ('No thanks', 'Not now'):
                    print('System prompt: ' + node.get('text'))
                continue
            if node.get('text') or node.get('content-desc') or node.get('class') == 'android.widget.EditText':
                print(json.dumps({key: node.get(key) for key in ('text', 'content-desc', 'class', 'bounds', 'enabled', 'checked', 'selected')}))
    elif action == 'tap':
        base.tap(sys.argv[2])
    elif action == 'enter':
        import shlex
        adb('shell', 'input', 'text', shlex.quote(sys.argv[2].replace(' ', '%s')))
    elif action == 'shot':
        root = tree()
        assert all(n.get('package') == PACKAGE for n in root.iter('node')), 'Dismiss system UI first'
        assert all('@' not in n.get('text', '') or n.get('text', '').endswith('@example.com') for n in root.iter('node'))
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
        adb('shell', 'rm', '-f', '/sdcard/summary-native-ui.xml')
        adb('shell', 'am', 'start', '--user', '0', '-n', PACKAGE + '/.MainActivity')
        print('Fresh preferences restored exactly, own mapping/XML removed; main menu launched')
    else:
        raise RuntimeError('Unknown action')
