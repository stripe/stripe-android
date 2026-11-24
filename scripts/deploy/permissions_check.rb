#!/usr/bin/env ruby

require 'colorize'

require_relative 'common'

@required_passwords = [
    {
        "password_key" => "bindings/gnupg/pubkey" ,
        "error_actions" => -> {
            request_membership_at_link("https://ldapmanager.corp.stripe.com/request?group=password-bindings-gnupg-pubkey")
        }
    },
    {
        "password_key" => "bindings/gnupg/privkey",
        "error_actions" => -> { get_password_from_coworker() }
    },
    {
        "password_key" => "bindings/gnupg/passphrase",
        "error_actions" => -> { get_password_from_coworker() }
    },
    {
        "password_key" => "bindings/sonatype-central-api-token",
        "error_actions" => -> {
            request_membership_at_link("https://ldapmanager.corp.stripe.com/request?group=password-bindings-sonatype-central-api-token")
        }
    },
    {
        "password_key" => "bindings/gnupg/fingerprint",
        "error_actions" => -> {
            request_membership_at_link("https://ldapmanager.corp.stripe.com/request?group=password-bindings-gnupg-fingerprint")
        }
    },
]

private def request_membership_at_link(link)
    rputs "Request membership at the opened link"
    open_url(link)
end

private def get_password_from_coworker()
    rputs "Ask a coworker to share this password with you by running this command:"
    rputs "bundle install && bundle exec ./scripts/deploy/add_new_user --user <your user name>"
end

private def check_github_access
    github_api("repos/#{GITHUB_REPO}")
    puts "Successfully reached #{GITHUB_REPO} on #{GITHUB_HOST}!".green
rescue StandardError => e
    rputs "Failed to reach #{GITHUB_REPO} on #{GITHUB_HOST} with the GitHub CLI: #{e.message}"
    rputs "Run `gh auth login --hostname #{GITHUB_HOST}`, and make sure you have write access to #{GITHUB_REPO_URL}."
    wait_for_user
end

def check_permissions
    check_github_access

    @required_passwords.each do |required_password|
        password_key = required_password["password_key"]
        begin
            password = fetch_password(required_password["password_key"])
            puts "Successfully retrieved password! #{password_key}".green
        rescue
            rputs "Failed to retrieve password: #{password_key}"
            required_password["error_actions"].call
            wait_for_user
        end
    end
end
