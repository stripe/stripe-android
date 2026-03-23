#!/usr/bin/env ruby

require 'subprocess'

require_relative 'common'

def update_pay_server_docs()
    if (@is_older_version)
        rputs "Skipping updating pay server docs because this release is for an older version."
        return
    end

    mint_repo = "../mint"
    constants_file = "pay-server/docs/content/constants.yaml"

    puts 'Ensuring mint repo is up-to-date.'
    begin
        execute_or_fail("git -C #{mint_repo} checkout master")
        execute_or_fail("git -C #{mint_repo} pull")

        puts '> Updating android SDK version in pay-server for latest release.'
        replace_in_file("#{mint_repo}/#{constants_file}",
          /sdk-version: [.\d]+/,
          "sdk-version: #{@version}",
        )
        execute_or_fail("git -C #{mint_repo} add #{constants_file}")
        switch_to_new_branch(pay_server_branch, "master", repo: mint_repo)
        execute_or_fail("git -C #{mint_repo} add #{constants_file}")
        execute_or_fail("git -C #{mint_repo} commit -m \"Update Android Payments SDK version to #{@version}\"")
    rescue
        execute("git -C #{mint_repo} restore #{constants_file}")
        raise
    end

    begin
        execute_or_fail("git -C #{mint_repo} push -u origin")
    rescue
        delete_pay_server_branch
        raise
    end

    if (@is_dry_run)
        rputs "At the opened link, verify that the stripe-android version was updated within the pay-server constants.yaml file."
    else
        rputs "Use the opened link to open a PR and request review. Ensure this gets merged, but continue with the next steps while waiting for review."
    end

    open_url("https://git.corp.stripe.com/stripe-internal/mint/compare/#{pay_server_branch}")
    wait_for_user
end

def delete_pay_server_branch
    delete_git_branch(pay_server_branch, "master", repo: "../mint")
end

private def pay_server_branch
    "release-android-payments-sdk-#{@version}"
end
