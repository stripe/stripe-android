#!/usr/bin/env ruby
# frozen_string_literal: true

require 'optparse'
require_relative 'latency_test_utils'

DEFAULT_DURATION_KEY = 'AutocompleteFindPredictions'
DEFAULT_SAMPLE_COUNT = 10
MODES = %w[sdk proxy].freeze

def parse_duration_samples(output, duration_key:)
  start_times = []
  durations = []

  output.each_line do |line|
    if line =~ /DURATION_STARTED:\s*#{Regexp.escape(duration_key)}:\s*(\d+)/
      start_times << Regexp.last_match(1).to_i
      next
    end

    next unless line =~ /DURATION_ENDED:\s*#{Regexp.escape(duration_key)}:\s*(\d+)/
    next if start_times.empty?

    end_time = Regexp.last_match(1).to_i
    start_time = start_times.shift
    durations << (end_time - start_time) / 1000.0
  end

  durations
end

def mean(values)
  values.sum / values.length.to_f
end

def median(values)
  sorted = values.sort
  midpoint = sorted.length / 2

  if sorted.length.odd?
    sorted[midpoint]
  else
    (sorted[midpoint - 1] + sorted[midpoint]) / 2.0
  end
end

def format_ms(value)
  format('%.1fms', value * 1000)
end

def print_mode_instructions(mode:, samples:, duration_key:)
  puts
  puts '=' * 80
  puts "Mode: #{mode}"
  puts '=' * 80
  puts "Duration key: #{duration_key}"
  puts "Expected samples: #{samples}"
  puts
  puts 'Before collecting samples:'
  puts '  1. Open the PaymentSheet playground on the device or emulator.'
  puts '  2. Enable inline autocomplete and address autocomplete in playground settings.'
  puts '  3. Use full billing address collection so the inline address field is shown.'
  puts "  4. Configure the app for the #{mode} path you want to measure."
  puts '  5. Use the same query text each run and do not select a result.'
  puts '  6. Clear the field between runs so each request is a fresh prediction lookup.'
  puts
  puts 'For proxy mode, make sure the backend gates are enabled and the checkout account can use the endpoint.'
  puts
end

def wait_for_user(message)
  puts message
  STDIN.gets
end

def collect_samples(mode:, samples:, duration_key:)
  print_mode_instructions(mode: mode, samples: samples, duration_key: duration_key)
  wait_for_user('Press Enter when you are ready to clear logcat and begin collection...')

  LatencyTestUtils.clear_logcat
  wait_for_user("Logcat cleared. Perform #{samples} autocomplete lookups now, then press Enter to finish collection...")

  durations = parse_duration_samples(
    LatencyTestUtils.dump_logcat,
    duration_key: duration_key,
  )

  puts
  if durations.empty?
    puts "No #{duration_key} samples were found in logcat."
  else
    puts "Collected #{durations.length}/#{samples} samples."
  end

  durations
end

def print_summary(label, durations)
  puts
  puts "#{label} summary"
  puts '-' * 80

  if durations.empty?
    puts 'No samples collected.'
    return
  end

  puts "Average: #{format_ms(mean(durations))}"
  puts "Median:  #{format_ms(median(durations))}"
  puts "Min:     #{format_ms(durations.min)}"
  puts "Max:     #{format_ms(durations.max)}"
  puts "Raw:     #{durations.map { |duration| format_ms(duration) }.join(', ')}"
end

def print_delta(base_label, base_durations, new_label, new_durations)
  return if base_durations.empty? || new_durations.empty?

  base_mean = mean(base_durations)
  new_mean = mean(new_durations)
  absolute_delta = new_mean - base_mean
  percent_delta = if base_mean.zero?
    0.0
  else
    (absolute_delta / base_mean) * 100.0
  end

  puts
  puts 'Delta'
  puts '-' * 80
  puts "#{new_label} vs #{base_label}: #{format_ms(absolute_delta)} (#{format('%+.1f%%', percent_delta)})"
end

options = {
  compare: false,
  duration_key: DEFAULT_DURATION_KEY,
  mode: 'sdk',
  samples: DEFAULT_SAMPLE_COUNT,
}

OptionParser.new do |opts|
  opts.banner = <<~BANNER
    Usage: measure_autocomplete_latency.rb [options]

    Clears adb logcat, waits for manual inline-autocomplete interactions in the example app,
    then parses DurationProvider markers for autocomplete prediction latency.

    Examples:
      ./scripts/measure_autocomplete_latency.rb --mode sdk --samples 10
      ./scripts/measure_autocomplete_latency.rb --compare --samples 15

    Options:
  BANNER

  opts.on('--mode MODE', MODES, 'Single mode to measure: sdk or proxy (default: sdk)') do |mode|
    options[:mode] = mode
  end

  opts.on('--compare', 'Collect sdk and proxy samples sequentially and print the delta') do
    options[:compare] = true
  end

  opts.on('--samples COUNT', Integer, 'Expected number of manual lookups to collect (default: 10)') do |samples|
    options[:samples] = samples
  end

  opts.on('--duration-key KEY', 'DurationProvider key to parse (default: AutocompleteFindPredictions)') do |key|
    options[:duration_key] = key
  end

  opts.on('-h', '--help', 'Prints this help') do
    puts opts
    exit
  end
end.parse!

if options[:samples] <= 0
  warn '--samples must be greater than 0'
  exit 1
end

if options[:compare]
  sdk_durations = collect_samples(
    mode: 'sdk',
    samples: options[:samples],
    duration_key: options[:duration_key],
  )
  proxy_durations = collect_samples(
    mode: 'proxy',
    samples: options[:samples],
    duration_key: options[:duration_key],
  )

  print_summary('SDK', sdk_durations)
  print_summary('Proxy', proxy_durations)
  print_delta('SDK', sdk_durations, 'Proxy', proxy_durations)
else
  durations = collect_samples(
    mode: options[:mode],
    samples: options[:samples],
    duration_key: options[:duration_key],
  )
  print_summary(options[:mode].upcase, durations)
end
