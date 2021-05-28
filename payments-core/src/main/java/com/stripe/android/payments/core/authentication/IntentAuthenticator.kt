package com.stripe.android.payments.core.authentication

import android.app.Activity
import androidx.fragment.app.Fragment
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.ApiRequest
import com.stripe.android.view.AuthActivityStarter

/**
 * A unit to authenticate a [StripeIntent] base on its next_action.
 */
internal interface IntentAuthenticator {

    /**
     * Authenticates an [StripeIntent] based on its next_action
     *
     * @param host the host([Activity] or [Fragment]) where client is calling from, used to redirect back to client.
     * @param stripeIntent the intent to authenticate
     * @param threeDs1ReturnUrl a dedicated deeplink URL to return to only for 3ds1 web authentication. TODO(ccen): move it to [WebIntentAuthenticator]
     * @param requestOptions configurations for the API request which triggers the authentication
     */
    suspend fun authenticate(
        host: AuthActivityStarter.Host,
        stripeIntent: StripeIntent,
        threeDs1ReturnUrl: String?,
        requestOptions: ApiRequest.Options
    )
}
