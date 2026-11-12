"""Scoped native LPM follow-up; unchanged SDK with real test backend.

Set ANDROID_SERIAL and LPMS_NATIVE_STATE to fresh private paths. Run serve,
prepare CASE, then observed UI actions. Restore before stopping the relay.
Only supported creation options are injected; responses are unchanged.
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
import urllib.parse
import xml.etree.ElementTree as ET

sys.dont_write_bytecode = True
os.environ['EMAIL_NATIVE_STATE'] = os.environ['LPMS_NATIVE_STATE']
spec = importlib.util.spec_from_file_location('email_native', Path(__file__).parent.parent / 'email-collection/native_probe.py')
email = importlib.util.module_from_spec(spec)
spec.loader.exec_module(email)
base, api, adb, STATE = email.base, email.api, email.adb, email.STATE
OUT = Path(__file__).with_name('native')
OUT.mkdir(exist_ok=True)
email.OUT = base.OUT = api.EVIDENCE = OUT
PREF, PACKAGE = base.PREF, base.PACKAGE


def save(name, value):
    def clean(item):
        if isinstance(item, dict):
            return {k: None if k == 'passive_captcha' else clean(v) for k, v in item.items()}
        if isinstance(item, list):
            return [clean(v) for v in item]
        if isinstance(item, str) and ('hooks.stripe.com/' in item or 'redirect.stripe.com/' in item):
            return '[REDACTED redirect URL]'
        return item
    (OUT / (name + '.json')).write_text(json.dumps(api.redact(clean(value)), indent=2) + '\n')


class Relay(base.Relay):
    def do_POST(self):
        body = json.loads(self.rfile.read(int(self.headers['Content-Length'])))
        if self.path == '/create_checkout_session':
            params = body['request_params']
            params['adaptive_pricing'] = {'enabled': False}
            if base.load('case.json') != 'default':
                params.setdefault('saved_payment_method_options', {})['allow_redisplay_filters'] = [
                    'always', 'limited', 'unspecified']
            save(base.load('case.json') + '-creation', body)
        self.forward(json.dumps(body).encode())


def prepare(case):
    assert case in ('default', 'mixed', 'visa-only', 'no-visa', 'sepa-only', 'bancontact')
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
    key = api.request(api.BASE + '/publishable_key?merchant=US')['publishable_key']
    assert key.startswith('pk_test_')
    api.private('key.json', {'publishable_key': key})
    if not (STATE / 'fixture.json').exists():
        customer = api.backend('create_customer', {'email': 'native-lpms@example.com'})['id']
        methods = []
        for method, token, redisplay in [('card', 'tok_visa', 'always'), ('card', 'tok_mastercard', 'limited'),
                                         ('card', 'tok_amex', 'unspecified'), ('sepa_debit', None, 'always')]:
            params = {'type': method, 'allow_redisplay': redisplay,
                      'billing_details[name]': 'Native LPM Tester', 'billing_details[email]': 'native-lpms@example.com',
                      'billing_details[address][country]': 'US', 'billing_details[address][postal_code]': '94103',
                      'billing_details[address][line1]': '510 Townsend St',
                      'billing_details[address][city]': 'San Francisco', 'billing_details[address][state]': 'CA'}
            params['card[token]' if method == 'card' else 'sepa_debit[iban]'] = token or 'DE89370400440532013000'
            pm = api.request('https://api.stripe.com/v1/payment_methods', urllib.parse.urlencode(params).encode(),
                             {'Authorization': 'Bearer ' + key})
            assert pm['livemode'] is False
            api.backend('attach_payment_method', {'customer': customer}, payment_method_id=pm['id'])
            methods.append(pm)
        fixture = {'customer': customer, 'methods': methods}
        api.private('fixture.json', fixture)
        save('fixture', fixture)
    customer = base.load('fixture.json')['customer']
    types = 'sepa_debit' if case == 'sepa-only' else 'bancontact' if case == 'bancontact' else 'card,sepa_debit,bancontact'
    values = {'session.backend_url': 'http://127.0.0.1:' + str(base.load('port.json')),
              'session.customer': 'returning', 'session.customer_id': customer,
              'session.customer_email': '', 'session.merchant': 'US', 'session.currency': 'eur',
              'session.automatic_tax': 'false', 'session.automatic_payment_methods': 'false',
              'session.payment_method_types': types, 'session.payment_method_save': 'false',
              'session.payment_method_remove': 'false', 'session.billing_address_collection': 'false',
              'session.shipping_address_collection': 'false', 'controller.payment_element.should_set_configuration': 'true',
              'payment.link.display': 'Never', 'payment.google_pay.display': 'Never'}
    if case in ('visa-only', 'no-visa'):
        values.update({'payment.card_brand_acceptance.mode': 'Allowed' if case == 'visa-only' else 'Disallowed',
                       'payment.card_brand_acceptance.visa': 'true', 'payment.card_brand_acceptance.mastercard': 'false',
                       'payment.card_brand_acceptance.amex': 'false', 'payment.card_brand_acceptance.discover': 'false'})
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
    print('Fresh backup retained; native case prepared: ' + case)


def tree():
    adb('shell', 'uiautomator', 'dump', '/sdcard/lpms-native-ui.xml')
    data = adb('exec-out', 'cat', '/sdcard/lpms-native-ui.xml').stdout
    (STATE / 'ui.xml').write_bytes(data)
    return ET.fromstring(data)


base.tree = tree


def snapshot(name, poll=False):
    assert base.load(base.load('case.json') + '-session.json') == base.load('session.json')
    result = base.client('poll' if poll else 'retrieve')
    save(name, result)
    print(json.dumps({k: result.get(k) for k in ('status', 'confirmation_status', 'payment_status', 'ordered_payment_method_types_and_wallets')}))


def bank_evidence():
    root = tree()
    assert any(n.get('text') == 'Bancontact test payment page' for n in root.iter('node'))
    for node in root.iter('node'):
        value = node.get('text', '')
        if value.startswith('{'):
            try:
                data = json.loads(value)
            except ValueError:
                continue
            if data.get('object') in ('payment_intent', 'payment_method'):
                assert data['livemode'] is False
                api.private('bank-' + data['object'] + '.json', data)
                save('bancontact-bank-' + data['object'], data)
    for node in root.iter('node'):
        for attr in ('text', 'content-desc'):
            value = node.get(attr, '')
            if 'http' in value or '_secret_' in value or node.get('resource-id', '').endswith('url_bar'):
                node.set(attr, '[REDACTED URL or capability-bearing object]')
    (OUT / 'bancontact-bank-redacted.xml').write_bytes(ET.tostring(root))
    raw = STATE / 'bank-screen.png'
    raw.write_bytes(adb('exec-out', 'screencap', '-p').stdout)
    try:
        subprocess.run(['sips', '-c', '1320', '1080', '--cropOffset', '290', '0', str(raw),
                        '--out', str(OUT / 'bancontact-bank-cropped.png')], check=True, stdout=subprocess.DEVNULL)
    finally:
        raw.unlink()


def final_intent():
    intent = base.load('bank-payment_intent.json')
    key = base.load('key.json')['publishable_key']
    assert key.startswith('pk_test_')
    query = urllib.parse.urlencode({'client_secret': intent['client_secret'], 'expand[]': 'payment_method'})
    result = api.request('https://api.stripe.com/v1/payment_intents/' + intent['id'] + '?' + query,
                         headers={'Authorization': 'Bearer ' + key})
    save('bancontact-native-payment-intent-final', result)
    print(json.dumps({k: result.get(k) for k in ('status', 'amount', 'currency', 'livemode')}))


if __name__ == '__main__':
    action = sys.argv[1]
    if action == 'serve':
        server = http.server.ThreadingHTTPServer(('127.0.0.1', 0), Relay)
        api.private('port.json', server.server_port)
        adb('reverse', 'tcp:' + str(server.server_port), 'tcp:' + str(server.server_port))
        print('LPM backend relay ready', flush=True)
        try:
            server.serve_forever()
        finally:
            server.server_close()
    elif action == 'prepare':
        prepare(sys.argv[2])
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
        adb('shell', 'input', 'text', shlex.quote(sys.argv[2].replace(' ', '%s')))
    elif action == 'shot':
        root = tree()
        assert all(n.get('package') == PACKAGE for n in root.iter('node')), 'Dismiss system UI first'
        assert all('@' not in n.get('text', '') or n.get('text', '').endswith('@example.com') for n in root.iter('node'))
        (OUT / (sys.argv[2] + '.xml')).write_bytes(ET.tostring(root))
        (OUT / (sys.argv[2] + '.png')).write_bytes(adb('exec-out', 'screencap', '-p').stdout)
    elif action in ('snapshot', 'poll'):
        snapshot(sys.argv[2], action == 'poll')
    elif action == 'bank':
        bank_evidence()
    elif action == 'final-intent':
        final_intent()
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
        adb('shell', 'rm', '-f', '/sdcard/lpms-native-ui.xml', '/sdcard/lpms-window.xml')
        adb('shell', 'am', 'start', '--user', '0', '-n', PACKAGE + '/.MainActivity')
        print('Fresh preferences restored exactly, own mapping/XML removed; main menu launched')
    else:
        raise RuntimeError('Unknown action')
