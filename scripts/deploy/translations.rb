#!/usr/bin/env ruby

require 'octokit'

require_relative 'common'

def validate_translations_merged
    octokit_client.auto_paginate = true
    pull_requests = octokit_client.pull_requests("stripe/stripe-android", state: "open")

    matching_prs = pull_requests.select do |pr|
        pr.title == "Update translations" && pr.user.login == "stripe-android-translations[bot]"
    end

    if matching_prs.empty?
        puts "No open pull requests for updating translations.".green
    else
        puts "Found #{matching_prs.count} pull request(s) for updating translations:"
        matching_prs.each do |pr|
            puts "----------------------------------------"
            puts "  URL:     #{pr.html_url}"
        end
        puts "----------------------------------------"

        raise "Please merge open translation update PRs."
    end
end
