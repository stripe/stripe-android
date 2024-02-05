#!/usr/bin/env ruby

require 'octokit'
require 'open3'

def run_command(command)
    stdout, stderr, _ = Open3.capture3(command)
    unless stderr.empty?
        puts "Command failed with error: #{stderr}"
    end
    stdout
end

diff_command = 'git status --porcelain paymentsheet-example/screenshots/debug'
stdout, _, _ = Open3.capture3(diff_command)

if stdout.empty?
    puts "No differences in screenshots found."
    exit true
end

token = ENV["UPDATE_SCREENSHOTS_COMMENT_TOKEN"]
client = Octokit::Client.new(access_token: token)

if client.login
    branch = ENV["BITRISE_GIT_BRANCH"]
    pull_request = ENV["BITRISE_PULL_REQUEST"]

    # Move the changes over to the pull request branch
#     run_command("git stash")
    run_command("git fetch --all")
    puts run_command("git checkout -b #{branch} origin/#{branch}")
#     puts run_command("git stash apply")

    # Commit new screenshots
    run_command("git add .")
    run_command("git commit -m \"Update screenshots\"")
    run_command("git push")

    # Add comment
    puts "Adding comment to pull request #{pull_request}"
    client.add_comment(
        "stripe/stripe-android",
        pull_request,
        "Automatically committed the updated screenshots."
    )
else
    puts "Couldn't log in to GitHub"
end

exit false
