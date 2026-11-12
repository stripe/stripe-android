"""Read-only assertions over the retained live order-summary evidence."""
import importlib.util
import json
from pathlib import Path
import re

OUT = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location('summary_probe', OUT / 'live_probe.py')
probe = importlib.util.module_from_spec(spec)
spec.loader.exec_module(probe)


def read(name):
    return json.loads((OUT / (name + '.json')).read_text())


for name in ('details', 'inclusive-tax', 'isk'):
    before = probe.summary(read(name + '-init'))
    assert probe.summary(read(name + '-retrieve')) == before
    current_name = 'inclusive-tax-sf-restored' if name == 'inclusive-tax' else name + '-init'
    current = probe.summary(read(current_name))
    paid = probe.summary(read(name + '-confirm'))
    current['status'] = 'complete'
    assert current == paid
    intent = read(name + '-payment-intent')
    assert intent['status'] == 'succeeded' and intent['amount'] == paid['totals']['total']
    assert intent['currency'] == paid['currency']

for name in ('inclusive-tax-sf', 'inclusive-tax-or', 'inclusive-tax-sf-restored'):
    response = read(name)
    assert probe.summary(response) == probe.summary(read(name + '-retrieve'))
    assert response['total_summary']['total'] == 4500
    assert response['total_summary']['tax_exclusive'] == 0
    assert response['total_summary']['tax_inclusive'] == (0 if name.endswith('-or') else 357)

for boundary in read('boundary-results'):
    assert boundary['accepted'] is False
assert 'same `tax_behavior`' in read('mixed-tax-error')['error']
assert 'do not support kwd' in read('kwd-error')['error']

bad_files = []
json_count = 0
for path in OUT.glob('*.json'):
    if path.name == 'evidence-validation.json':
        continue
    json.loads(path.read_text())
    json_count += 1
    if re.search(r'(?:pk|sk)_(?:test|live)_[A-Za-z0-9]+|[A-Za-z0-9]+_secret_[A-Za-z0-9]+', path.read_text()):
        bad_files.append(path.name)
assert not bad_files, bad_files
print(json.dumps({'status': 'passed', 'completed_payment_cases': 3, 'live_tax_updates': 3,
    'documented_backend_configuration_limits': 5, 'json_files_parsed': json_count,
    'retained_credential_matches': 0}, indent=2))
