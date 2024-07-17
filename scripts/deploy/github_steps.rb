#!/usr/bin/env ruby

require 'open3'
require 'octokit'

private def execute_or_fail(command)
    system(command) or raise "Failed to execute #{command}"
end

def release_branch
    "release/#{@version}"
end

def ensure_clean_repo()
  repo_dir = File.basename(Dir.getwd)
  if repo_dir != "stripe-android"
    abort("You must run this script from 'stripe-android'.")
  end

  stdout, stderr, status = Open3.capture3("git status --porcelain")

  if !stdout.empty?
    abort("You must have a clean working directory to continue.")
  end
end

def pull_latest()
    execute_or_fail("git checkout #{@branch}")
    execute_or_fail("git pull")
end

def switch_to_release_branch()
    execute_or_fail("git checkout -b #{release_branch}")
end

private def github_login
  token = `fetch-password -q bindings/gh-tokens/$USER`
  if $?.exitstatus != 0
    puts "Couldn't fetch GitHub token. Follow the Android SDK Deploy Guide (https://go/android-sdk-deploy) to set up a token. \a".red
    exit(1)
  end
  client = Octokit::Client.new(access_token: token)
  abort('Invalid GitHub token. Follow the wiki instructions for setting up a GitHub token.') unless client.login
  client
end

def create_pr_description()
    template_file_path = File.join(File.dirname(__FILE__), '../.github/PULL_REQUEST_TEMPLATE.md')
    template_file = File.open(template_file_path)
    template = template_file.read
    template_file.close

    summary = <<~EOS
    Bump version to `#{@version}`
    EOS

    template["<!-- Simple summary of what was changed. -->"] = summary
    template
end

def create_version_bump_pr()
    execute_or_fail("git add *")
    execute_or_fail("git commit -m \"Bump version to #{@version}\"")
    execute_or_fail("git push -u origin")

    pr_description = create_pr_description()
    # TODO: make this a draft if it is a dry run
    github_login.create_pull_request(
        "stripe/stripe-android",
        "master",
        release_branch,
        "Bump version to #{@version}",
        pr_description
    )
end