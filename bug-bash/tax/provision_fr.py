"""Provision a synthetic test-card fixture with France-only billing."""
import json, urllib.parse
from backend_probe import PRIVATE, BASE, request
p=json.loads(PRIVATE.read_text()); key=p['publishable_key']; assert key.startswith('pk_test_')
status,raw=request('https://api.stripe.com/v1/payment_methods',urllib.parse.urlencode({'type':'card','card[token]':'tok_visa','billing_details[address][country]':'FR'}).encode(),{'Authorization':'Bearer '+key})
pm=json.loads(raw)
if status!=200: print({'create_status':status,'error_type':pm.get('error',{}).get('type')}); raise SystemExit(1)
assert pm['livemode'] is False
status,raw=request(BASE+'/attach_payment_method',json.dumps({'merchant':'us_tax','payment_method_id':pm['id'],'request_params':{'customer':p['customer_id']}}).encode(),{'Content-Type':'application/json'})
result=json.loads(raw)
print(json.dumps({'create_status':200,'attach_status':status,'address':pm.get('billing_details',{}).get('address'),'livemode':pm['livemode']}))
