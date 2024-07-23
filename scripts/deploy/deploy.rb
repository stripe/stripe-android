#!/usr/bin/env ruby

require 'colorize'
require 'optparse'

require_relative 'common'
require_relative 'create_github_release'
require_relative 'github_steps'
require_relative 'update_version_numbers'
require_relative 'validate_version_number'

@step_index = 1
@is_dry_run = false
@branch = 'master'

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

  opts.on('--dry-run', "Don't do a real deployment, but test what would happen if you did") do |t|
      @is_dry_run = t
  end

  opts.on('--branch BRANCH', "Branch to deploy from") do |t|
      @branch = t
  end
end.parse!

steps = [
    # Prep for making changes
    method(:validate_version_number),
    method(:ensure_clean_repo),
    method(:pull_latest),
    method(:switch_to_release_branch),

    # Update version number
    method(:update_read_me),
    method(:update_stripe_sdk_version),
    method(:update_gradle_properties),
    method(:update_changelog),
    method(:create_version_bump_pr),

    # TODO: Does this need to be done on @branch or should it be done on release/version branch?
    # TODO: try it on release branch and see what it looks like
    method(:create_github_release)
]

execute_steps(steps, @step_index)

if (@is_dry_run)
    rputs "Please verify that the dry run worked as expected.

    You should see a PR opened that bumps version numbers in the stripe-android codebase on branch release/<new release number>.
    There should also be a draft release opened in the stripe-android repo which includes a changelog for the new version.

    When you're done, press enter to revert all changes."
    wait_for_user()

    revert_all_changes()
    delete_github_release()
    execute_or_fail("git checkout #{@branch}")
    delete_release_branch()
end
