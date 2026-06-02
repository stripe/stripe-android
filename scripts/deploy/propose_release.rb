#!/usr/bin/env ruby

require_relative 'permissions_check'
require_relative 'release_cli'
require_relative 'release_tag_steps'
require_relative 'translations'
require_relative 'update_version_numbers'
require_relative 'validate_version_number'
require_relative 'version_bump_pr_steps'

def print_propose_release_handoff
    rputs "propose release complete"
    puts "Version: #{@version}"
    puts "Branch: #{release_branch}"
    puts "Tag: #{release_tag_name}"

    if @is_dry_run
        rputs "Dry run: the signed tag was created locally but not pushed. Use deploy release only after a real propose release has created the final remote tag."
    else
        rputs "Hand off #{release_tag_name} to deploy release."
    end
end

def pause_for_pr_merge
    if @is_dry_run
        rputs "Dry run: skipping PR merge wait. Continuing to create release tag."
        return
    end

    resume_command = "./scripts/deploy/propose_release.rb --continue-from 8 --version #{@version}"
    resume_command += " --branch #{@deploy_branch}" if @deploy_branch != 'master'
    puts ""
    rputs "Version bump PR has been created. Get it reviewed and merged."
    puts ""
    puts "Once the PR is merged, resume the release process by running:"
    puts ""
    puts "  #{resume_command}"
    puts ""

    exit 0
end

def prepare_propose_release_resume(step_index)
    # When resuming after the PR merge pause (step 8+), ensure we have a
    # clean repo and are on the deploy branch with latest changes.
    return if step_index < 8

    ensure_clean_repo
    execute_or_fail("git checkout #{@deploy_branch}")
    execute_or_fail("git pull")
end

def cleanup_propose_release_dry_run
    delete_release_tag
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
    method(:pause_for_pr_merge),
    method(:create_release_tag),
    method(:print_propose_release_handoff),
]

@propose_release_succeeded = false

begin
    execute_steps(steps, before_resume: method(:prepare_propose_release_resume))
    @propose_release_succeeded = true
ensure
    if @is_dry_run && @propose_release_succeeded
        rputs "Press enter to clean up the dry run branch and tag."
        wait_for_user
        cleanup_propose_release_dry_run
    end
end
