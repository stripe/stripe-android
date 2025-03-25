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
import zipfile
from collections import defaultdict

# These need to be set in environment variables.
user = os.getenv("BROWSERSTACK_USERNAME")
authKey = os.getenv("BROWSERSTACK_ACCESS_KEY")

PROJECT_NAME = "Mobile Payments"


# https://www.browserstack.com/docs/app-automate/api-reference/espresso/apps#list-uploaded-apps
def listApps():
    # print step description
    print("LISTING apps...", end="")
    url = "https://api-cloud.browserstack.com/app-automate/espresso/v2/apps"
    response = requests.get(url, auth=(user, authKey))
    if response.status_code == 200:
        # print result
        print("DONE")
        print("Result:")
        print("| Uploaded at |    App id     | Expire at |")
        print("| ----------- | ------------- | --------- |")
        if 0 < len(response.json()["apps"]):
            for fileDescription in response.json()["apps"]:
                print(
                    "| {uploadDate} | {id} | {expireDate} |".format(
                        uploadDate=fileDescription["uploaded_at"],
                        id=fileDescription["app_id"],
                        expireDate=fileDescription["expiry"],
                    )
                )
            print("\n\n")
        else:
            print("NONE\n\n")
    else:
        print(
            "DONE\nRESULT: " + str(response.status_code) + "\n" + str(response.json())
        )


# https://www.browserstack.com/docs/app-automate/api-reference/espresso/tests#list-uploaded-test-suites
def listEspressoApps():
    # print step description
    print("LISTING test apps...", end="")
    url = "https://api-cloud.browserstack.com/app-automate/espresso/v2/test-suites"
    response = requests.get(url, auth=(user, authKey))
    if response.status_code == 200:
        # print result
        print("DONE")
        print("Result:")
        print("| Uploaded at | Test Suite id | Expire at |")
        print("| ----------- | ------------- | --------- |")
        if 0 < len(response.json()["test_suites"]):
            for fileDescription in response.json()["test_suites"]:
                print(
                    "| {uploadDate} | {id} | {expireDate} |".format(
                        uploadDate=fileDescription["uploaded_at"],
                        id=fileDescription["test_suite_id"],
                        expireDate=fileDescription["expiry"],
                    )
                )
            print("\n\n")
    else:
        print(
            "DONE\nRESULT: " + str(response.status_code) + "\n" + str(response.json())
        )


# https://www.browserstack.com/docs/app-automate/api-reference/espresso/tests#delete-a-test-suite
def deleteTestSuite(testSuiteID):
    # print step description
    print("DELETING test app: {id} ...".format(id=testSuiteID), end="")
    url = (
        "https://api-cloud.browserstack.com/app-automate/espresso/v2/test-suites/"
        + testSuiteID
    )
    response = requests.delete(url, auth=(user, authKey))
    if response.status_code == 200:
        # print result
        print("DONE\nResult: \n" + str(response.json()))
    else:
        print(
            "DONE\nRESULT: " + str(response.status_code) + "\n" + str(response.json())
        )


# https://www.browserstack.com/docs/app-automate/api-reference/espresso/apps#upload-an-app
def uploadApk(apkFile):
    # print step description
    print("UPLOADING the file: {file}...".format(file=apkFile), end="")
    url = "https://api-cloud.browserstack.com/app-automate/upload"
    files = {
        "file": ("paymentsheet-example.apk", open(apkFile, "rb")),
    }
    response = requests.post(url, files=files, auth=(user, authKey))
    if response.status_code == 200:
        appUrl = response.json()["app_url"]

        # print result
        print("DONE\nRESULT app url: " + appUrl)
        return appUrl
    else:
        print(
            "DONE\nRESULT: " + str(response.status_code) + "\n" + str(response.json())
        )
        return None


def uploadAppLiveApk(apkFile):
    # print step description
    print("UPLOADING the file: {file}...".format(file=apkFile), end="")
    url = "https://api-cloud.browserstack.com/app-live/upload"
    files = {
        "file": (os.path.basename(apkFile), open(apkFile, "rb")),
    }
    response = requests.post(url, files=files, auth=(user, authKey))
    if response.status_code == 200:
        appUrl = response.json()["app_url"]

        # print result
        print("DONE\nRESULT app url: " + appUrl)
        return appUrl
    else:
        print(
            "DONE\nRESULT: " + str(response.status_code) + "\n" + str(response.json())
        )
        return None


# https://www.browserstack.com/docs/app-automate/api-reference/espresso/tests#upload-a-test-suite
def uploadEspressoApk(espressoApkFile):
    # print step description
    print("UPLOADING the file: {file}...".format(file=espressoApkFile), end="")

    url = "https://api-cloud.browserstack.com/app-automate/espresso/test-suite"
    files = {
        "file": ("paymentSheet-espresso.apk", open(espressoApkFile, "rb")),
    }
    response = requests.post(url, files=files, auth=(user, authKey))

    if response.status_code == 200:
        testUrl = response.json()["test_url"]

        # print result
        print("DONE\nRESULT test url: " + testUrl)
        return testUrl
    else:
        print(
            "DONE\nRESULT: " + str(response.status_code) + "\n" + str(response.json())
        )
        return None


# https://stackoverflow.com/a/59803793
def runFastScandir(dir, ext):
    subfolders, files = [], []

    for f in os.scandir(dir):
        if f.is_dir():
            subfolders.append(f.path)
        if f.is_file():
            if os.path.splitext(f.name)[1].lower() in ext:
                files.append(f.path)

    for dir in list(subfolders):
        sf, f = runFastScandir(dir, ext)
        subfolders.extend(sf)
        files.extend(f)

    return subfolders, files


def testFilesFromKotlinFiles(kotlinFiles):
    testFiles = []

    for file in kotlinFiles:
        with open(file) as file_handle:
            if 'import org.junit.Test' in file_handle.read():
                testFiles.append(file)
                continue

    return testFiles


def classNamesFromTestFiles(testDirectory, testFileNames):
    classNames = []

    for fileName in testFileNames:
        # Removes the prefix of the test directory, then removes the suffix of the '.kt',
        #   then replaces the directory markers ('/', with their package representation.
        # Transforms something like:
        #   paymentsheet-example/src/androidTest/java/com/stripe/android/TestCustomers.kt
        #   to com.stripe.android.TestCustomers
        className = (fileName[len(testDirectory):][:-3]).replace('/', '.')
        classNames.append(className)

    return classNames


def getAllTestClassNames():
    directory = 'paymentsheet-example/src/androidTest/java/'
    _, kotlinFileNames = runFastScandir(directory, [".kt"])
    testFileNames = testFilesFromKotlinFiles(kotlinFileNames)
    return classNamesFromTestFiles(directory, testFileNames)


# https://www.browserstack.com/docs/app-automate/api-reference/espresso/builds#execute-a-build
def executeTestsWithAddedParams(appUrl, testUrl, devices, addedParams):
    baseParams = {
        "app": appUrl,
        "devices": devices,
        "testSuite": testUrl,
        "networkLogs": True,
        "deviceLogs": True,
        "video": True,
        "acceptInsecureCerts": True,
        "locale": "en_US",
        "enableSpoonFramework": False,
        "project": PROJECT_NAME,
    }
    json = {**baseParams, **addedParams}
    print(
        "RUNNING the tests (appUrl: {app}, testUrl: {test})...".format(
            app=appUrl, test=testUrl
        ),
        end="",
    )
    url = "https://api-cloud.browserstack.com/app-automate/espresso/v2/build"
    response = requests.post(
        url,
        json=json,
        auth=(user, authKey),
    )
    jsonResponse = response.json()

    if response.status_code == 200:
        # print result
        print("DONE\nRESULT build Started: " + jsonResponse["message"])
        if jsonResponse["message"] == "Success":
            print("RESULT build id: " + jsonResponse["build_id"])
            print(
                "RESULT see build here: "
                + "https://app-automate.browserstack.com/dashboard/v2/builds/{buildId}".format(
                    buildId=jsonResponse["build_id"]
                )
            )
            return jsonResponse["build_id"]
        else:
            return None
    else:
        print(
            "DONE\nRESULT: " + str(response.status_code) + "\n" + str(response.json())
        )
        return None

def executeTests(appUrl, testUrl, isNightly):
    devices = []
    if isNightly:
        devices = [
            "Google Pixel 8-14.0",
            "Google Pixel 7-13.0",
            "Samsung Galaxy S22-12.0",
            "Google Pixel 5-11.0",
            "Google Pixel 4 XL-10.0",
            "Google Pixel 3-9.0",
            "Samsung Galaxy S9-8.0",
            "Samsung Galaxy S8-7.0",
        ]
    else:
        devices = [
            "Samsung Galaxy S22-12.0",
        ]

    # We only have 25 parallel runs, and we want multiple PRs to run at the same time.
    numberOfShards = 2.0 if isNightly else 10.0

    addedParams = {
        "shards": {
            "numberOfShards": numberOfShards,
        },
    }
    return executeTestsWithAddedParams(appUrl, testUrl, devices, addedParams)

def executeTestsForFailure(appUrl, testUrl, device, testClasses):
    addedParams = {
        "class": testClasses,
    }
    return executeTestsWithAddedParams(appUrl, testUrl, [device], addedParams)

# https://www.browserstack.com/docs/app-automate/api-reference/espresso/builds#get-build-status
def get_build_status(buildId):
    url = (
        "https://api-cloud.browserstack.com/app-automate/espresso/v2/builds/" + buildId
    )
    return requests.get(url, auth=(user, authKey))


def zipFiles(files):
    def testReportName(idx):
        return f"test-reports/test-report-{idx}.xml"

    zip = zipfile.ZipFile("test-reports/test-reports.zip", "w")
    for idx in range(len(files)):
        f = open(testReportName(idx), "w")
        f.write(files[idx])
        f.close()
        zip.write(testReportName(idx))
    zip.close()
    return zip


# https://www.browserstack.com/docs/app-automate/espresso/view-test-reports#junit-xml-report
def getJunitXmlReport(buildId, sessionId):
    url = f"https://api-cloud.browserstack.com/app-automate/espresso/v2/builds/{buildId}/sessions/{sessionId}/report"
    return requests.get(url, auth=(user, authKey)).text


def getJunitXmlReports(buildId):
    reports = []
    sessionIds = getSessionIdsForBuild(buildId)
    for sessionId in sessionIds:
        reports.append(getJunitXmlReport(buildId, sessionId))

    return reports


# https://www.browserstack.com/docs/test-observability/quick-start/junit-reports#integrate-with-test-observability-using-junit-reports
def uploadTestReportsToObservability():
    files = {"data": ("test-reports.zip", open("test-reports/test-reports.zip", "rb"))}
    url = "https://upload-observability.browserstack.com/upload"
    response = requests.post(
        url,
        data={
            "projectName": PROJECT_NAME,
            "buildName": "Android SDK",
        },
        files=files,
        auth=(user, authKey),
    )
    observabilityUrl = response.json()["message"].split(" ")[-1]
    print(f"View observability results for this build: {observabilityUrl}")


def deleteObservabilityFiles():
    testReportFiles = os.listdir("test-reports")
    for testReportFile in testReportFiles:
        os.remove(f"test-reports/{testReportFile}")
    os.removedirs("test-reports")


def updateObservabilityWithResults(buildId):
    os.makedirs("test-reports")
    reportsForBuildId = getJunitXmlReports(buildId)
    zipFiles(reportsForBuildId)
    uploadTestReportsToObservability()
    deleteObservabilityFiles()


def waitForBuildComplete(buildId):
    print("WAITING for build id: {buildId}...".format(buildId=buildId), end="")
    responseStatus = "running"

    while responseStatus == "running":
        time.sleep(10)
        print(".", end="")
        try:
            response = get_build_status(buildId)
            responseStatus = response.json()["status"]
        except:
            print("Failed to get build status, trying again.")
    print("DONE.\nRESULT is: " + responseStatus)
    if responseStatus == "passed":
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


def runTests(appUrl, testUrl, isNightly):
    print("RUNNING all test cases")
    buildId = executeTests(appUrl, testUrl, isNightly)
    exitStatus = 1
    if buildId != None:
        exitStatus = waitForBuildComplete(buildId)
    else:
        deleteTestSuite(testUrl.replace("bs://", ""))
    return {"exitStatus": exitStatus, "buildId": buildId}

def runTestsForFailure(appUrl, testUrl, device, testClasses):
    print(f"RUNNING {str(len(testClasses))} test cases on {device}")
    buildId = executeTestsForFailure(appUrl, testUrl, device, testClasses)
    exitStatus = 1
    if buildId != None:
        exitStatus = waitForBuildComplete(buildId)
    else:
        deleteTestSuite(testUrl.replace("bs://", ""))
    return {"exitStatus": exitStatus, "buildId": buildId}

# https://www.browserstack.com/docs/app-automate/api-reference/espresso/sessions#get-session-details
def getFailedTestClassesForSession(buildId, sessionId):
    failedTestClasses = []
    url = (
        "https://api-cloud.browserstack.com/app-automate/espresso/v2/builds/"
        + buildId
        + "/sessions/"
        + sessionId
    )
    details = requests.get(url, auth=(user, authKey)).json()
    sessionFailed = details["status"] == "failed"

    if not sessionFailed:
        return []

    for classData in details["testcases"]["data"]:
        testCaseStatuses = [testCase["status"] for testCase in classData["testcases"]]
        if "failed" in testCaseStatuses:
            failedTestClasses.append(classData["class"])
    return failedTestClasses


def classNameToFullyQualifiedClassName(failedTestClassName):
    fullyQualifiedTestClassNames = getAllTestClassNames()
    for fullyQualifiedTestClassName in fullyQualifiedTestClassNames:
        testClassName = fullyQualifiedTestClassName.split(".")[-1]
        if testClassName == failedTestClassName:
            return fullyQualifiedTestClassName
    return None


def getSessionIdsForBuild(buildId):
    sessionIds = []
    buildStatus = get_build_status(buildId)
    devices = buildStatus.json()["devices"]
    for device in devices:
        sessions_on_device = device["sessions"]
        for session in sessions_on_device:
            sessionIds.append(session["id"])
    return sessionIds

def getSessionIdsAndDeviceForBuild(buildId):
    sessionIds = []
    buildStatus = get_build_status(buildId)
    devices = buildStatus.json()["devices"]
    for device in devices:
        sessions_on_device = device["sessions"]
        for session in sessions_on_device:
            deviceIdentifier = f"{device['device']}-{device['os_version']}"
            sessionIds.append({"session_id": session["id"], "device": deviceIdentifier})
    return sessionIds


def getFailedTestsForBuild(buildId):
    print(f"Getting failed tests for build {buildId}")
    sessionIdsAndDevices = getSessionIdsAndDeviceForBuild(buildId)
    devicesWithFailedClasses = defaultdict(list)
    for item in sessionIdsAndDevices:
        failedClassesInSession = getFailedTestClassesForSession(buildId, item["session_id"])
        for failedClass in failedClassesInSession:
            devicesWithFailedClasses[item["device"]].append(classNameToFullyQualifiedClassName(failedClass))
            print(f"Device failed: {item['device']} - {failedClass}")
    return devicesWithFailedClasses


def retryFailedTests(buildId, numRetries):
    failedTestsDictionary = getFailedTestsForBuild(buildId)
    while numRetries > 0:
        updatedFailedTestsDictionary = {}
        numRetries -= 1
        for failedDevice in failedTestsDictionary:
            deviceTestResults = runTestsForFailure(appUrl, testUrl, failedDevice, failedTestsDictionary[failedDevice])
            deviceExitStatus = deviceTestResults["exitStatus"]
            if deviceExitStatus != 0:
                updatedFailedTestsDictionary[failedDevice] = getFailedTestsForBuild(deviceTestResults["buildId"])[failedDevice]

        if len(updatedFailedTestsDictionary) == 0:
            return 0
        failedTestsDictionary = updatedFailedTestsDictionary
    return -1


if __name__ == "__main__":
    # Parse arguments
    parser = argparse.ArgumentParser(description="Interact with browserstack.")
    parser.add_argument(
        "-t",
        "--test",
        help="Runs the espresso test.  Requires -a and -e",
        action="store_true",
    )
    parser.add_argument(
        "-a", "--apk", help="The app under test resulting from ./gradlew assemble"
    )
    parser.add_argument(
        "-e",
        "--espresso",
        help="The espresso test suite resulting from ./gradlew assembleDebugAndroidTest",
    )
    parser.add_argument("--is-nightly", action="store_true")

    parser.add_argument(
        "-u", "--upload", help="Upload a file to browserstack for app live testing"
    )

    parser.add_argument(
        "-l", "--list", help="List apps and test apps", action="store_true"
    )

    parser.add_argument(
        "-d",
        "--delete",
        help="Delete a test suite id.  Pass in the test suite id (no bs://)",
    )
    parser.add_argument(
        "-f", "--force", help="Force delete with no prompt", action="store_true"
    )
    parser.add_argument("-n", "--num-retries", help="Retry failed tests")

    args = parser.parse_args()
    print("\n")

    if (not user or user == "") or (not authKey or authKey == ""):
        print("You must set the environment variables: ")
        print("   export BROWSERSTACK_USERNAME=<user>")
        print("   export BROWSERSTACK_ACCESS_KEY=<authkey>")
        sys.exit(1)

    elif args.list:
        listApps()
        listEspressoApps()
        sys.exit(0)

    elif args.delete != None:
        if args.force or (
            confirm("Are you sure you want to delete the test suite: " + args.delete)
            == True
        ):
            deleteTestSuite(args.delete)
        sys.exit(0)

    elif args.upload != None:
        appUrl = uploadAppLiveApk(args.upload)
        print("Uploaded app live apk url: " + appUrl)
        sys.exit(0)

    elif args.test:
        if args.espresso == None or args.apk == None:  # or args.name == None):
            parser.print_help()
            sys.exit(2)
        else:
            print(
                "Running the test with:\nApp under test: {apk}\nEspresso test suite: {testSuite}".format(
                    apk=args.apk, testSuite=args.espresso
                )
            )
            print("-----------------")
            appUrl = uploadApk(args.apk)
            print("-----------------")
            testUrl = uploadEspressoApk(args.espresso)
            print("-----------------")
            numRetries = int(args.num_retries) if args.num_retries is not None else 0

            exitStatus = 1
            testResults = runTests(appUrl, testUrl, args.is_nightly)
            print("-----------------")
            exitStatus = testResults["exitStatus"]
            updateObservabilityWithResults(testResults["buildId"])
            if exitStatus != 0 and numRetries > 0:
                os.environ["BROWSERSTACK_RERUN"] = "true"
                exitStatus = retryFailedTests(testResults["buildId"], numRetries)

            os.environ["BROWSERSTACK_RERUN"] = "false"
            sys.exit(exitStatus)
    else:
        parser.print_help()
