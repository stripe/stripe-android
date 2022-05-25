#!/usr/bin/env ruby

require 'octokit'
require 'fileutils'
require 'erb'
require 'colorize'
require 'optparse'

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

def get_current_release_version_of_repo(repo)
  begin
    latest_version = Octokit.latest_release(repo)
    latest_version.tag_name
  rescue
    raise "No releases found."
  end
end

def changelog(version)
  changelog = ''
  reading = false
  # Get changelog for version from CHANGELOG
  File.foreach('CHANGELOG.md') do |line|
    # If the line starts with ##, we've reached the end of the entry
    break if reading && line.start_with?('## ')

    # If the line starts with ## and the version, start reading the entry
    reading = true if line.start_with?('## ') && line.include?(version)
    changelog += line if reading
  end
  changelog
end

def wait_for_enter
  prompt_user "Press enter to continue..."
end

def open_url(url)
  `open '#{url}'`
end

def prompt_user(prompt)
  puts "#{prompt} \a".red
  STDIN.gets
end

OptionParser.new do |opts|
  opts.banner = "Release scripts\n Usage: propose_release.rb [options]"

  opts.on("--version VERSION",
    "Version to release (e.g. 21.2.0)") do |t|
    @specified_version = t
  end
end.parse!

prompt_user "Ensure you have merged your README, CHANGELOG, and MIGRATING changes to master and then press enter"
`git checkout master && git pull`

github_login()

last_release = get_current_release_version_of_repo('stripe/stripe-android')

version = @specified_version

changelog = changelog(version)

# Print the latest commit hash
last_commit = `git log --pretty=format:'%h (%cd)' --date=format:'%Y-%m-%d' -n 1`
# Print the first commit hash from the last release
first_commit = `git log --pretty=format:'%h (%cd)' --date=format:'%Y-%m-%d' -n 1 #{last_release}`

# File a JIRA ticket under CHANGEDOC with the following fields:
jiradescription = %{
h3. Summary of the change

{code}
#{changelog}
{code}

h3. Commit range the change includes
First commit: #{first_commit}
Last commit: #{last_commit}

h3. Detailed rollout instructions
https://confluence.corp.stripe.com/display/MOBILE/Android+SDK+Deployment+Guide

h3. Additional testing details
<Add any additional testing details or leave "n/a">
}

currentUser = ENV['USER']
jira_url = "https://jira.corp.stripe.com/secure/CreateIssueDetails!init.jspa?pid=10712&issuetype=10100&reporter=#{ERB::Util.url_encode(currentUser)}&summary=Android+SDK+Release+#{ERB::Util.url_encode(version)}&description=#{ERB::Util.url_encode(jiradescription)}&components=Android+SDK"

puts "File a JIRA ticket..."
puts "You can file one manually at https://go/changedoc with the following information:"
puts "Summary: Android SDK Release #{version}"
puts "Component: Android SDK"
puts jiradescription
puts ""
prompt_user "Or press enter to open pre-filled ticket.\nClick 'Request Review' when you're done."
open_url jira_url
wait_for_enter

puts "Send an email to mobile-sdk-updates@stripe.com with the following information:"
puts "Subject: [Android SDK] #{version}"
puts "Body:"
puts "#{changelog}"
puts ""
prompt_user "Press enter to open pre-filled email in your default client...\n(To make Gmail your default email client, visit https://support.google.com/a/users/answer/9308783)"
open_url "mailto:mobile-sdk-updates@stripe.com?subject=%5BAndroid%20SDK%5D%20#{ERB::Util.url_encode(version)}&body=#{ERB::Util.url_encode(changelog)}"
wait_for_enter

puts "Release proposed. Please contact your deployer.".green
