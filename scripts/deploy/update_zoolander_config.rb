#!/usr/bin/env ruby

require 'subprocess'
require 'yaml'

require_relative 'common'

def update_zoolander_config()
    if (@is_older_version)
        rputs "Skipping updating zoolander config because this release is for an older version."
        return
    end

    puts 'Ensuring zoolander repo is up-to-date.'
    begin
        execute_or_fail("git -C ../zoolander checkout master")
        execute_or_fail("git -C ../zoolander pull")

        puts '> Updating bindings_version in zoolander for latest release.'
        config_file = "../zoolander/src/resources/com/stripe/dscore/analyticseventlogger/server/rpcserver/client_config.yaml"

        # Read and parse the YAML file
        config = YAML.load_file(config_file)

        # Find the stripe-mobile-payments-sdk-legacy client
        clients = config['client_configs'] || []
        mobile_sdk_client = clients.find { |client| client['client_id'] == 'stripe-mobile-payments-sdk-legacy' }

        if mobile_sdk_client.nil?
            raise "Could not find stripe-mobile-payments-sdk-legacy client in config"
        end

        # Check if version is already in bindings_version list
        metrics_tags = mobile_sdk_client['metrics_tags']
        bindings_version_yaml = metrics_tags.find{ |metric_tag| metric_tag['name'] == 'bindings_version'}
        bindings_versions = bindings_version_yaml['values']

        if bindings_versions.include?(@version)
            rputs "Version #{@version} is already in the bindings_version list. No changes needed."
            return
        end

        # Add version to the top of the list
        bindings_versions.unshift(@version)
        bindings_version_yaml['values'] = bindings_versions

        # Write the updated YAML back to file
        File.write(config_file, YAML.dump(config))

        execute_or_fail("git -C ../zoolander add src/resources/com/stripe/dscore/analyticseventlogger/server/rpcserver/client_config.yaml")
        switch_to_new_branch(zoolander_branch, "master", repo: "../zoolander")
        execute_or_fail("git -C ../zoolander add src/resources/com/stripe/dscore/analyticseventlogger/server/rpcserver/client_config.yaml")
        execute_or_fail("git -C ../zoolander commit -m \"Update Android Payments SDK bindings_version to #{@version}\"")
    rescue
        execute("git -C ../zoolander restore src")
        raise
    end

    begin
        execute_or_fail("git -C ../zoolander push -u origin")
    rescue
        delete_zoolander_branch
        raise
    end

    if (@is_dry_run)
        rputs "At the opened link, verify that the stripe-android version was added to the bindings_version list in the client_config.yaml file."
    else
        rputs "Use the opened link to open a PR and request review. Ensure this gets merged, but continue with the next steps while waiting for review."
    end

    open_url("https://git.corp.stripe.com/stripe-internal/zoolander/compare/#{zoolander_branch}")
    wait_for_user
end

def delete_zoolander_branch
    delete_git_branch(zoolander_branch, "master", repo: "../zoolander")
end

private def zoolander_branch
    "release-android-payments-sdk-#{@version}"
end
