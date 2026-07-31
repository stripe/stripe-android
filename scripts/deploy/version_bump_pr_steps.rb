#!/usr/bin/env ruby

require 'open3'
require 'octokit'
require 'uri'

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

def revert_version_bump_changes()
    delete_git_branch(release_branch, @deploy_branch)
end

def create_version_bump_pr()
    if @is_headless
        switch_to_headless_release_branch()
    else
        switch_to_release_branch()
    end
    update_read_me()
    update_stripe_sdk_version()
    update_gradle_properties()
    update_changelog()
    update_version()
    update_3ds2_version()
    execute_or_fail("git commit -m \"Bump version to #{@version}\"")

    begin
        if @is_headless
            execute_or_fail("git push --force-with-lease -u origin #{release_branch}")
        else
            execute_or_fail("git push -u origin")
        end

        pr_description = create_pr_description()
        pr_title = "Bump version to #{@version}"

        if @is_headless
            print_headless_pr_handoff(pr_title, pr_description)
            return
        end

        if (@is_dry_run)
          user_message = "Verify that a draft PR containing version number bumps was opened."
        else
          user_message = "Verify that a PR containing version number bumps was opened. Press enter once you've confirmed the PR was created."
        end

        create_pr(
           release_branch,
           pr_title,
           pr_description,
           user_message
        )
    rescue
        if @is_headless
            execute("git checkout #{@deploy_branch}")
        else
            revert_version_bump_changes()
            execute("git checkout #{@deploy_branch}")
        end
        raise
    end
end

private def release_branch
    "release/#{@version}"
end

private def switch_to_headless_release_branch
    execute_or_fail("git fetch origin #{@deploy_branch}")
    execute_or_fail("git checkout -B #{release_branch} origin/#{@deploy_branch}")
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

private def print_headless_pr_handoff(pr_title, pr_description)
    compare_url = "https://github.com/stripe/stripe-android/compare/#{@deploy_branch}...#{release_branch}"
    create_pr_url = "#{compare_url}?#{URI.encode_www_form(
        quick_pull: 1,
        title: pr_title,
        body: pr_description
    )}"

    rputs "Headless propose release complete. The release branch was pushed, but no PR was created."
    puts ""
    puts "PR title:"
    puts pr_title
    puts ""
    puts "PR body:"
    puts pr_description
    puts ""
    puts "Create PR URL:"
    puts create_pr_url
    puts ""
    puts "Compare URL:"
    puts compare_url
end
