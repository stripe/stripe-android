#!/usr/bin/env ruby
# frozen_string_literal: true

require 'optparse'
require 'open3'

PROJECT_ROOT = File.expand_path('..', __dir__)
LOGCAT_TAG = 'MPELatencyBenchmark'
BENCHMARK_CLASS = 'com.stripe.android.lpm.MPELatencyTest'

# ============================================================================
# DATA COLLECTION
# ============================================================================

def merge_results(target, source)
  target.merge!(source) { |_, old, new| old + new }
end

def run_latency_tests_for_commit(commit, sample_count)
  puts "\n" + "=" * 80
  puts "Testing commit: #{commit}"
  puts "=" * 80

  checkout_commit(commit)

  puts "Collecting #{sample_count} sample(s) in one Gradle invocation..."

  output = run_android_latency_tests(sample_count)
  results = parse_latency_results(output)
  puts "\nFound #{results.length} test(s) with results"
  results
end

def run_android_latency_tests(sample_count)
  clear_logcat

  command = [
    './gradlew',
    ':paymentsheet-example:connectedBaseDebugAndroidTest',
    "-Pandroid.testInstrumentationRunnerArguments.class=#{BENCHMARK_CLASS}",
    '-Pandroid.testInstrumentationRunnerArguments.mpe_benchmark_enabled=true',
    "-Pandroid.testInstrumentationRunnerArguments.mpe_latency_samples=#{sample_count}"
  ]

  output = +''

  Dir.chdir(PROJECT_ROOT) do
    Open3.popen2e(*command) do |stdin, stdout_stderr, wait_thread|
      stdin.close

      stdout_stderr.each_line do |line|
        print line
        output << line
      end

      exit_status = wait_thread.value
      unless exit_status.success?
        puts "\nWarning: Gradle command exited with status #{exit_status.exitstatus}"
        puts 'Continuing to parse available logcat output...'
      end
    end
  end

  output + "\n" + dump_logcat
end

def clear_logcat
  stdout, status = Open3.capture2e('adb', 'logcat', '-c', chdir: PROJECT_ROOT)
  return if status.success?

  warn stdout
  raise 'Failed to clear adb logcat. Ensure adb is installed and a device/emulator is available.'
end

def dump_logcat
  stdout, status = Open3.capture2e(
    'adb',
    'logcat',
    '-d',
    '-s',
    "#{LOGCAT_TAG}:I",
    '*:S',
    chdir: PROJECT_ROOT
  )

  return stdout if status.success?

  warn stdout
  raise 'Failed to read adb logcat. Ensure adb is installed and a device/emulator is available.'
end

def parse_latency_results(output)
  results = Hash.new { |h, k| h[k] = [] }

  output.each_line do |line|
    next unless line =~ /SYNTHETIC_LATENCY_RESULT:\s*(.+?):\s*([\d.]+)/

    test_name = Regexp.last_match(1).strip
    duration_ms = Regexp.last_match(2).to_f
    duration_seconds = duration_ms / 1000.0
    results[test_name] << duration_seconds
    puts "  Found result: #{test_name} = #{format('%.4f', duration_seconds)}s"
  end

  results
end

# ============================================================================
# STATISTICS
# ============================================================================

T_CRITICAL_VALUES = {
  1 => 12.706, 2 => 4.303, 3 => 3.182, 4 => 2.776, 5 => 2.571,
  6 => 2.447, 7 => 2.365, 8 => 2.306, 9 => 2.262, 10 => 2.228,
  11 => 2.201, 12 => 2.179, 13 => 2.160, 14 => 2.145, 15 => 2.131,
  16 => 2.120, 17 => 2.110, 18 => 2.101,
  19 => 2.093, 20 => 2.086, 21 => 2.080, 22 => 2.074, 23 => 2.069,
  24 => 2.064, 25 => 2.060, 26 => 2.056, 27 => 2.052, 28 => 2.048,
  29 => 2.045, 30 => 2.042,
  32 => 2.037, 34 => 2.032, 36 => 2.028, 38 => 2.024, 40 => 2.021,
  42 => 2.018, 44 => 2.015, 46 => 2.013, 48 => 2.011, 50 => 2.009,
  55 => 2.004, 60 => 2.000, 65 => 1.997, 70 => 1.994, 75 => 1.992,
  80 => 1.990, 85 => 1.989, 90 => 1.987, 95 => 1.985, 98 => 1.984
}.freeze

T_KEYS = T_CRITICAL_VALUES.keys.sort.freeze
MIN_KEY = T_KEYS.first
MAX_KEY = T_KEYS.last

def get_t_critical(df)
  df = df.to_f
  return T_CRITICAL_VALUES[MIN_KEY] if df <= MIN_KEY
  return T_CRITICAL_VALUES[MAX_KEY] if df >= MAX_KEY

  lower = T_KEYS.reverse_each.find { |key| key <= df }
  T_CRITICAL_VALUES[lower]
end

def mean(array)
  array.sum / array.length.to_f
end

def standard_deviation(array, mean_val)
  variance = array.map { |value| (value - mean_val)**2 }.sum / (array.length - 1).to_f
  Math.sqrt(variance)
end

def compute_independent_statistics(base_values, new_values)
  base_mean = mean(base_values)
  new_mean = mean(new_values)
  base_sd = standard_deviation(base_values, base_mean)
  new_sd = standard_deviation(new_values, new_mean)
  n1 = base_values.length
  n2 = new_values.length

  abs_mean = new_mean - base_mean
  abs_se = Math.sqrt((base_sd**2 / n1) + (new_sd**2 / n2))

  numerator = (base_sd**2 / n1 + new_sd**2 / n2)**2
  denominator = ((base_sd**2 / n1)**2 / (n1 - 1)) + ((new_sd**2 / n2)**2 / (n2 - 1))
  df = numerator / denominator
  tcrit = get_t_critical(df)

  abs_margin = tcrit * abs_se
  abs_lo = abs_mean - abs_margin
  abs_hi = abs_mean + abs_margin

  pct_mean = abs_mean / base_mean * 100.0
  pct_lo = abs_lo / base_mean * 100.0
  pct_hi = abs_hi / base_mean * 100.0
  pct_margin = (pct_hi - pct_mean).abs

  {
    abs_mean: abs_mean,
    abs_margin: abs_margin,
    pct_mean: pct_mean,
    pct_margin: pct_margin,
    significant: abs_mean.abs > abs_margin
  }
end

def compute_statistical_report(all_results)
  base_tests = all_results[:base]
  new_tests = all_results[:new]

  raise 'No test results found for one or both commits' if base_tests.empty? || new_tests.empty?

  common_tests = base_tests.keys & new_tests.keys
  raise 'No common tests found between commits' if common_tests.empty?

  common_tests.each_with_object({}) do |test_name, test_stats|
    test_stats[test_name] = compute_independent_statistics(
      base_tests.fetch(test_name),
      new_tests.fetch(test_name)
    )
  end
end

# ============================================================================
# PRINTING
# ============================================================================

def print_raw_measurements(all_results, base_commit, new_commit)
  puts "\n" + "=" * 80
  puts 'RAW LATENCY MEASUREMENTS'
  puts '=' * 80

  puts "\nBase Commit: #{base_commit}"
  puts '-' * 80
  all_results[:base].each do |test_name, durations|
    avg = durations.sum / durations.length
    puts "  #{test_name}:"
    puts "    Runs: #{durations.map { |duration| format('%.4fs', duration) }.join(', ')}"
    puts "    Average: #{format('%.4fs', avg)}"
  end

  puts "\nNew Commit: #{new_commit}"
  puts '-' * 80
  all_results[:new].each do |test_name, durations|
    avg = durations.sum / durations.length
    puts "  #{test_name}:"
    puts "    Runs: #{durations.map { |duration| format('%.4fs', duration) }.join(', ')}"
    puts "    Average: #{format('%.4fs', avg)}"
  end
end

def print_statistical_report(test_stats, all_results)
  puts "\n" + "=" * 80
  puts 'LATENCY DELTA REPORT (vs Base Commit)'
  puts '=' * 80

  max_test_name_length = [test_stats.keys.map(&:length).max, 40].max
  base_width = 13
  new_width = 12
  delta_width = 45
  significance_width = 20
  separators = 12
  total_width = max_test_name_length + base_width + new_width + delta_width + significance_width + separators

  puts
  puts format(
    "%-#{max_test_name_length}s | %#{base_width}s | %#{new_width}s | %-#{delta_width}s | %s",
    'Test',
    'Base commit',
    'New commit',
    'Mean Delta (95% CI)',
    'Significant Difference?'
  )
  puts '-' * total_width

  test_stats.sort.each do |test_name, stats|
    base_avg = mean(all_results[:base].fetch(test_name)) * 1000
    new_avg = mean(all_results[:new].fetch(test_name)) * 1000

    pct_str = format('Δ %+.1f%% ± %.1f%%', stats[:pct_mean], stats[:pct_margin])
    abs_str = format('%+.0fms ± %.0fms', stats[:abs_mean] * 1000, stats[:abs_margin] * 1000)
    delta_str = "#{pct_str}; #{abs_str}"
    significance_str = stats[:significant] ? 'Yes (p < 0.05)' : 'No'

    puts format(
      "%-#{max_test_name_length}s | %#{base_width}s | %#{new_width}s | %-#{delta_width}s | %s",
      test_name,
      format('%.0fms', base_avg),
      format('%.0fms', new_avg),
      delta_str,
      significance_str
    )
  end

  puts
end

# ============================================================================
# GIT / PRECONDITIONS
# ============================================================================

def inside_clean_worktree?
  Dir.chdir(PROJECT_ROOT) do
    stdout, status = Open3.capture2e('git', 'status', '--porcelain')
    raise stdout unless status.success?

    stdout.strip.empty?
  end
end

def checkout_commit(commit)
  Dir.chdir(PROJECT_ROOT) do
    success = system('git', 'checkout', commit)
    raise "Failed to checkout commit #{commit}" unless success
  end
end

def current_checkout
  Dir.chdir(PROJECT_ROOT) do
    branch, branch_status = Open3.capture2e('git', 'rev-parse', '--abbrev-ref', 'HEAD')
    raise branch unless branch_status.success?

    commit, commit_status = Open3.capture2e('git', 'rev-parse', 'HEAD')
    raise commit unless commit_status.success?

    branch = branch.strip
    commit = commit.strip

    branch == 'HEAD' ? commit : branch
  end
end

def restore_checkout(target)
  puts "\nRestoring original checkout: #{target}"
  Dir.chdir(PROJECT_ROOT) do
    system('git', 'checkout', target)
  end
end

# ============================================================================
# TEST MODE
# ============================================================================

def run_statistics_test
  puts '=' * 80
  puts 'STATISTICS TEST MODE'
  puts '=' * 80

  base = [1.0, 1.1, 1.2, 1.1, 1.0] * 2
  new = [1.1, 1.2, 1.3, 1.2, 1.1] * 2
  stats = compute_independent_statistics(base, new)

  puts "\nTest data:"
  puts "  Base mean: #{mean(base).round(3)}"
  puts "  New mean: #{mean(new).round(3)}"
  puts "\nComputed statistics:"
  puts "  Percentage delta: #{stats[:pct_mean].round(1)}% ± #{stats[:pct_margin].round(1)}%"
  puts "  Absolute delta: #{(stats[:abs_mean] * 1000).round(0)}ms ± #{(stats[:abs_margin] * 1000).round(0)}ms"
  puts "  Statistically significant: #{stats[:significant]}"
  puts '=' * 80
end

# ============================================================================
# MAIN
# ============================================================================

options = {
  iterations: 20
}

OptionParser.new do |opts|
  opts.banner = <<~BANNER
    Usage: measure_latency_difference.rb [options]

    Measures and compares Android PaymentSheet latency between two git commits.

    This script:
    1. Checks out each commit and runs MPE latency tests in ABBA order
    2. Parses SYNTHETIC_LATENCY_RESULT output from adb logcat
    3. Performs Welch t-test analysis to compare latency differences
    4. Reports mean delta with 95% confidence intervals and statistical significance

    Examples:
      ./scripts/measure_latency_difference.rb --base-commit abc123 --commit def456
      ./scripts/measure_latency_difference.rb --base-commit abc123 --commit abc123

    Options:
  BANNER

  opts.on('--base-commit COMMIT', 'Base commit to compare against (required)') do |commit|
    options[:base_commit] = commit
  end

  opts.on('--commit COMMIT', 'New commit to compare (required)') do |commit|
    options[:commit] = commit
  end

  opts.on('--iterations N', Integer, 'Number of total samples to collect per commit (default: 20)') do |value|
    options[:iterations] = value
  end

  opts.on('-h', '--help', 'Prints this help') do
    puts opts
    exit
  end
end.parse!

if ENV['TEST_STATS'] == '1'
  run_statistics_test
  exit
end

if options[:base_commit].nil? || options[:commit].nil?
  warn 'Error: --base-commit and --commit are required'
  exit 1
end

if options[:iterations] < 10
  warn "Error: Iterations must be at least 10 (got #{options[:iterations]})"
  exit 1
end

unless inside_clean_worktree?
  warn 'Error: stripe-android has uncommitted changes. Commit or stash them before running this script.'
  exit 1
end

original_checkout = current_checkout
puts 'Configuration:'
puts "  Base commit: #{options[:base_commit]}"
puts "  New commit:  #{options[:commit]}"
puts "  Iterations:  #{options[:iterations]}"
puts "  Original checkout: #{original_checkout}"

all_results = { base: {}, new: {} }
run_size = options[:iterations] / 2

unless options[:iterations].even?
  warn 'Error: Iterations must be even so ABBA ordering produces balanced samples'
  exit 1
end

puts "\nUsing ABBA ordering (2 runs per commit, #{run_size} samples per run = #{options[:iterations]} total samples per commit)"

begin
  merge_results(all_results[:base], run_latency_tests_for_commit(options[:base_commit], run_size))
  merge_results(all_results[:new], run_latency_tests_for_commit(options[:commit], run_size))
  merge_results(all_results[:new], run_latency_tests_for_commit(options[:commit], run_size))
  merge_results(all_results[:base], run_latency_tests_for_commit(options[:base_commit], run_size))
ensure
  restore_checkout(original_checkout)
end

test_stats = compute_statistical_report(all_results)
print_raw_measurements(all_results, options[:base_commit], options[:commit])
print_statistical_report(test_stats, all_results)
