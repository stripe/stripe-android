#!/usr/bin/env python3
"""
Script to randomly select a subset of E2E tests for Browserstack execution.
This allows splitting tests between Browserstack (limited) and emulators (remaining).
"""

import os
import sys
import random
import json
import argparse
from pathlib import Path


def get_all_test_classes():
    """Get all test class names from the paymentsheet-example test directory."""
    test_directory = Path('paymentsheet-example/src/androidTest/java/')
    
    if not test_directory.exists():
        print(f"Error: Test directory {test_directory} does not exist")
        sys.exit(1)
    
    test_classes = []
    
    # Walk through the test directory and find all .kt files
    for kt_file in test_directory.rglob('*.kt'):
        # Skip non-test files (like README.md, utils, etc.)
        if any(skip in str(kt_file) for skip in ['README.md', 'utils/', 'test/core/']):
            continue
            
        # Convert file path to class name
        relative_path = kt_file.relative_to(test_directory)
        class_name = str(relative_path).replace('/', '.').replace('.kt', '')
        
        # Only include classes that look like test classes (contain 'Test' in name)
        if 'Test' in class_name:
            test_classes.append(class_name)
    
    return sorted(test_classes)


def select_random_tests(all_tests, count=15, seed=None):
    """
    Select a random subset of tests.
    
    Args:
        all_tests: List of all available test classes
        count: Number of tests to select (default: 15)
        seed: Random seed for reproducible selection (optional)
    
    Returns:
        Tuple of (selected_tests, remaining_tests)
    """
    if seed is not None:
        random.seed(seed)
    else:
        # Use current timestamp for maximum randomness
        import time
        random.seed(int(time.time()))
    
    if count >= len(all_tests):
        print(f"Warning: Requested {count} tests but only {len(all_tests)} available. Returning all tests.")
        return all_tests, []
    
    selected = random.sample(all_tests, count)
    remaining = [test for test in all_tests if test not in selected]
    
    return selected, remaining


def save_test_selection(selected_tests, remaining_tests, output_dir='test_selection'):
    """Save the test selection to files for use by CI/CD pipelines."""
    os.makedirs(output_dir, exist_ok=True)
    
    # Save selected tests for Browserstack
    with open(f'{output_dir}/browserstack_tests.json', 'w') as f:
        json.dump(selected_tests, f, indent=2)
    
    # Save remaining tests for emulators
    with open(f'{output_dir}/emulator_tests.json', 'w') as f:
        json.dump(remaining_tests, f, indent=2)
    
    # Save a summary file
    summary = {
        'total_tests': len(selected_tests) + len(remaining_tests),
        'browserstack_tests': len(selected_tests),
        'emulator_tests': len(remaining_tests),
        'selection_timestamp': str(Path().cwd()),
        'browserstack_test_classes': selected_tests,
        'emulator_test_classes': remaining_tests
    }
    
    with open(f'{output_dir}/test_selection_summary.json', 'w') as f:
        json.dump(summary, f, indent=2)
    
    return summary


def main():
    parser = argparse.ArgumentParser(description='Select random subset of E2E tests for Browserstack')
    parser.add_argument('--count', type=int, default=15, 
                       help='Number of tests to select for Browserstack (default: 15)')
    parser.add_argument('--seed', type=int, 
                       help='Random seed for reproducible selection')
    parser.add_argument('--output-dir', default='test_selection',
                       help='Output directory for test selection files (default: test_selection)')
    parser.add_argument('--list-all', action='store_true',
                       help='List all available test classes and exit')
    parser.add_argument('--verbose', action='store_true',
                       help='Enable verbose output')
    
    args = parser.parse_args()
    
    if args.verbose:
        print("Discovering all test classes...")
    
    all_tests = get_all_test_classes()
    
    if args.list_all:
        print(f"Found {len(all_tests)} test classes:")
        for i, test in enumerate(all_tests, 1):
            print(f"  {i:2d}. {test}")
        return
    
    if args.verbose:
        print(f"Found {len(all_tests)} total test classes")
    
    selected_tests, remaining_tests = select_random_tests(
        all_tests, 
        count=args.count, 
        seed=args.seed
    )
    
    if args.verbose:
        print(f"Selected {len(selected_tests)} tests for Browserstack:")
        for test in selected_tests:
            print(f"  - {test}")
        print(f"\nRemaining {len(remaining_tests)} tests for emulators:")
        for test in remaining_tests:
            print(f"  - {test}")
    
    summary = save_test_selection(selected_tests, remaining_tests, args.output_dir)
    
    print(f"Test selection complete!")
    print(f"  Browserstack tests: {summary['browserstack_tests']}")
    print(f"  Emulator tests: {summary['emulator_tests']}")
    print(f"  Total tests: {summary['total_tests']}")
    print(f"  Output directory: {args.output_dir}")


if __name__ == '__main__':
    main()
