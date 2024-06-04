#!/usr/bin/env ruby

require 'octokit'
require 'fileutils'
require 'erb'
require 'colorize'
require 'optparse'
require 'open3'

def prompt_for_new_version
  puts "What's the new version number?"
  version = gets.chomp
end

def validate_version_number(version_number)
  part_names = ['major', 'minor', 'patch']
  parts = version_number.split('.')

  unless parts.length() == 3
    abort("Invalid version number. It should consists of a major, minor, and patch number.")
  end

  parts.each_with_index do | part, index |
    if part.start_with?('0') && part.length() > 1
      part_name = part_names[index]
      abort("Invalid version number: #{part_name} number can\'t begin with 0.")
    end
  end
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

def update_changelog(version)
  done = false
  final_lines = []

  File.foreach('CHANGELOG.md') do |line|
    is_line = line.include? "XX.XX.XX"
    final_lines.append(line)

    date = Time.now.strftime("%Y-%m-%d")

    if is_line
        final_lines.append("\n")
        final_lines.append("## #{version} - #{date}\n")
    end
  end

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

def execute_or_fail(command)
    system(command) or raise "Failed to execute #{command}"
end

def pull_current_master
  execute_or_fail("git checkout master")
  execute_or_fail("git pull")
end

def update_changelog_on_new_branch(branch_name, version_number)
  execute_or_fail("git checkout -b #{branch_name}")
  update_changelog(version_number)
end

def push_changes(commit_title)
  execute_or_fail("git add CHANGELOG.md")
  execute_or_fail("git commit -m \"#{commit_title}\"")
  execute_or_fail("git push -u origin")
end

def create_pull_request_body(version_number)
    template_file_path = File.join(File.dirname(__FILE__), '../.github/PULL_REQUEST_TEMPLATE.md')
    template_file = File.open(template_file_path)
    template = template_file.read
    template_file.close

    summary = <<~EOS
    This pull request adds the release notes for version #{version_number}.

    - [ ] Update README (version number is done by bindings)
    - [ ] Update CHANGELOG with any new features or breaking changes (be thorough when reviewing commit history)
    - [ ] Update MIGRATING (if necessary)
    EOS

    template["<!-- Simple summary of what was changed. -->"] = summary
    template
end

def open_pull_request(branch_name, title, body)
    github_login.create_pull_request(
      "stripe/stripe-android",
      "master",
      branch_name,
      title,
      body
    )
end

# Script start

ensure_clean_repo

new_version_number = prompt_for_new_version
validate_version_number(new_version_number)

new_branch_name = "release/#{new_version_number}"
action_title = "Update release notes for #{new_version_number}"

# Do changes
pull_current_master
update_changelog_on_new_branch(new_branch_name, new_version_number)
push_changes(action_title)

# Make pull request
pull_request_body = create_pull_request_body(new_version_number)
response = open_pull_request(new_branch_name, action_title, pull_request_body)
open_url(response.html_url)
