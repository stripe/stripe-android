package com.stripe.android.payments.core

import android.app.Activity
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.payments.core.authentication.IntentAuthenticator
import com.stripe.android.view.AuthActivityStarter

/**
 * A host of [ActivityResultLauncher] which is used to start another [Activity] through
 * [AuthActivityStarter].
 *
 * Gets notified when [ActivityResultLauncher] needs to be recreated or invalidated due to
 * host Activity recreation.
 */
internal interface ActivityResultLauncherHost {
    /**
     * Notify the [IntentAuthenticator] that a new [ActivityResultCaller] and
     * [ActivityResultCallback] is available. This happens when the host Activity is recreated and
     * its [ActivityResultLauncher] needs to be re-registered.
     */
    fun onNewActivityResultCaller(
        activityResultCaller: ActivityResultCaller,
        activityResultCallback: ActivityResultCallback<PaymentFlowResult.Unvalidated>
    ) {
    }

    /**
     * Notify the [ActivityResultLauncher]s are invalid. This happens when the host [Activity] is
     * destroyed.
     */
    fun onLauncherInvalidated() {}
}
