#!/usr/bin/env ruby

require 'colorize'
require 'optparse'

require_relative 'github_steps'
require_relative 'update_version_numbers'
require_relative 'validate_version_number'

@step_index = 1
@is_dry_run = false
@branch = 'master'

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

# TODO: update the string here to make it clear what we are waiting for
def wait_for_user
    rputs "Press enter to continue..."
    STDIN.gets
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

  # TODO: understand do |s| vs. do |t|
  opts.on('--dry-run', "Don't do a real deployment, but test what would happen if you did") do |s|
      @is_dry_run = s
  end

  opts.on('--branch', "Branch to deploy from") do |t|
      @branch = t
  end
end.parse!

steps = [
    # Prep for making changes
    method(:validate_version_number),
    method(:ensure_clean_repo),
    method(:pull_latest),
    method(:switch_to_release_branch),

    # Update version numbers
    method(:update_read_me),
    method(:update_stripe_sdk_version),
    method(:update_gradle_properties),

    # Create PR
    method(:create_version_bump_pr),
]

execute_steps(steps, @step_index)

if (@is_dry_run)
    wait_for_user()
end

# TODO: do clean up.
