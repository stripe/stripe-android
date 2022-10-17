package com.stripe.android.payments.core.authentication

import android.app.Activity
import androidx.annotation.RestrictTo
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenResumed
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.Source
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.core.ActivityResultLauncherHost
import com.stripe.android.view.AuthActivityStarterHost
import kotlinx.coroutines.launch

/**
 * A class to authenticate a [StripeIntent] or [Source].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
abstract class PaymentAuthenticator<Authenticatable> : ActivityResultLauncherHost {

    /**
     * Authenticates a [StripeIntent] or [Source].
     *
     * @param host the host([Activity] or [Fragment]) where client is calling from, used to redirect back to client
     * @param authenticatable the [StripeIntent] or [Source] object to authenticate
     * @param requestOptions configurations for the API request which triggers the authentication
     */
    suspend fun authenticate(
        host: AuthActivityStarterHost,
        authenticatable: Authenticatable,
        requestOptions: ApiRequest.Options
    ) {
        val lifecycleOwner = host.lifecycleOwner
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.whenResumed {
                performAuthentication(host, authenticatable, requestOptions)
            }
        }
    }

    protected abstract suspend fun performAuthentication(
        host: AuthActivityStarterHost,
        authenticatable: Authenticatable,
        requestOptions: ApiRequest.Options
    )
}
