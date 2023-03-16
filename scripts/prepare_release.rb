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

def open_url(url)
  `open '#{url}'`
end

# Script start

ensure_clean_repo

new_version_number = prompt_for_new_version
validate_version_number(new_version_number)

new_branch_name = "release/#{new_version_number}"
action_title = "Update release notes for #{new_version_number}"

# Pull the current state
system("git checkout master")
system("git pull")

# Update the changelog on a new branch
system("git checkout -b #{new_branch_name}")
update_changelog(new_version_number)

# Push the new branch
system("git add CHANGELOG.md")
system("git commit -m \"#{action_title}\"")
system("git push -u origin")

# Open the pull request creation page
compare_url = "https://github.com/stripe/stripe-android/compare/master...#{new_branch_name}?expand=1"
open_url(compare_url)
