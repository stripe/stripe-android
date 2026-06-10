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


def get_all_test_class_names():
    directory = 'paymentsheet-example/src/androidTest/java/'
    _, kotlin_file_names = run_fast_scandir(directory, [".kt"])
    test_file_names = test_files_from_kotlin_files(kotlin_file_names)
    return sorted(class_names_from_test_files(directory, test_file_names))


def get_shard_classes(shard_index, num_shards):
    all_classes = get_all_test_class_names()
    return [c for i, c in enumerate(all_classes) if i % num_shards == shard_index]


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Get test classes for a given shard.")
    parser.add_argument("--shard-index", type=int, required=True)
    parser.add_argument("--num-shards", type=int, required=True)
    args = parser.parse_args()

    classes = get_shard_classes(args.shard_index, args.num_shards)
    if not classes:
        print("No test classes found for shard", args.shard_index, file=sys.stderr)
        sys.exit(1)

    print(",".join(classes))
