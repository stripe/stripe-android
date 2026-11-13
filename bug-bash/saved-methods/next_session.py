#!/usr/bin/env python3
"""Create a fresh Session from the last test configuration; inspect backend filtering."""
import importlib.util,json,pathlib,sys
spec=importlib.util.spec_from_file_location('backend',pathlib.Path(__file__).with_name('backend_probe.py'));b=importlib.util.module_from_spec(spec);spec.loader.exec_module(b)
d=json.loads(b.PRIVATE.read_text());params=d['creation'].copy();params['customer']=d['customer_id'];params.pop('customer_email',None)
if len(sys.argv)>1: params.update(json.loads(sys.argv[1]))
status,raw=b.request(b.BASE+'/create_checkout_session',json.dumps({'merchant':'US','stripe_version':'2026-08-26.preview','request_params':params}).encode(),{'Content-Type':'application/json'})
assert status==200,(status,raw.decode())
r=json.loads(raw);assert r['client_secret'].startswith('cs_test_');b.private_update({'client_secret':r['client_secret'],'creation':params});b.probe('next-session')
