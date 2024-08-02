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
        "password_key" => "bindings/java-maven-api-token",
        "error_actions" => -> {
            request_membership_at_link("https://ldapmanager.corp.stripe.com/request?group=password-bindings-java-maven-api-token")
        }
    },
    {
        "password_key" => "bindings/gnupg/fingerprint",
        "error_actions" => -> {
            request_membership_at_link("https://ldapmanager.corp.stripe.com/request?group=password-bindings-gnupg-fingerprint")
        }
    },
    {
        "password_key" => "bindings/gh-tokens/$USER",
        "error_actions" => -> {
            rputs "Follow the instructions at the opened link to create a github access token."
            open_url("https://go/android-release#first-time-setup")
            wait_for_user
            puts "Enter your access token from github to add it to the vault"
            execute("add-password -n \"$USER\" bindings/gh-tokens/$USER")
        }
    },
    {
        "password_key" => "nexus-sonatype-login",
        "error_actions" => -> {
            rputs "Follow the prompts:"
            execute("fetch-password nexus-sonatype-login")
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

def check_permissions
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
