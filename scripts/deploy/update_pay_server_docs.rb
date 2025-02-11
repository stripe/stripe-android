#!/usr/bin/env ruby

require 'subprocess'

require_relative 'common'

def update_pay_server_docs()
    if (@is_older_version)
        rputs "Skipping updating pay server docs because this release is for an older version."
        return
    end

    puts 'Ensuring pay-server repo is up-to-date.'
    begin
        execute_or_fail("git -C ../pay-server checkout master")
        execute_or_fail("git -C ../pay-server pull")

        puts '> Updating android SDK version in pay-server for latest release.'
        replace_in_file("../pay-server/docs/content/constants.yaml",
          /sdk-version: [.\d]+/,
          "sdk-version: #{@version}",
        )
        execute_or_fail("git -C ../pay-server add docs/content/constants.yaml")
        switch_to_new_branch(pay_server_branch, "master", repo: "../pay-server")
        execute_or_fail("git -C ../pay-server add docs/content/constants.yaml")
        execute_or_fail("git -C ../pay-server commit -m \"Update Android Payments SDK version to #{@version}\"")
    rescue
        execute("git -C ../pay-server restore docs")
        raise
    end

    begin
        execute_or_fail("git -C ../pay-server push -u origin")
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
end

def delete_pay_server_branch
    delete_git_branch(pay_server_branch, "master", repo: "../pay-server")
end

private def pay_server_branch
    "release-android-payments-sdk-#{@version}"
end
