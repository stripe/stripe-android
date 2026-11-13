"""Minimal real-backend comparison. Retain only sanitized results, never secrets."""
import copy, json, urllib.parse, uuid
from pathlib import Path
from backend_probe import BASE, ROOT, request
base=json.loads(Path('bug-bash/discounts/creation-allow-promotion.json').read_text())
base.pop('allow_promotion_codes')
fixtures=json.loads((ROOT/'fixtures.json').read_text())
key=json.loads(request(BASE+'/publishable_key?merchant=US')[1])['publishable_key']
assert key.startswith('pk_test_')
results=[]
for label,extra in [('plain',{}),('allow',{'allow_promotion_codes':True}),('coupon',{'discounts':[{'coupon':fixtures['percent']['coupon_id']}]})]:
 params=copy.deepcopy(base);params.update(extra)
 status,raw=request(BASE+'/create_checkout_session',json.dumps({'merchant':'US','stripe_version':'2026-08-26.preview','request_params':params}).encode(),{'Content-Type':'application/json'})
 data=json.loads(raw)
 item={'case':label,'http_status':status}
 if status==200:
  secret=data['client_secret'];assert secret.startswith('cs_test_')
  sid=secret.split('_secret_')[0]
  for code in [fixtures['percent']['code'],'']:
   payload={'client_secret':secret,'locale':'en-US','mobile_session_id':str(uuid.uuid4()),'mobile_app_id':'com.stripe.android.paymentsheet.example','promotion_code':code}
   st,body=request('https://api.stripe.com/v1/checkout_client/'+sid+'/update',urllib.parse.urlencode(payload).encode(),{'Authorization':'Bearer '+key,'Stripe-Version':'2020-03-02','Content-Type':'application/x-www-form-urlencoded'})
   err=json.loads(body).get('error',{})
   item.setdefault('updates',[]).append({'operation':'apply' if code else 'remove','http_status':st,'error':{k:v for k,v in err.items() if k in ['type','code','param','message']}})
 else: item['error']=data.get('error')
 results.append(item)
assert results[0]['http_status']==200
assert results[1]['http_status']==400 and results[2]['http_status']==400
assert all(x['http_status']==400 and x['error']['param']=='promotion_code' for x in results[0]['updates'])
print(json.dumps(results,indent=2))
