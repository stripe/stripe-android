#!/usr/bin/env ruby
# frozen_string_literal: true

require 'base64'
require 'optparse'
require 'open3'
require_relative 'latency_test_utils'

PROJECT_ROOT = LatencyTestUtils::PROJECT_ROOT
DEFAULT_PRIMARY_DURATION_KEY = LatencyTestUtils::DURATION_KEY
TRACE_DURATION_KEY_PREFIX = 'PaymentSheetLoad'
MINGLE_DIAGRAM_URL = 'https://pages.stripe.me/mingle/diagrams?diagram='

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
  Dir.chdir(PROJECT_ROOT) do
    success = system('git', '-c', 'core.fsmonitor=false', 'checkout', target)
    warn "Failed to restore original checkout: #{target}" unless success
  end
end

def parse_trace_output(output, primary_duration_key: DEFAULT_PRIMARY_DURATION_KEY)
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

      active_spans[span_name] = started_at
      test_started_at ||= started_at
      next
    end

    next unless line =~ /DURATION_ENDED:\s*(\w+):\s*(\d+)/
    span_name = Regexp.last_match(1)
    ended_at = Regexp.last_match(2).to_i
    next if current_test_name.nil? || test_started_at.nil?

    started_at = active_spans.delete(span_name)
    next if started_at.nil?

    session = sessions[current_test_name] ||= TraceSession.new(
      test_name: current_test_name,
      total_duration_ms: nil,
      spans: []
    )
    session.spans << TraceSpan.new(
      name: humanize_span_name(span_name, primary_duration_key: primary_duration_key),
      start_offset_ms: started_at - test_started_at,
      duration_ms: ended_at - started_at,
    )
    if span_name == primary_duration_key
      session.total_duration_ms = ended_at - started_at
    end
  end

  sessions.each_value do |session|
    session.total_duration_ms ||= session.spans.map { |span| span.start_offset_ms + span.duration_ms }.max
  end

  raise 'No completed duration traces found in test output' if sessions.empty?

  sessions.values
end

def humanize_test_name(test_name)
  test_name.split('_').map(&:capitalize).join(' ')
end

def humanize_span_name(span_name, primary_duration_key: DEFAULT_PRIMARY_DURATION_KEY)
  return span_name if span_name == primary_duration_key

  span_name
    .sub(/^#{TRACE_DURATION_KEY_PREFIX}/, '')
    .gsub(/([a-z])([A-Z])/, '\1 \2')
    .strip
end

def add_manual_session_markers(output, primary_duration_key:, session_name:)
  return output if output.include?('LATENCY_TEST_CASE_STARTED:')

  marked_output = +''
  session_number = 0
  current_session_name = nil

  output.each_line do |line|
    if line =~ /DURATION_STARTED:\s*#{Regexp.escape(primary_duration_key)}:\s*\d+/
      session_number += 1
      current_session_name = session_number == 1 ? session_name : "#{session_name}_#{session_number}"
      marked_output << "LATENCY_TEST_CASE_STARTED: #{current_session_name}\n"
    end

    marked_output << line

    if current_session_name && line =~ /DURATION_ENDED:\s*#{Regexp.escape(primary_duration_key)}:\s*\d+/
      marked_output << "LATENCY_TEST_CASE_FINISHED: #{current_session_name}\n"
      current_session_name = nil
    end
  end

  marked_output
end

def gantt_value(value)
  [value.round, 1].max
end

def gantt_end(start_offset_ms, duration_ms)
  [gantt_value(start_offset_ms + duration_ms), gantt_value(start_offset_ms) + 1].max
end

def build_mermaid_diagram(trace_target, sessions, title: 'PaymentSheet Duration Trace')
  diagram_lines = []
  diagram_lines << "%%{init: {'gantt': {'titleTopMargin': 50, 'topPadding': 100, 'leftPadding': 200}}}%%"
  diagram_lines << 'gantt'
  diagram_lines << "    title #{title} - #{format_trace_target(trace_target)}"
  diagram_lines << '    dateFormat x'

  task_index = 0

  sessions.each do |session|
    diagram_lines << ''
    diagram_lines << "    section #{humanize_test_name(session.test_name)} (Latency #{format('%.0f', session.total_duration_ms)}ms)"

    session.spans.sort_by { |span| [span.start_offset_ms, -span.duration_ms, span.name] }.each do |span|
      diagram_lines << (
        "    #{span.name} (#{format('%.0f', span.duration_ms)}ms) " \
        ":t#{task_index}, #{span.start_offset_ms.round}, #{gantt_end(span.start_offset_ms, span.duration_ms)}"
      )
      task_index += 1
    end
  end

  diagram_lines.join("\n")
end

def build_diagram_url(trace_target, sessions, title: 'PaymentSheet Duration Trace')
  encoded_diagram = Base64.strict_encode64(build_mermaid_diagram(trace_target, sessions, title: title))
  "#{MINGLE_DIAGRAM_URL}#{encoded_diagram}"
end

def collect_trace_output
  original_stdout = $stdout

  begin
    $stdout = $stderr
    LatencyTestUtils.run_android_latency_tests(1)
  ensure
    $stdout = original_stdout
  end
end

def format_trace_target(trace_target)
  trace_target.match?(/\A[0-9a-f]{10,40}\z/i) ? trace_target[0, 10] : trace_target
end

options = {
  primary_duration_key: DEFAULT_PRIMARY_DURATION_KEY,
  title: 'PaymentSheet Duration Trace',
}

OptionParser.new do |opts|
  opts.banner = <<~BANNER
    Usage: generate_loader_flamegraph.rb [--commit COMMIT]
           generate_loader_flamegraph.rb --input FILE [options]
           generate_loader_flamegraph.rb --stdin [options]

    Runs TestLatency once and prints a Mingle diagram URL with the Mermaid
    gantt chart encoded into the link. When --commit is provided, the script
    checks out that commit before running and restores the original checkout
    afterward.

    --input and --stdin parse previously captured StripeSdk logcat output. Each
    primary-duration span is treated as a separate trace session. Duration
    lines are emitted by debug SDK builds.

    Checkout Session example:
      adb logcat -d -s StripeSdk:D '*:S' > checkout-session.log
      ./scripts/generate_loader_flamegraph.rb --input checkout-session.log --primary-duration CheckoutSessionConfigure
  BANNER

  opts.on('--commit COMMIT', 'Commit to trace (defaults to current checkout)') do |commit|
    options[:commit] = commit
  end

  opts.on('--input FILE', 'Read StripeSdk logcat output from a file') do |file|
    options[:input] = file
  end

  opts.on('--stdin', 'Read StripeSdk logcat output from standard input') do
    options[:stdin] = true
  end

  opts.on('--primary-duration KEY', 'Overall duration key (defaults to Loading)') do |key|
    options[:primary_duration_key] = key
  end

  opts.on('--label LABEL', 'Label for an imported trace (defaults to the input filename)') do |label|
    options[:label] = label
  end

  opts.on('--title TITLE', 'Diagram title') do |title|
    options[:title] = title
  end

  opts.on('-h', '--help', 'Prints this help') do
    puts opts
    exit
  end
end.parse!

input_option_count = [options[:input], options[:stdin]].compact.length
if input_option_count > 1 || (input_option_count == 1 && options[:commit])
  warn 'Error: use only one of --commit, --input, or --stdin.'
  exit 1
end

if input_option_count == 1
  input = options[:stdin] ? $stdin.read : File.read(options[:input])
  default_label = options[:stdin] ? 'stdin' : File.basename(options[:input])
  trace_target = options[:label] || default_label
  marked_input = add_manual_session_markers(
    input,
    primary_duration_key: options[:primary_duration_key],
    session_name: trace_target,
  )
  sessions = parse_trace_output(marked_input, primary_duration_key: options[:primary_duration_key])
  puts build_diagram_url(trace_target, sessions, title: options[:title])
  exit
end

original_checkout = current_checkout
trace_target = options[:commit] || original_checkout

if options[:commit] && !inside_clean_worktree?
  warn 'Error: stripe-android has uncommitted changes. Commit or stash them before running this script.'
  exit 1
end

begin
  checkout_commit(options[:commit]) if options[:commit]
  sessions = parse_trace_output(
    collect_trace_output,
    primary_duration_key: options[:primary_duration_key],
  )
  puts build_diagram_url(trace_target, sessions, title: options[:title])
ensure
  restore_checkout(original_checkout) if options[:commit]
end
