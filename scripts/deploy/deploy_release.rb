#!/usr/bin/env ruby

require_relative 'create_github_release'
require_relative 'permissions_check'
require_relative 'publish_to_sonatype'
require_relative 'release_cli'
require_relative 'release_tag_steps'
require_relative 'update_pay_server_docs'
require_relative 'version_bump_pr_steps'

def cleanup_deploy_release(success:)
    delete_github_release if @is_dry_run
    delete_release_tag if @is_dry_run && @created_local_release_tag
    execute("git checkout #{@deploy_branch}") if success && @checked_out_release_source
end

def prepare_deploy_release_resume(step_index)
    return if step_index < 4

    checkout_release_source
    verify_release_tag_matches_release_source if step_index > 4 && !@skip_release_tag
end

parse_release_options!(flow_name: 'deploy release', version_required: true)

steps = [
    method(:check_permissions),
    method(:ensure_clean_repo),
    method(:checkout_release_source),
    method(:create_release_tag),
    method(:publish_to_sonatype),
    method(:create_github_release),
    method(:update_pay_server_docs),
]

@deploy_release_succeeded = false

begin
    execute_steps(steps, before_resume: method(:prepare_deploy_release_resume))
    @deploy_release_succeeded = true
ensure
    cleanup_deploy_release(success: @deploy_release_succeeded)
end
