#!/usr/bin/env ruby

require 'subprocess'

require_relative 'common'
require_relative 'gnupg_utils'

def fetch_origin_and_tags
    execute_or_fail("git fetch --tags origin #{@deploy_branch}")
end

def verify_release_pr_merged
    pr = fetch_release_pr
    if pr.nil?
        raise "Could not find a version bump PR for #{release_branch} targeting #{@deploy_branch}. Run `propose release` first."
    end

    if pr.merged_at.nil?
        raise "Version bump PR for #{release_branch} is not merged yet: #{pr.html_url}"
    end

    if pr.merge_commit_sha.nil? || pr.merge_commit_sha.empty?
        raise "Version bump PR for #{release_branch} is merged, but GitHub did not return a merge commit sha."
    end

    @release_source_sha = pr.merge_commit_sha
    puts "Verified merged version bump PR: #{pr.html_url}".green
    pr
end

def prepare_release_source_from_merged_pr
    verify_release_pr_merged
    fetch_origin_and_tags

    unless git_commit_exists?(@release_source_sha)
        raise "Merged PR commit #{@release_source_sha} is not available locally after fetching #{@deploy_branch}."
    end
end

def verify_release_tag_exists
    unless local_git_tag_exists?(release_tag_name)
        raise "Release tag #{release_tag_name} does not exist locally. Run `git fetch origin --tags` or confirm `propose release` completed."
    end
end

def create_release_tag
    if @is_dry_run
        pr = fetch_release_pr
        if pr.nil?
            raise "Could not find a version bump PR for #{release_branch} targeting #{@deploy_branch}. Run `propose release` first."
        end

        if pr.head.sha.nil? || pr.head.sha.empty?
            raise "Could not determine the release branch head for dry-run tag creation."
        end

        @release_source_sha = pr.head.sha
        rputs "Dry run: using release branch head #{@release_source_sha} as a stand-in for the merged PR revision."
    else
        prepare_release_source_from_merged_pr
    end

    if local_git_tag_exists?(release_tag_name)
        verify_release_tag_matches_release_source
    elsif remote_git_tag_exists?(release_tag_name)
        fetch_origin_and_tags
        verify_release_tag_matches_release_source
    else
        create_local_release_tag
    end

    if @is_dry_run
        rputs "Dry run: verified local signed tag #{release_tag_name} at #{@release_source_sha} without pushing it to origin."
        return
    end

    unless remote_git_tag_exists?(release_tag_name)
        execute_or_fail("git push origin #{release_tag_name}")
    end
end

def checkout_release_source
    begin
        prepare_release_source_from_merged_pr
        verify_release_tag_matches_release_source
        checkout_release_tag
    rescue StandardError => e
        raise unless @is_dry_run

        rputs "Dry run: continuing without checked-out release source: #{e.message}"
        rputs "Dry run: remaining deploy release steps will run from the current checkout instead of #{release_tag_name}."
    end
end

def verify_release_tag_matches_release_source
    verify_release_tag_exists
    tag_commit = git_commit_for_ref("refs/tags/#{release_tag_name}")

    if tag_commit != @release_source_sha
        raise "Release tag #{release_tag_name} points to #{tag_commit}, but the merged PR revision is #{@release_source_sha}."
    end

    puts "Verified release tag #{release_tag_name} points to #{@release_source_sha}".green
end

def checkout_release_tag
    verify_release_tag_exists
    execute_or_fail("git checkout --detach refs/tags/#{release_tag_name}")
    @checked_out_release_tag = true
end

def delete_release_tag(delete_remote: false, delete_local: true)
    if delete_remote && remote_git_tag_exists?(release_tag_name)
        execute("git push origin :refs/tags/#{release_tag_name}")
    end

    if delete_local && local_git_tag_exists?(release_tag_name)
        execute("git tag -d #{release_tag_name}")
    end
end

private def create_local_release_tag
    gnupg_env do |env|
        Subprocess.check_call(
            ["git", "tag", "-s", "-u", gnupg_key_id, "-m", "Version #{@version}", release_tag_name, @release_source_sha],
            env: env
        )
    end
end

private def fetch_release_pr
    octokit_client.auto_paginate = true
    pull_requests = octokit_client.pull_requests(
        "stripe/stripe-android",
        state: "all",
        head: "stripe:#{release_branch}",
        base: @deploy_branch,
    )

    matching_prs = pull_requests.select do |pr|
        pr.head.ref == release_branch && pr.base.ref == @deploy_branch
    end

    matching_prs.max_by { |pr| pr.updated_at || pr.created_at }
end

private def gnupg_key_id
    fetch_password("bindings/gnupg/fingerprint").strip
end
