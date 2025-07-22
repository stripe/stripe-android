package com.stripe.android.model

import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class CryptoCustomerRequestParams internal constructor(
    val consumerSessionClientSecret: String
) : StripeParamsModel {

    override fun toParamMap(): Map<String, Any> {
        return mapOf(
            PARAM_CREDENTIALS to mapOf(PARAM_CONSUMER_SESSION_CLIENT_SECRET to consumerSessionClientSecret),
        )
    }

    companion object {
        private const val PARAM_CONSUMER_SESSION_CLIENT_SECRET = "consumer_session_client_secret"
        private const val PARAM_CREDENTIALS = "credentials"
    }
}
