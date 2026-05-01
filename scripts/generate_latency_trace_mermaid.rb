#!/usr/bin/env ruby
# frozen_string_literal: true

require 'optparse'
require 'open3'

PROJECT_ROOT = File.expand_path('..', __dir__)
TRACE_CLASS = 'com.stripe.android.lpm.MPELatencyTest'
TRACE_PREFIX = 'MPE_LOAD_TRACE'
TRACE_LOG_TAG = 'MPELoadTrace'

TraceSpan = Struct.new(:name, :start_offset_ms, :duration_ms, keyword_init: true)
TraceSession = Struct.new(:test_name, :total_duration_ms, :spans, keyword_init: true)

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

def run_android_trace_tests
  clear_logcat

  command = [
    './gradlew',
    '--no-configuration-cache',
    ':paymentsheet-example:connectedBaseDebugAndroidTest',
    "-Pandroid.testInstrumentationRunnerArguments.class=#{TRACE_CLASS}",
    '-Pandroid.testInstrumentationRunnerArguments.mpe_trace_enabled=true'
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
    "#{TRACE_LOG_TAG}:I",
    '*:S',
    chdir: PROJECT_ROOT
  )

  return stdout if status.success?

  warn stdout
  raise 'Failed to read adb logcat. Ensure adb is installed and a device/emulator is available.'
end

def parse_trace_output(output)
  sessions = {}

  output.each_line do |line|
    if line =~ /#{TRACE_PREFIX}\|SESSION\|([^|]+)\|([\d.]+)/
      test_name = Regexp.last_match(1).strip
      total_duration_ms = Regexp.last_match(2).to_f
      sessions[test_name] ||= TraceSession.new(
        test_name: test_name,
        total_duration_ms: total_duration_ms,
        spans: []
      )
      sessions[test_name].total_duration_ms = total_duration_ms
    elsif line =~ /#{TRACE_PREFIX}\|SPAN\|([^|]+)\|([^|]+)\|([\d.]+)\|([\d.]+)/
      test_name = Regexp.last_match(1).strip
      span_name = Regexp.last_match(2).strip
      start_offset_ms = Regexp.last_match(3).to_f
      duration_ms = Regexp.last_match(4).to_f
      sessions[test_name] ||= TraceSession.new(
        test_name: test_name,
        total_duration_ms: nil,
        spans: []
      )
      sessions[test_name].spans << TraceSpan.new(
        name: span_name,
        start_offset_ms: start_offset_ms,
        duration_ms: duration_ms
      )
    end
  end

  sessions.each_value do |session|
    if session.total_duration_ms.nil?
      raise "Missing session total for #{session.test_name}"
    end
  end

  raise 'No load traces found in test output' if sessions.empty?

  sessions.values
end

def humanize_test_name(test_name)
  test_name.split('_').map(&:capitalize).join(' ')
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
  puts "    title PaymentSheet Load Trace - #{commit[0, 10]}"
  puts '    dateFormat x'

  task_index = 0

  sessions.each do |session|
    puts
    puts "    section #{humanize_test_name(session.test_name)} (Latency: #{format('%.2f', session.total_duration_ms)}ms)"
    puts "    Load (#{format('%.3f', session.total_duration_ms)}ms) :t#{task_index}, 0, #{gantt_value(session.total_duration_ms)}"
    task_index += 1

    session.spans
      .sort_by { |span| [span.start_offset_ms, -span.duration_ms, span.name] }
      .each do |span|
        puts(
          "    #{span.name} (#{format('%.3f', span.duration_ms)}ms) " \
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

    Checks out a commit, runs MPE trace mode once, and prints Mermaid gantt syntax.
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

begin
  checkout_commit(options[:commit])
  sessions = parse_trace_output(run_android_trace_tests)
  print_mermaid(options[:commit], sessions)
ensure
  restore_checkout(original_checkout)
end
