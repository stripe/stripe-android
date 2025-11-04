#!/usr/bin/env ruby

require 'subprocess'
require 'yaml'

require_relative 'common'

def update_zoolander_config()
    if (@is_older_version)
        rputs "Skipping updating zoolander config because this release is for an older version."
        return
    end

    if (@is_dry_run)
        rputs "Verify that the opened link points to client_config.yaml with allowlisted values for the stripe-mobile-payments-sdk-legacy client."
    else
        rputs """
Please update the allowlisted bindings_version values for the stripe-mobile-payments-sdk-legacy at the opened link.
Insert the version in its correct spot in the ordered list. If the version already exists, you don't need to do anything.
        """
        end

    open_url("https://git.corp.stripe.com/stripe-internal/zoolander/blob/master/src/resources/com/stripe/dscore/analyticseventlogger/server/rpcserver/client_config.yaml")
    wait_for_user
end
