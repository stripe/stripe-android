"""Validate recorded native LPM evidence without provisioning test objects."""
import ast
import json
from pathlib import Path
import re
import xml.etree.ElementTree as ET

OUT = Path(__file__).with_name('native')


def read(name):
    return json.loads((OUT / (name + '.json')).read_text())


def inventory(case):
    return (read(case + '-initial').get('customer') or {}).get('payment_methods', [])


def saved_endings(name):
    root = ET.parse(OUT / (name + '.xml'))
    return [re.sub(r'\D', '', node.get('content-desc')) for node in root.iter('node')
            if 'ending in' in node.get('content-desc', '')]


full = inventory('mixed')
assert [(p['type'], (p.get('card') or {}).get('brand'), p['allow_redisplay']) for p in full] == [
    ('sepa_debit', None, 'always'), ('card', 'amex', 'unspecified'),
    ('card', 'mastercard', 'limited'), ('card', 'visa', 'always')]
assert saved_endings('mixed-saved-all') == ['3000', '8431', '4444', '4242']
assert saved_endings('default-saved') == ['3000', '4242']
assert [p['id'] for p in inventory('default')] == [full[0]['id'], full[3]['id']]
for case in ('visa-only', 'no-visa'):
    assert [p['id'] for p in inventory(case)] == [p['id'] for p in full]
assert saved_endings('visa-only-saved') == ['3000', '4242']
assert saved_endings('no-visa-saved') == ['3000', '8431', '4444']
assert 'Mastercard is not accepted' in (OUT / 'visa-only-new-mastercard-rejected.xml').read_text()
assert [p['type'] for p in inventory('sepa-only')] == ['sepa_debit']
assert not inventory('bancontact')
assert read('sepa-only-initial')['ordered_payment_method_types_and_wallets'] == ['sepa_debit']
assert read('bancontact-initial')['ordered_payment_method_types_and_wallets'] == ['bancontact']
for name in ('sepa-completed-poll', 'bancontact-after-authorize'):
    poll = read(name)
    assert (poll['status'], poll['confirmation_status'], poll['payment_status']) == ('complete', 'succeeded', 'paid')
for name in ('sepa-before-confirm', 'bancontact-before-confirm'):
    assert read(name)['confirmation_status'] == 'not_started'
pending = read('bancontact-awaiting-authorization')
assert (pending['status'], pending['confirmation_status'], pending['payment_status']) == ('open', 'pending', 'unpaid')
before, after = read('bancontact-bank-payment_intent'), read('bancontact-native-payment-intent-final')
assert before['id'] == after['id']
assert before['status'] == 'requires_action' and before['next_action']['type'] == 'redirect_to_url'
assert after['status'] == 'succeeded' and after['amount'] == 12000 and after['currency'] == 'eur'
assert before['livemode'] is False and after['livemode'] is False
for case in ('mixed', 'default', 'visa-only', 'no-visa', 'sepa-only', 'bancontact'):
    assert read(case + '-initial')['total_summary']['total'] == 12000
for name in ('sepa-completed', 'bancontact-completed'):
    assert 'Payment complete!' in (OUT / (name + '.xml')).read_text()
amex = full[1]
assert amex['card']['brand'] == 'amex' and amex['card']['display_brand'] == 'american_express'
assert 'Unknown ending in 8 4 3 1' in (OUT / 'mixed-saved-all.xml').read_text()
assert 'Unknown ending in 8 4 3 1' in (OUT / 'no-visa-saved.xml').read_text()
for path in OUT.glob('*.json'):
    json.loads(path.read_text())
for path in OUT.glob('*.xml'):
    ET.parse(path)
for path in list(OUT.glob('*.json')) + list(OUT.glob('*.xml')):
    text = path.read_text()
    assert not re.search(r'(?:pk|sk)_(?:test|live)_\w+|\w+_secret_\w+', text), path
    assert not re.search(r'https://(?:hooks|redirect)\.stripe\.com/[^"\s]+', text), path
ast.parse(Path(__file__).with_name('native_probe.py').read_text())
print('Native evidence valid: six sessions, two native payments, one completed bank redirect, one Amex label defect.')
