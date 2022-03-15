#!/bin/python
import requests
import time
import os
from requests.auth import HTTPBasicAuth
from requests_toolbelt.multipart import encoder

user=os.getenv('BROWSERSTACK_USERNAME')
authKey=os.getenv('BROWSERSTACK_ACCESS_KEY')
apk="paymentsheet-example/build/outputs/apk/debug/paymentsheet-example-debug.apk"
espressoApk="paymentsheet-example/build/outputs/apk/androidTest/debug/paymentsheet-example-debug-androidTest.apk"

# curl -u "user:authKey" -X POST "https://api-cloud.browserstack.com/app-automate/upload" -F "file=@apk"
# RETURN: {"app_url":"bs://527636cd103e394cb10e57d3ef0d57bcd0e0f433"}
print("Uploading the file: " + apk)
url = "https://api-cloud.browserstack.com/app-automate/upload"
files = { 'file': ('paymentSheet.apk', open(apk, 'rb')), }
response = requests.post(url, files=files, auth=(user, authKey))
appUrl = response.json()["app_url"]
print("App url: " + appUrl)

# curl -u "user:authKey" -X POST "https://api-cloud.browserstack.com/app-automate/espresso/test-suite" -F "file=@espressoApk"
# RETURN:  {"test_url":"bs://xxx"}
print("Uploading the file: " + apk)
url = "https://api-cloud.browserstack.com/app-automate/espresso/test-suite"
files = { 'file': ('paymentSheet-espresso.apk', open(espressoApk, 'rb')), }
response = requests.post(url, files=files, auth=(user, authKey))
testUrl = response.json()["test_url"]
print("test_url: " + testUrl)

# curl -u ""user:authKey"" \
#   -X POST "https://api-cloud.browserstack.com/app-automate/espresso/v2/build" \
#   -d '{"app": "bs://xxx",
# "testSuite": "bs://xxx",
#  "devices": ["Samsung Galaxy S9 Plus-9.0"]}' \
#   -H "Content-Type: application/json"
print("Execute the tests: " + apk)
url="https://api-cloud.browserstack.com/app-automate/espresso/v2/build"
# firefox doesn't work on this samsung: Samsung Galaxy S9 Plus-9.0"]
response = requests.post(url, json={"app": appUrl, "devices": ["Google Pixel 3-9.0"], "testSuite": testUrl}, auth=(user, authKey))
jsonResponse = response.json()
print(jsonResponse)
if(jsonResponse["message"] == "Success"):
   buildId = jsonResponse["build_id"]
   print("build id: " + buildId)
   url="https://api-cloud.browserstack.com/app-automate/espresso/v2/builds/"+ buildId
   responseStatus="running"
   print("Waiting for tests to complete.")
   while(responseStatus == "running"):
       time.sleep(60)
       print(".")
       response = requests.get(url, auth=(user, authKey))
       responseStatus = response.json()["status"]
   print("DONE.")

   # No uploaded files need to be deleted.

   sys.exit(0)
else:
   sys.exit(1)


