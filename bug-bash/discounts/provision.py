"""Fresh test-mode discount fixtures via source-verified backend routes."""
import json, time
from backend_probe import request, BASE, ROOT
status, raw=request(BASE+'/publishable_key?merchant=US')
assert status==200 and json.loads(raw)['publishable_key'].startswith('pk_test_')
fixtures={}
for label, coupon, restriction in [('percent', {'percent_off':25},{}),('fixed',{'amount_off':1000,'currency':'usd'},{}),('minimum',{'percent_off':10},{'restrictions':{'minimum_amount':20000,'minimum_amount_currency':'usd'}}),('full',{'percent_off':100},{})]:
 def create(route,params):
  status,raw=request(BASE+'/'+route,json.dumps({'merchant':'US','stripe_version':'2026-08-26.preview','request_params':params}).encode(),{'Content-Type':'application/json'})
  parsed=json.loads(raw)
  if status!=200: raise RuntimeError((status,parsed))
  assert not parsed.get('livemode',False)
  return parsed
 c=create('create_coupon',{'duration':'once','name':'Discount bugbash '+label,**coupon})
 code='BASH'+label.upper()+str(int(time.time()))
 p=create('create_promotion_code',{'promotion':{'type':'coupon','coupon':c['id']},'code':code,**restriction})
 fixtures[label]={'coupon_id':c['id'],'code':p['code'],'coupon':coupon,'restriction':restriction}
(ROOT/'fixtures.json').write_text(json.dumps(fixtures))
print(json.dumps(fixtures,indent=2))
