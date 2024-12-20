#!/usr/bin/env ruby

require 'colorize'
require 'optparse'

require_relative 'common'
require_relative 'create_github_release'
require_relative 'permissions_check'
require_relative 'publish_to_sonatype'
require_relative 'update_dokka'
require_relative 'update_pay_server_docs'
require_relative 'update_version_numbers'
require_relative 'validate_version_number'
require_relative 'version_bump_pr_steps'

@step_index = 1
@is_dry_run = false
@deploy_branch = 'master'
@is_older_version = false

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
      @deploy_branch = t
  end

  opts.on('--release-older-version', "Indicates you are not releasing the newest version of stripe-android.") do |t|
      @is_older_version = t
  end

end.parse!

steps = [
    # Prep for making changes
    method(:check_permissions),
    method(:validate_version_number),
    method(:ensure_clean_repo),
    method(:pull_latest),

    # Update version number
    method(:create_version_bump_pr),

    # Actually release a new SDK version
    method(:publish_to_sonatype),

    # Create a Github release
    method(:create_github_release),

    # Do docs updates
    method(:generate_dokka),
    method(:update_pay_server_docs),
]

execute_steps(steps, @step_index)

if (@is_dry_run)
    rputs "Press enter to revert all changes made during the dry run."
    wait_for_user()

    delete_github_release()
    revert_version_bump_changes()
    revert_dokka_changes()
    delete_pay_server_branch()
end
