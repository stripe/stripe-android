#!/usr/bin/env ruby

require 'octokit'
require 'fileutils'
require 'erb'
require 'colorize'
require 'optparse'

def update_changelog(version)
  done = false
  final_lines = []

  File.foreach('CHANGELOG.md') { |line|
    is_line = line.include? "XX.XX.XX"
    final_lines.append(line)

    date = Time.now.strftime("%Y-%m-%d")

    if is_line
        final_lines.append("\n")
        final_lines.append("## #{version} - #{date}\n")
    end
  }

  File.write('CHANGELOG.md', final_lines.join(""))
end

def github_login
  token = `fetch-password -q bindings/gh-tokens/$USER`
  if $?.exitstatus != 0
    puts "Couldn't fetch GitHub token. Follow the Android SDK Deploy Guide (https://go/android-sdk-deploy) to set up a token. \a".red
    exit(1)
  end
  client = Octokit::Client.new(access_token: token)
  abort('Invalid GitHub token. Follow the wiki instructions for setting up a GitHub token.') unless client.login
  client
end

def open_url(url)
  `open '#{url}'`
end

# Script start

if ARGV.length != 1
    abort('Provide the new version number as an argument.')
end

new_version_number = ARGV[0]
new_branch_name = "release/#{new_version_number}"
action_title = "Update release notes for #{new_version_number}"

# Pull the current state
system("git checkout master")
system("git pull")

# Update the changelog on a new branch
system("git checkout -b #{new_branch_name}")
update_changelog(new_version_number)

# Push the new branch
system("git add .")
system("git commit -m \"#{action_title}\"")
system("git push -u origin")

# Open the pull request creation page
compare_url = "https://github.com/stripe/stripe-android/compare/master...#{new_branch_name}?expand=1"
open_url(compare_url)
