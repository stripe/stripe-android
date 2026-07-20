package com.stripe.android.payments.core.authentication

import android.app.Activity
import androidx.annotation.RestrictTo
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.withResumed
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.Source
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.core.ActivityResultLauncherHost
import com.stripe.android.view.AuthActivityStarterHost
import kotlinx.coroutines.CompletableDeferred

/**
 * A class to authenticate a [StripeIntent] or [Source].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
abstract class PaymentNextActionHandler<Actionable> : ActivityResultLauncherHost {

    /**
     * Authenticates a [StripeIntent] or [Source].
     *
     * @param host the host([Activity] or [Fragment]) where client is calling from, used to redirect back to client
     * @param actionable the [StripeIntent] or [Source] object to perform next action on (e.g authenticate)
     * @param requestOptions configurations for the API request which triggers the authentication
     * @param returnUrl the confirm-time resolved return URL (merchant custom value if set, else the
     * SDK default), or `null` if no confirm-time value is available (e.g. manual next-action handling
     * or Source authentication). Used by web-based next actions (e.g. Alipay) to know which URL the
     * redirect will ultimately land on.
     */
    suspend fun performNextAction(
        host: AuthActivityStarterHost,
        actionable: Actionable,
        requestOptions: ApiRequest.Options,
        returnUrl: String?
    ) {
        val lifecycleOwner = host.lifecycleOwner

        lifecycleOwner.awaitResumed()
        performNextActionOnResumed(host, actionable, requestOptions, returnUrl)
    }

    protected abstract suspend fun performNextActionOnResumed(
        host: AuthActivityStarterHost,
        actionable: Actionable,
        requestOptions: ApiRequest.Options,
        returnUrl: String?
    )
}

private suspend fun LifecycleOwner.awaitResumed() {
    val completable = CompletableDeferred<Unit>()
    withResumed { completable.complete(Unit) }
    completable.await()
}
