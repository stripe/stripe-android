#!/usr/bin/env ruby

require 'subprocess'

require_relative 'common'

def update_pay_server_docs()
    puts 'Ensuring pay-server repo is up-to-date.'
    execute_or_fail("cd ../pay-server")
    begin
        execute_or_fail("git checkout master")
        execute_or_fail("git pull")

        puts '> Updating android SDK version in pay-server for latest release.'
        replace_in_file("../pay-server/docs/content/constants.yaml",
          /sdk-version: [.\d]+/,
          "sdk-version: #{@version}",
        )
        switch_to_new_branch(pay_server_branch)
        execute_or_fail("git add docs/content/constants.yaml")
        execute_or_fail("git commit -m \"Update Android Payments SDK version to #{@version}\"")
    rescue
        execute("git restore docs")
        execute("cd ../stripe-android")
        raise
    end

    begin
        execute_or_fail("git push -u origin")
    rescue
        delete_pay_server_branch
        raise
    end

    if (@is_dry_run)
        rputs "At the opened link, verify that the stripe-android version was updated within the pay-server constants.yaml file."
    else
        rputs "Use the opened link to open a PR and request review. Ensure this gets merged, but continue with the next steps while waiting for review."
    end

    open_url("https://git.corp.stripe.com/stripe-internal/pay-server/compare/#{pay_server_branch}")
    wait_for_user

    # Return to master + stripe-android
    execute("git checkout master")
    execute("cd ../stripe-android")
end

def delete_pay_server_branch
    execute("cd ../pay-server")
    delete_git_branch(pay_server_branch, "master")
end

private def pay_server_branch
    "release-android-payments-sdk-#{@version}"
end
