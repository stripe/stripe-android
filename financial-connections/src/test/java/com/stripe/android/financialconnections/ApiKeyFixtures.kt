package com.stripe.android.financialconnections

import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest

internal object ApiKeyFixtures {
    const val DEFAULT_PUBLISHABLE_KEY = "pk_test_vOo1umqsYxSrP5UXfOeL3ecm"
    const val DEFAULT_LINK_ACCOUNT_SESSION_SECRET = "las_client_secret_asdf1234"

    const val HOSTED_AUTH_URL = "https://stripe.com/auth/flow/start"
    const val SUCCESS_URL = "stripe-auth://link-accounts/success"
    const val CANCEL_URL = "stripe-auth://link-accounts/cancel"
    val MANIFEST = FinancialConnectionsSessionManifest(HOSTED_AUTH_URL, SUCCESS_URL, CANCEL_URL)
}
