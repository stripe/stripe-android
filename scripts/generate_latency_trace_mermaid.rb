#!/usr/bin/env ruby
# frozen_string_literal: true

require 'optparse'
require 'open3'

PROJECT_ROOT = File.expand_path('..', __dir__)
LOGCAT_TAG = 'StripeSdk'
LATENCY_TEST_CLASS = 'com.stripe.android.latency.TestLatency'
PRIMARY_DURATION_KEY = 'Loading'
TRACE_DURATION_KEY_PREFIX = 'PaymentSheetLoad'
LOGCAT_BUFFER_SIZE = ENV.fetch('LOGCAT_BUFFER_SIZE', '16M')

TraceSpan = Struct.new(:name, :start_offset_ms, :duration_ms, keyword_init: true)
TraceSession = Struct.new(:test_name, :total_duration_ms, :spans, keyword_init: true)

def inside_clean_worktree?
  Dir.chdir(PROJECT_ROOT) do
    stdout, status = Open3.capture2e('git', '-c', 'core.fsmonitor=false', 'status', '--porcelain')
    raise stdout unless status.success?

    stdout.strip.empty?
  end
end

def checkout_commit(commit)
  Dir.chdir(PROJECT_ROOT) do
    success = system('git', '-c', 'core.fsmonitor=false', 'checkout', commit)
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
    system('git', '-c', 'core.fsmonitor=false', 'checkout', target)
  end
end

def run_android_trace_tests
  clear_logcat

  command = [
    './gradlew',
    '--no-configuration-cache',
    ':paymentsheet-example:connectedBaseDebugAndroidTest',
    "-Pandroid.testInstrumentationRunnerArguments.class=#{LATENCY_TEST_CLASS}",
    '-PLATENCY_EXPERIMENT_ITERATIONS=1'
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

def parse_trace_output(output)
  sessions = {}
  current_test_name = nil
  test_started_at = nil
  active_spans = {}

  output.each_line do |line|
    if line =~ /LATENCY_TEST_CASE_STARTED:\s*(.+?)\s*$/
      current_test_name = Regexp.last_match(1).strip
      test_started_at = nil
      active_spans = {}
      next
    end

    if line =~ /LATENCY_TEST_CASE_FINISHED:\s*(.+?)\s*$/
      finished_test_name = Regexp.last_match(1).strip
      current_test_name = nil if finished_test_name == current_test_name
      test_started_at = nil
      active_spans = {}
      next
    end

    if line =~ /DURATION_STARTED:\s*(\w+):\s*(\d+)/
      span_name = Regexp.last_match(1)
      started_at = Regexp.last_match(2).to_i
      next unless tracked_duration_key?(span_name)

      active_spans[span_name] = started_at
      test_started_at ||= started_at
      next
    end

    next unless line =~ /DURATION_ENDED:\s*(\w+):\s*(\d+)/
    span_name = Regexp.last_match(1)
    ended_at = Regexp.last_match(2).to_i
    next unless tracked_duration_key?(span_name)
    next if current_test_name.nil? || test_started_at.nil?

    started_at = active_spans.delete(span_name)
    next if started_at.nil?

    session = sessions[current_test_name] ||= TraceSession.new(
      test_name: current_test_name,
      total_duration_ms: nil,
      spans: []
    )
    session.spans << TraceSpan.new(
      name: humanize_span_name(span_name),
      start_offset_ms: started_at - test_started_at,
      duration_ms: ended_at - started_at,
    )
    if span_name == PRIMARY_DURATION_KEY
      session.total_duration_ms = ended_at - started_at
    end
  end

  sessions.each_value do |session|
    session.total_duration_ms ||= session.spans.max_by(&:duration_ms)&.duration_ms
  end

  raise 'No load traces found in test output' if sessions.empty?

  sessions.values
end

def humanize_test_name(test_name)
  test_name.split('_').map(&:capitalize).join(' ')
end

def humanize_span_name(span_name)
  return span_name if span_name == PRIMARY_DURATION_KEY

  span_name
    .sub(/^#{TRACE_DURATION_KEY_PREFIX}/, '')
    .gsub(/([a-z])([A-Z])/, '\1 \2')
    .strip
end

def tracked_duration_key?(span_name)
  span_name == PRIMARY_DURATION_KEY || span_name.start_with?(TRACE_DURATION_KEY_PREFIX)
end

def gantt_value(value)
  [value.round, 1].max
end

def gantt_end(start_offset_ms, duration_ms)
  [gantt_value(start_offset_ms + duration_ms), gantt_value(start_offset_ms) + 1].max
end

def print_mermaid(commit, sessions)
  puts
  puts 'gantt'
  puts "    title PaymentSheet Loading Trace - #{commit[0, 10]}"
  puts '    dateFormat x'

  task_index = 0

  sessions.each do |session|
    puts
    puts "    section #{humanize_test_name(session.test_name)} (Latency #{format('%.0f', session.total_duration_ms)}ms)"

    session.spans.sort_by { |span| [span.start_offset_ms, -span.duration_ms, span.name] }.each do |span|
      puts(
        "    #{span.name} (#{format('%.0f', span.duration_ms)}ms) " \
        ":t#{task_index}, #{span.start_offset_ms.round}, #{gantt_end(span.start_offset_ms, span.duration_ms)}"
      )
      task_index += 1
    end
  end
end

options = {}

OptionParser.new do |opts|
  opts.banner = <<~BANNER
    Usage: generate_latency_trace_mermaid.rb --commit COMMIT

    Checks out a commit, runs TestLatency once, and prints Mermaid gantt syntax
    based on DurationProvider Loading logs.
  BANNER

  opts.on('--commit COMMIT', 'Commit to trace (required)') do |commit|
    options[:commit] = commit
  end

  opts.on('-h', '--help', 'Prints this help') do
    puts opts
    exit
  end
end.parse!

if options[:commit].nil?
  warn 'Error: --commit is required'
  exit 1
end

unless inside_clean_worktree?
  warn 'Error: stripe-android has uncommitted changes. Commit or stash them before running this script.'
  exit 1
end

original_checkout = current_checkout
puts 'Configuration:'
puts "  Commit: #{options[:commit]}"
puts "  Original checkout: #{original_checkout}"
puts "  Logcat buffer size: #{LOGCAT_BUFFER_SIZE}"

begin
  checkout_commit(options[:commit])
  sessions = parse_trace_output(run_android_trace_tests)
  print_mermaid(options[:commit], sessions)
ensure
  restore_checkout(original_checkout)
end
