#!/bin/python
import requests
import time
import os
from requests.auth import HTTPBasicAuth
from requests_toolbelt.multipart import encoder
import argparse
import sys
import json
import math

# These need to be set in environment variables.
user=os.getenv('BROWSERSTACK_USERNAME')
authKey=os.getenv('BROWSERSTACK_ACCESS_KEY')

# https://www.browserstack.com/docs/app-automate/api-reference/espresso/apps#list-uploaded-apps
def listApps():
    # print step description
    print("LISTING apps...", end='')
    url = "https://api-cloud.browserstack.com/app-automate/espresso/v2/apps"
    response = requests.get(url,auth=(user, authKey))
    if(response.status_code == 200):
        # print result
        print("DONE")
        print("Result:")
        print("| Uploaded at |    App id     | Expire at |")
        print("| ----------- | ------------- | --------- |")
        if(0 < len(response.json()["apps"])):
            for fileDescription in response.json()["apps"]:
                print("| {uploadDate} | {id} | {expireDate} |".format(
                      uploadDate=fileDescription["uploaded_at"],
                      id=fileDescription["app_id"],
                      expireDate=fileDescription["expiry"]
                  )
                )
            print("\n\n")
        else:
            print("NONE\n\n")
    else:
        print("DONE\nRESULT: " + str(response.status_code) + "\n" + str(response.json()))

# https://www.browserstack.com/docs/app-automate/api-reference/espresso/tests#list-uploaded-test-suites
def listEspressoApps():
    # print step description
    print("LISTING test apps...", end='')
    url = "https://api-cloud.browserstack.com/app-automate/espresso/v2/test-suites"
    response = requests.get(url,auth=(user, authKey))
    if(response.status_code == 200):
        # print result
        print("DONE")
        print("Result:")
        print("| Uploaded at | Test Suite id | Expire at |")
        print("| ----------- | ------------- | --------- |")
        if(0 < len(response.json()["test_suites"])):
            for fileDescription in response.json()["test_suites"]:
                print("| {uploadDate} | {id} | {expireDate} |".format(
                      uploadDate=fileDescription["uploaded_at"],
                      id=fileDescription["test_suite_id"],
                      expireDate=fileDescription["expiry"]
                  )
                )
            print("\n\n")
    else:
        print("DONE\nRESULT: " + str(response.status_code) + "\n" + str(response.json()))

# https://www.browserstack.com/docs/app-automate/api-reference/espresso/tests#delete-a-test-suite
def deleteTestSuite(testSuiteID):
     # print step description
     print("DELETING test app: {id} ...".format(id = testSuiteID), end='')
     url = "https://api-cloud.browserstack.com/app-automate/espresso/v2/test-suites/" + testSuiteID
     response = requests.delete(url,auth=(user, authKey))
     if(response.status_code == 200):
         # print result
         print("DONE\nResult: \n" + str(response.json()))
     else:
        print("DONE\nRESULT: " + str(response.status_code) + "\n" + str(response.json()))

# https://www.browserstack.com/docs/app-automate/api-reference/espresso/apps#upload-an-app
def uploadApk(apkFile):
    # print step description
    print("UPLOADING the file: {file}...".format(file=apkFile), end='')
    url = "https://api-cloud.browserstack.com/app-automate/upload"
    files = { 'file': ("paymentsheet-example.apk", open(apkFile, 'rb')), }
    response = requests.post(url, files=files, auth=(user, authKey))
    if(response.status_code == 200):
        appUrl = response.json()["app_url"]

        # print result
        print("DONE\nRESULT app url: " + appUrl)
        return appUrl
    else:
        print("DONE\nRESULT: " + str(response.status_code) + "\n" + str(response.json()))
        return None

def uploadAppLiveApk(apkFile):
    # print step description
    print("UPLOADING the file: {file}...".format(file=apkFile), end='')
    url = "https://api-cloud.browserstack.com/app-live/upload"
    files = { 'file': (os.path.basename(apkFile), open(apkFile, 'rb')), }
    response = requests.post(url, files=files, auth=(user, authKey))
    if(response.status_code == 200):
        appUrl = response.json()["app_url"]

        # print result
        print("DONE\nRESULT app url: " + appUrl)
        return appUrl
    else:
        print("DONE\nRESULT: " + str(response.status_code) + "\n" + str(response.json()))
        return None

# https://www.browserstack.com/docs/app-automate/api-reference/espresso/tests#upload-a-test-suite
def uploadEspressoApk(espressoApkFile):
    # print step description
    print("UPLOADING the file: {file}...".format(file=espressoApkFile), end='')

    url = "https://api-cloud.browserstack.com/app-automate/espresso/test-suite"
    files = { 'file': ('paymentSheet-espresso.apk', open(espressoApkFile, 'rb')), }
    response = requests.post(url, files=files, auth=(user, authKey))

    if(response.status_code == 200):
        testUrl = response.json()["test_url"]

        # print result
        print("DONE\nRESULT test url: " + testUrl)
        return testUrl
    else:
        print("DONE\nRESULT: " + str(response.status_code) + "\n" + str(response.json()))
        return None

def testShards(isNightly):
    testClassNames = [
        # Hard coded tests.
        "com.stripe.android.TestAuthorization",
        "com.stripe.android.TestBrowsers",
        "com.stripe.android.TestCustomers",
        "com.stripe.android.addresselement.AddressElementTest",
    ]

    # Add LPM tests to testClassNames.
    lpmFileNames = os.listdir('paymentsheet-example/src/androidTest/java/com/stripe/android/lpm')
    for fileName in lpmFileNames:
        if not fileName.endswith(".kt"):
            continue
        className = fileName[:-3]
        testClassNames.append(f"com.stripe.android.lpm.{className}")

    # We only have 25 parallel runs, and we want multiple PRs to run at the same time.
    numberOfShards = 8.0 if isNightly else 10.0
    testClassesPerShard = math.ceil(len(testClassNames) / numberOfShards)

    shards = []
    shardValues = []

    for testClass in testClassNames:
        shardValues.append(testClass)
        if len(shardValues) == testClassesPerShard:
            shards.append(
                {
                   "name": f"Shard {len(shards) + 1}",
                   "strategy": "class",
                   "values": shardValues
                },
            )
            shardValues = []

    if len(shardValues) > 0:
        shards.append(
            {
               "name": f"Shard {len(shards) + 1}",
               "strategy": "class",
               "values": shardValues
            },
        )

    return shards

# https://www.browserstack.com/docs/app-automate/api-reference/espresso/builds#execute-a-build
def executeTests(appUrl, testUrl, isNightly):
    print("RUNNING the tests (appUrl: {app}, testUrl: {test})..."
        .format(app=appUrl, test=testUrl),
         end=''
    )
    url="https://api-cloud.browserstack.com/app-automate/espresso/v2/build"
    # firefox doesn't work on this samsung: Samsung Galaxy S9 Plus-9.0"]
    shards = testShards(isNightly)
    devices = []
    if isNightly:
        devices = [
            "Google Pixel 7-13.0",
            "Samsung Galaxy S22-12.0",
        ]
    else:
        devices = [
            "Samsung Galaxy S22-12.0",
        ]
    response = requests.post(url, json={
         "app": appUrl,
         "devices": devices,
         "testSuite": testUrl,
         "networkLogs": True,
         "deviceLogs": True,
         "video": True,
         "acceptInsecureCerts": True,
         "locale": "en_US",
         "enableSpoonFramework": False,
         "project": "Mobile Payments",
         "shards": {
            "numberOfShards": len(shards),
            "mapping": shards,
         }
      }, auth=(user, authKey))
    jsonResponse = response.json()

    if(response.status_code == 200):
        # print result
        print("DONE\nRESULT build Started: " + jsonResponse["message"])
        if(jsonResponse["message"] == "Success"):
            print("RESULT build id: " + jsonResponse["build_id"])
            print("RESULT see build here: " +
                 "https://app-automate.browserstack.com/dashboard/v2/builds/{buildId}".format(buildId=jsonResponse["build_id"]))
            return jsonResponse["build_id"]
        else:
            return None
    else:
        print("DONE\nRESULT: " + str(response.status_code) + "\n" + str(response.json()))
        return None

# https://www.browserstack.com/docs/app-automate/api-reference/espresso/builds#get-build-status
def waitForBuildComplete(buildId):
       print("WAITING for build id: {buildId}...".format(buildId=buildId), end='')
       url="https://api-cloud.browserstack.com/app-automate/espresso/v2/builds/" + buildId
       responseStatus="running"

       while(responseStatus == "running"):
           time.sleep(10)
           print(".", end='')
           response = requests.get(url, auth=(user, authKey))
           responseStatus = response.json()["status"]
       print("DONE.\nRESULT is: " + responseStatus)
       if(responseStatus == "passed"):
          return 0
       else:
          return 1


def confirm(message):
    """
    Ask user to enter Y or N (case-insensitive).
    :return: True if the answer is Y.
    :rtype: bool
    """
    answer = ""
    while answer not in ["y", "n"]:
        answer = input(message + " [Y/N]? ").lower()
    return answer == "y"

if __name__ == "__main__":
    # Parse arguments
    parser = argparse.ArgumentParser(description='Interact with browserstack.')
    parser.add_argument("-t", "--test", help="Runs the espresso test.  Requires -a and -e", action="store_true")
    parser.add_argument("-a", "--apk", help="The app under test resulting from ./gradlew assemble")
    parser.add_argument("-e", "--espresso", help="The espresso test suite resulting from ./gradlew assembleDebugAndroidTest")
    parser.add_argument("--is-nightly", action="store_true")

    parser.add_argument("-u", "--upload", help="Upload a file to browserstack for app live testing")

    parser.add_argument("-l", "--list", help="List apps and test apps", action="store_true")

    parser.add_argument("-d", "--delete", help="Delete a test suite id.  Pass in the test suite id (no bs://)")
    parser.add_argument("-f", "--force", help="Force delete with no prompt", action="store_true")

    args = parser.parse_args()
    print("\n")

    if((not user or user == "") or (not authKey or authKey == "")):
        print("You must set the environment variables: ")
        print("   export BROWSERSTACK_USERNAME=<user>")
        print("   export BROWSERSTACK_ACCESS_KEY=<authkey>")
        sys.exit(1)

    elif(args.list):
       listApps()
       listEspressoApps()
       sys.exit(0)

    elif(args.delete != None):
       if(args.force or (confirm("Are you sure you want to delete the test suite: " + args.delete) == True)):
          deleteTestSuite(args.delete)
       sys.exit(0)

    elif(args.upload != None):
        appUrl = uploadAppLiveApk(args.upload)
        print("Uploaded app live apk url: " + appUrl)
        sys.exit(0)

    elif(args.test):
       if(args.espresso == None or args.apk == None):# or args.name == None):
           parser.print_help()
           sys.exit(2)
       else:
           print("Running the test with:\nApp under test: {apk}\nEspresso test suite: {testSuite}"
              .format(
                 apk=args.apk,
                 testSuite=args.espresso
              )
           )
           print("-----------------")
           appUrl = uploadApk(args.apk)
           print("-----------------")
           testUrl = uploadEspressoApk(args.espresso)
           print("-----------------")
           buildId = executeTests(appUrl, testUrl, args.is_nightly)
           exitStatus = 1
           if(buildId != None):
               exitStatus = waitForBuildComplete(buildId)
           else:
               deleteTestSuite(testUrl.replace("bs://", ""))
           sys.exit(exitStatus)
    else:
       parser.print_help()
