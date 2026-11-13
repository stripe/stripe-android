"""Bounded, observed navigation of the native country selector."""
import subprocess, xml.etree.ElementTree as E, re, sys
A=['adb','-s','2B211JEGR05252']
for _ in range(35):
 subprocess.check_output(A+['shell','uiautomator','dump','/sdcard/checkout-tax.xml'])
 nodes=list(E.fromstring(subprocess.check_output(A+['exec-out','cat','/sdcard/checkout-tax.xml'])).iter('node'))
 matches=[n for n in nodes if n.get('text','').endswith(' '+sys.argv[1])]
 if matches:
  x,y,r,b=map(int,re.findall(r'\d+',matches[0].get('bounds')))
  subprocess.check_call(A+['shell','input','tap',str((x+r)//2),str((y+b)//2)]);break
 labels=[n.get('text') for n in nodes if n.get('text')]
 print(labels[0:1],flush=True)
 subprocess.check_call(A+['shell','input','swipe','450','740','450','1680','450'])
else: raise SystemExit('Country not reached')
