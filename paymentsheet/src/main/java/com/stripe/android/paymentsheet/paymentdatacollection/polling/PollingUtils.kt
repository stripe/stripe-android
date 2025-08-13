package com.stripe.android.paymentsheet.paymentdatacollection.polling

import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityOptionsCompat
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.uicore.utils.AnimationConstants
import com.stripe.android.view.AuthActivityStarterHost

// TODO: improve name.
internal object PollingUtils {

    fun launchPollingAuthenticator(
        pollingLauncher: ActivityResultLauncher<PollingContract.Args>?,
        host: AuthActivityStarterHost,
        args: PollingContract.Args,
    ) {
        val options = ActivityOptionsCompat.makeCustomAnimation(
            host.application.applicationContext,
            AnimationConstants.FADE_IN,
            AnimationConstants.FADE_OUT,
        )

        if (pollingLauncher == null) {
            ErrorReporter.createFallbackInstance(host.application)
                .report(ErrorReporter.UnexpectedErrorEvent.MISSING_POLLING_AUTHENTICATOR)
        } else {
            pollingLauncher.launch(args, options)
        }
    }

}