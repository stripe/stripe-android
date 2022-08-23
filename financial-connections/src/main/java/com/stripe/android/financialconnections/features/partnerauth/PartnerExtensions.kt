package com.stripe.android.financialconnections.features.partnerauth

import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.FinancialConnectionsAuthorizationSession.Flow

internal fun Flow.isOAuth(): Boolean = when (this) {
    Flow.DIRECT,
    Flow.DIRECT_WEBVIEW,
    Flow.FINICITY_CONNECT_V2_OAUTH,
    Flow.FINICITY_CONNECT_V2_OAUTH_REDIRECT,
    Flow.FINICITY_CONNECT_V2_OAUTH_WEBVIEW,
    Flow.MX_OAUTH,
    Flow.MX_OAUTH_REDIRECT,
    Flow.MX_OAUTH_WEBVIEW,
    Flow.TRUELAYER_OAUTH,
    Flow.TRUELAYER_OAUTH_HANDOFF,
    Flow.TRUELAYER_OAUTH_WEBVIEW,
    Flow.WELLS_FARGO,
    Flow.WELLS_FARGO_WEBVIEW,
    Flow.TESTMODE_OAUTH,
    Flow.TESTMODE_OAUTH_WEBVIEW -> true
    Flow.FINICITY_CONNECT_V2_LITE,
    Flow.FINICITY_CONNECT_V2_FIX,
    Flow.MX_CONNECT,
    Flow.TESTMODE -> false
}
