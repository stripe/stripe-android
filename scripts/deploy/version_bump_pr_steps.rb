#!/usr/bin/env ruby

require 'open3'
require 'octokit'

require_relative 'common'

def ensure_clean_repo()
  repo_dir = File.basename(Dir.getwd)
  if repo_dir != "stripe-android"
    abort("You must run this script from 'stripe-android'.")
  end

  ensure_directory_is_clean()
end

def pull_latest()
    execute_or_fail("git checkout #{@deploy_branch}")
    execute_or_fail("git pull")
end

def switch_to_release_branch()
    switch_to_new_branch(release_branch, @deploy_branch)
end

def revert_version_bump_changes()
    delete_git_branch(release_branch, @deploy_branch)
end

def create_version_bump_pr()
    switch_to_release_branch()
    update_read_me()
    update_stripe_sdk_version()
    update_gradle_properties()
    update_changelog()
    execute_or_fail("git commit -m \"Bump version to #{@version}\"")

    begin
        execute_or_fail("git push -u origin")

        pr_description = create_pr_description()

        create_pr(
             release_branch,
             "Bump version to #{@version}",
             pr_description,
             "Merge the version bump PR"
        )
    rescue
        revert_version_bump_changes()
        execute("git checkout #{@deploy_branch}")
        raise
    end

    # If this is a dry run, we need to stay on the release/ branch to do a github release. This is
    # because the github release depends on the changes we made to the CHANGELOG.
    if (!@is_dry_run)
        execute_or_fail("git checkout #{@deploy_branch}")
    end
end

private def release_branch
    "release/#{@version}"
end

private def create_pr_description()
    template_file_path = File.join(File.dirname(__FILE__), '../../.github/PULL_REQUEST_TEMPLATE.md')
    template_file = File.open(template_file_path)
    template = template_file.read
    template_file.close

    summary = <<~EOS
    Bump version to `#{@version}`
    EOS

    template["<!-- Simple summary of what was changed. -->"] = summary
    template
end
