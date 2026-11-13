#!/usr/bin/env python3
"""Minimal test-mode comparison; keys, IDs and secrets never written to disk."""
import copy
import hashlib
import json
import urllib.error
import urllib.request
from pathlib import Path
BASE = 'https://stp-mobile-playground-backend-v7.stripedemos.com'
VERSION = '2026-08-26.preview'
def request(route, params=None):
    data = None if params is None else json.dumps({'merchant':'US','stripe_version':VERSION,'request_params':params}).encode()
    req = urllib.request.Request(BASE+route, data=data, headers={'Content-Type':'application/json'})
    try:
        with urllib.request.urlopen(req, timeout=45) as r: return r.status, json.load(r)
    except urllib.error.HTTPError as e: return e.code, json.load(e)
status, key = request('/publishable_key?merchant=US')
assert status == 200 and key['publishable_key'].startswith('pk_test_')
status, customer = request('/create_customer', {'email':'migration.bugbash@example.com'})
assert status == 200 and customer['id'].startswith('cus_') and customer['livemode'] is False
base = {'ui_mode':'mobile_elements','customer':customer['id'], 'items':[{'type':'one_time_price','one_time_price':{'items':[{'price_data':{'currency':'usd','unit_amount':1099,'product_data':{'name':'T-shirt'}},'quantity':1}]}}]}
results = {'version':VERSION,'customer_created':True,'customer_livemode':False,'customer_hash':hashlib.sha256(customer['id'].encode()).hexdigest()[:12],'cases':{}}
for name, addition in [('plain',{}),('legacy',{'payment_intent_data':{'setup_future_usage':'off_session'}}),('current',{'payment_settings':{'setup_future_usage':'off_session'}})]:
    params = copy.deepcopy(base); params.update(addition)
    status, response = request('/create_checkout_session',params)
    record = {'http_status':status,'addition':addition}
    if status == 200:
        assert response['id'].startswith('cs_test_') and response['livemode'] is False
        record.update({k:response.get(k) for k in ('livemode','mode','ui_mode','status','payment_status','payment_settings','payment_intent_data')})
        record['session_hash'] = hashlib.sha256(response['id'].encode()).hexdigest()[:12]
    else:
        record['error'] = response.get('error',response)
    results['cases'][name] = record
safe_base=copy.deepcopy(base); safe_base['customer']='<same valid test Customer with synthetic email>'
results['base_request']=safe_base
Path(__file__).with_name('backend-contract-results.json').write_text(json.dumps(results,indent=2)+'\n')
print(json.dumps(results,indent=2))
assert results['cases']['plain']['http_status'] == 200
assert results['cases']['current']['http_status'] == 200
assert results['cases']['legacy']['http_status'] == 400
