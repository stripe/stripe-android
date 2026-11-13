#!/usr/bin/env python3
"""Test-backend relay plus sanitized checkout-client retrieval. Secrets stay in /tmp."""
import hashlib
import http.server
import json
import os
import pathlib
import sys
import urllib.error
import urllib.parse
import urllib.request
import uuid

ROOT = pathlib.Path('/tmp/checkout-appearance')
PRIVATE = ROOT / 'backend-private.json'
BASE = 'https://stp-mobile-playground-backend-v7.stripedemos.com'
PORT = 18775

def request(url, data=None, headers=None):
    req = urllib.request.Request(url, data=data, headers=headers or {})
    try:
        with urllib.request.urlopen(req, timeout=45) as response:
            return response.status, response.read()
    except urllib.error.HTTPError as error:
        return error.code, error.read()

def private_update(values):
    current = json.loads(PRIVATE.read_text()) if PRIVATE.exists() else {}
    current.update(values)
    PRIVATE.write_text(json.dumps(current))
    os.chmod(PRIVATE, 0o600)

class Relay(http.server.BaseHTTPRequestHandler):
    def log_message(self, *args):
        pass

    def do_GET(self):
        self.forward()

    def do_POST(self):
        self.forward()

    def forward(self):
        body = self.rfile.read(int(self.headers.get('Content-Length', '0'))) or None
        if self.path.split('?')[0] not in ('/publishable_key', '/create_checkout_session'):
            self.send_error(404)
            return
        if self.path == '/create_checkout_session':
            params=json.loads(body)
            params['request_params']['adaptive_pricing']={'enabled': not (ROOT/'disabled').exists()}
            body=json.dumps(params).encode()
        status, raw = request(BASE + self.path, body, {'Content-Type': 'application/json'})
        parsed = json.loads(raw)
        if status == 200:
            key = parsed.get('publishable_key')
            if key:
                assert key.startswith('pk_test_'), 'Refusing non-test key'
                private_update({'publishable_key': key})
            secret = parsed.get('client_secret')
            if secret:
                assert secret.startswith('cs_test_'), 'Refusing non-test session'
                private_update({'client_secret': secret, 'creation': parsed})
        print(json.dumps({'endpoint': self.path.split('?')[0], 'status': status}), flush=True)
        self.send_response(status)
        self.send_header('Content-Type', 'application/json')
        self.send_header('Content-Length', str(len(raw)))
        self.end_headers()
        self.wfile.write(raw)

def probe(label):
    credentials = json.loads(PRIVATE.read_text())
    key, secret = credentials['publishable_key'], credentials['client_secret']
    assert key.startswith('pk_test_') and secret.startswith('cs_test_')
    session_id = secret.split('_secret_')[0]
    creation=credentials.get('creation', {})
    result = {'creation': {k: creation[k] for k in ('currency','amount_total','adaptive_pricing','presentment_details','livemode') if k in creation}, 'label': label, 'session_hash': hashlib.sha256(session_id.encode()).hexdigest()[:12]}
    for endpoint in ('poll', 'retrieve'):
        query = {'client_secret': secret}
        if endpoint == 'retrieve':
            query.update(locale='en-US', mobile_session_id=str(uuid.uuid4()),
                         mobile_app_id='com.stripe.android.paymentsheet.example', timezone='America/New_York')
        url = f'https://api.stripe.com/v1/checkout_client/{session_id}/{endpoint}?{urllib.parse.urlencode(query)}'
        status, raw = request(url, headers={'Authorization': 'Bearer ' + key, 'Stripe-Version': '2020-03-02'})
        parsed = json.loads(raw)
        (ROOT / ('raw-' + endpoint + '.json')).write_text(json.dumps(parsed))
        result[endpoint+'_keys']=list(parsed)
        fields = ('status', 'confirmation_status', 'payment_status', 'livemode', 'currency', 'amount_total',
                  'total_summary', 'adaptive_pricing_info', 'integration_currency', 'presentment_currency', 'ordered_payment_method_types_and_wallets', 'items', 'allowed_payment_method_types', 'presentment_details', 'adaptive_pricing', 'currency_conversion', 'line_items', 'localized_prices', 'payment_method_types')
        result[endpoint] = {'http_status': status, **{k: v for k, v in parsed.items() if k in fields}}
        if 'items' in result[endpoint]:
            result[endpoint]['items'] = [{'name': i['price']['product']['name'], 'quantity': i['quantity'], 'currency': i['price']['currency'], 'unit_amount': i['unit_amount'], 'subtotal': i['subtotal'], 'total': i['total']} for group in parsed['items'] for i in group.get('one_time_price', {}).get('items', [])]
        if 'error' in parsed:
            result[endpoint]['error'] = {k: v for k, v in parsed['error'].items() if k in ('type', 'code', 'param')}
    print(json.dumps(result, indent=2))

if __name__ == '__main__':
    if sys.argv[1] == 'serve':
        http.server.ThreadingHTTPServer(('127.0.0.1', PORT), Relay).serve_forever()
    elif sys.argv[1] == 'probe':
        probe(sys.argv[2])
