#!/usr/bin/env python3
import argparse
import os
import sys


def run_fast_scandir(dir, ext):
    subfolders, files = [], []

    for f in os.scandir(dir):
        if f.is_dir():
            subfolders.append(f.path)
        if f.is_file():
            if os.path.splitext(f.name)[1].lower() in ext:
                files.append(f.path)

    for dir in list(subfolders):
        sf, f = run_fast_scandir(dir, ext)
        subfolders.extend(sf)
        files.extend(f)

    return subfolders, files


def test_files_from_kotlin_files(kotlin_files):
    test_files = []
    for file in kotlin_files:
        with open(file) as file_handle:
            if 'import org.junit.Test' in file_handle.read():
                test_files.append(file)
    return test_files


def class_names_from_test_files(test_directory, test_file_names):
    class_names = []
    for file_name in test_file_names:
        class_name = (file_name[len(test_directory):][:-3]).replace('/', '.')
        class_names.append(class_name)
    return class_names


# TestGooglePay runs on Firebase Test Lab (needs Google Play) via run-paymentsheet-google-pay-e2e.
FIREBASE_TEST_CLASSES = {
    'com.stripe.android.lpm.TestGooglePay',
}

# Browser-redirect tests need a real browser (Chrome) for the authorization step, so they run on the
# Chrome-bearing pixel2api33chrome device via the run-paymentsheet-browser-e2e Bitrise workflow. The
# lean aosp-atd image used for the main run ships no browser, so these would silently skip there.
BROWSER_REDIRECT_TEST_CLASSES = {
    'com.stripe.android.TestBrowsers',
    'com.stripe.android.lpm.TestAfterpay',
    'com.stripe.android.lpm.TestAlipay',
    'com.stripe.android.lpm.TestBancontact',
    'com.stripe.android.lpm.TestBillie',
    'com.stripe.android.lpm.TestCashApp',
    'com.stripe.android.lpm.TestEps',
    'com.stripe.android.lpm.TestFpx',
    'com.stripe.android.lpm.TestGrabPay',
    'com.stripe.android.lpm.TestIdeal',
    'com.stripe.android.lpm.TestMobilePay',
    'com.stripe.android.lpm.TestP24',
    'com.stripe.android.lpm.TestPayPal',
    'com.stripe.android.lpm.TestPayPay',
    'com.stripe.android.lpm.TestSatispay',
    'com.stripe.android.lpm.TestSunbit',
    'com.stripe.android.lpm.TestSwish',
    'com.stripe.android.lpm.TestTwint',
    'com.stripe.android.lpm.TestWero',
}

# Never run on the main (aosp-atd) sharded GMD run.
EXCLUDED_TEST_CLASSES = FIREBASE_TEST_CLASSES | BROWSER_REDIRECT_TEST_CLASSES


def get_all_test_class_names():
    directory = 'paymentsheet-example/src/androidTest/java/'
    _, kotlin_file_names = run_fast_scandir(directory, [".kt"])
    test_file_names = test_files_from_kotlin_files(kotlin_file_names)
    class_names = class_names_from_test_files(directory, test_file_names)
    return sorted(c for c in class_names if c not in EXCLUDED_TEST_CLASSES)


def get_shard_classes(shard_index, num_shards):
    all_classes = get_all_test_class_names()
    return [c for i, c in enumerate(all_classes) if i % num_shards == shard_index]


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Get test classes for a given shard.")
    parser.add_argument("--shard-index", type=int)
    parser.add_argument("--num-shards", type=int)
    parser.add_argument(
        "--browser-redirect",
        action="store_true",
        help="Print the browser-redirect test classes (run on the Chrome-bearing device).",
    )
    args = parser.parse_args()

    if args.browser_redirect:
        print(",".join(sorted(BROWSER_REDIRECT_TEST_CLASSES)))
        sys.exit(0)

    if args.shard_index is None or args.num_shards is None:
        parser.error("--shard-index and --num-shards are required unless --browser-redirect is set")

    classes = get_shard_classes(args.shard_index, args.num_shards)
    if not classes:
        print("No test classes found for shard", args.shard_index, file=sys.stderr)
        sys.exit(1)

    print(",".join(classes))
