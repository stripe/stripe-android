package com.stripe.android.googlepaylauncher

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.ColorInt
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parcelize

internal class StripeGooglePayContract :
    ActivityResultContract<StripeGooglePayContract.Args, GooglePayLauncherResult>() {

    override fun createIntent(context: Context, input: Args): Intent {
        return Intent(context, StripeGooglePayActivity::class.java)
            .putExtra(ActivityStarter.Args.EXTRA, input)
    }

    override fun parseResult(
        resultCode: Int,
        intent: Intent?
    ): GooglePayLauncherResult {
        return GooglePayLauncherResult.fromIntent(intent)
    }

    /**
     * Args to start [StripeGooglePayActivity] and collect payment data. If successful, the
     * result will be returned through [GooglePayLauncherResult.PaymentData].
     */
    @Parcelize
    data class Args(
        var config: GooglePayConfig,
        @ColorInt val statusBarColor: Int?
    ) : ActivityStarter.Args {

        companion object {
            @JvmSynthetic
            internal fun create(intent: Intent): Args? {
                return intent.getParcelableExtra(ActivityStarter.Args.EXTRA)
            }
        }
    }
}
