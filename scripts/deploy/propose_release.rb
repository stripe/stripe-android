#!/usr/bin/env ruby

require_relative 'permissions_check'
require_relative 'release_cli'
require_relative 'translations'
require_relative 'update_version_numbers'
require_relative 'validate_version_number'
require_relative 'version_bump_pr_steps'

def print_propose_release_handoff
    rputs "propose release complete"
    puts "Version: #{@version}"
    puts "Branch: #{release_branch}"
    puts ""
    rputs "Wait for the version bump PR to merge, then let the deployer know that #{@version} is ready to deploy."
end

def cleanup_propose_release_dry_run
    delete_git_branch(release_branch, @deploy_branch)
end

parse_release_options!(flow_name: 'propose release')

steps = [
    method(:check_permissions),
    method(:validate_translations_merged),
    method(:validate_version_number),
    method(:ensure_clean_repo),
    method(:pull_latest),
    method(:create_version_bump_pr),
    method(:print_propose_release_handoff),
]

@propose_release_succeeded = false

begin
    execute_steps(steps)
    @propose_release_succeeded = true
ensure
    if @is_dry_run && @propose_release_succeeded
        rputs "Press enter to clean up the dry run branch and tag."
        wait_for_user
        cleanup_propose_release_dry_run
    end
end
