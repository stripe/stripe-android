#!/usr/bin/env ruby

require 'colorize'
require 'optparse'

require_relative 'update_version_numbers'
require_relative 'validate_version_number'

@step_index = 1

def rputs(string)
    puts string.red
end

def execute_steps(steps, step_index)
  step_count = steps.length

  if step_index > 1
    steps = steps.drop(step_index - 1)
    rputs "Continuing from step #{step_index}: #{steps.first.name}"
  end

  begin
    steps.each do |step|
      rputs "# #{step.name} (step #{step_index}/#{step_count})"
      step.call
      step_index += 1
    end
  rescue Exception => e
    rputs "Restart with --continue-from #{step_index} to re-run from this step."
    raise
  end
end

OptionParser.new do |opts|
  opts.on('--version VERSION',
          'Version to release (e.g. 21.2.0)') do |t|
    @version = t
  end

  opts.on('--continue-from NUMBER',
          'Continue from a specified step') do |t|
    @step_index = t.to_i
  end
end.parse!

steps = [
    method(:validate_version_number),
    method(:update_read_me),
    method(:update_stripe_sdk_version),
    method(:update_gradle_properties),
]

execute_steps(steps, @step_index)
