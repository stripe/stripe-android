# frozen_string_literal: true

require 'open3'

module LatencyTestUtils
  PROJECT_ROOT = File.expand_path('..', __dir__)
  LOGCAT_TAG = 'StripeSdk'
  LATENCY_TEST_CLASS = 'com.stripe.android.latency.TestLatency'
  LATENCY_TEST_GRADLE_TASK = ':paymentsheet-example:connectedBaseDebugAndroidTest'
  DURATION_KEY = 'Loading'
  MAX_INVOCATION_ATTEMPTS = 3
  LOGCAT_BUFFER_SIZE = ENV.fetch('LOGCAT_BUFFER_SIZE', '16M')

  module_function

  def run_latency_tests(
    sample_count,
    failure_context: nil,
    gradle_task: LATENCY_TEST_GRADLE_TASK,
    latency_test_class: LATENCY_TEST_CLASS,
    duration_key: DURATION_KEY
  )
    (1..MAX_INVOCATION_ATTEMPTS).each do |attempt|
      puts "Collecting #{sample_count} sample(s) in one Gradle invocation..."
      puts "Attempt #{attempt}/#{MAX_INVOCATION_ATTEMPTS}"

      output = run_android_latency_tests(
        sample_count,
        gradle_task: gradle_task,
        latency_test_class: latency_test_class
      )
      print_raw_result_debug_summary(output)
      results = parse_latency_results(output, duration_key: duration_key)
      expected_test_count = extract_expected_test_count(output)

      if valid_invocation_results?(
        results: results,
        sample_count: sample_count,
        expected_test_count: expected_test_count
      )
        puts "\nFound #{results.length} test(s) with results"
        return results
      end

      puts "\nInvocation did not produce a complete sample set."
      puts "  Expected tests: #{expected_test_count || 'unknown'}"
      puts "  Actual tests:   #{results.length}"
      if results.any?
        puts '  Samples per test:'
        results.sort.each do |test_name, durations|
          puts "    #{test_name}: #{durations.length}"
        end
      end
    end

    suffix = failure_context ? " #{failure_context}" : ''
    raise "Failed to collect a complete sample set#{suffix} after #{MAX_INVOCATION_ATTEMPTS} attempts"
  end

  def run_android_latency_tests(
    sample_count,
    gradle_task: LATENCY_TEST_GRADLE_TASK,
    latency_test_class: LATENCY_TEST_CLASS
  )
    clear_logcat

    command = [
      './gradlew',
      '--no-configuration-cache',
      gradle_task,
      "-Pandroid.testInstrumentationRunnerArguments.class=#{latency_test_class}",
      "-PLATENCY_EXPERIMENT_ITERATIONS=#{sample_count}"
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
    resize_logcat_buffer

    stdout, status = Open3.capture2e('adb', 'logcat', '-c', chdir: PROJECT_ROOT)
    return if status.success?

    warn stdout
    raise 'Failed to clear adb logcat. Ensure adb is installed and a device/emulator is available.'
  end

  def resize_logcat_buffer
    stdout, status = Open3.capture2e('adb', 'logcat', '-G', LOGCAT_BUFFER_SIZE, chdir: PROJECT_ROOT)
    return if status.success?

    warn stdout
    raise "Failed to resize adb logcat buffer to #{LOGCAT_BUFFER_SIZE}."
  end

  def dump_logcat
    stdout, status = Open3.capture2e(
      'adb',
      'logcat',
      '-d',
      '-s',
      "#{LOGCAT_TAG}:D",
      '*:S',
      chdir: PROJECT_ROOT
    )

    return stdout if status.success?

    warn stdout
    raise 'Failed to read adb logcat. Ensure adb is installed and a device/emulator is available.'
  end

  def parse_latency_results(output, duration_key: DURATION_KEY)
    results = Hash.new { |hash, key| hash[key] = [] }
    current_test_name = nil
    start_time = nil

    output.each_line do |line|
      if line =~ /LATENCY_TEST_CASE_STARTED:\s*(.+?)\s*$/
        current_test_name = Regexp.last_match(1).strip
        start_time = nil
        next
      end

      if line =~ /LATENCY_TEST_CASE_FINISHED:\s*(.+?)\s*$/
        finished_test_name = Regexp.last_match(1).strip
        current_test_name = nil if finished_test_name == current_test_name
        start_time = nil
        next
      end

      if line =~ /DURATION_STARTED:\s*#{Regexp.escape(duration_key)}:\s*(\d+)/
        start_time = Regexp.last_match(1).to_i
        next
      end

      next unless line =~ /DURATION_ENDED:\s*#{Regexp.escape(duration_key)}:\s*(\d+)/
      next if current_test_name.nil? || start_time.nil?

      end_time = Regexp.last_match(1).to_i
      duration_seconds = (end_time - start_time) / 1000.0
      results[current_test_name] << duration_seconds
      puts "  Found result: #{current_test_name} = #{format('%.4f', duration_seconds)}s"
      start_time = nil
    end

    results
  end

  def print_raw_result_debug_summary(output)
    counts = Hash.new(0)

    output.each_line do |line|
      next unless line =~ /LATENCY_TEST_CASE_STARTED:\s*(.+?)\s*$/

      test_name = Regexp.last_match(1).strip
      counts[test_name] += 1
    end

    puts "\nRaw test start counts for this invocation:"
    if counts.empty?
      puts '  No LATENCY_TEST_CASE_STARTED lines found'
      return
    end

    counts.sort.each do |test_name, count|
      puts "  #{test_name}: #{count}"
    end
  end

  def extract_expected_test_count(output)
    match = output.match(/Starting (\d+) tests on/)
    # Subtract 1 to ignore the warm up test case
    (match && match[1].to_i) - 1
  end

  def valid_invocation_results?(results:, sample_count:, expected_test_count:)
    return false if expected_test_count.nil?
    return false unless results.length == expected_test_count

    results.values.all? { |durations| durations.length == sample_count }
  end
end
