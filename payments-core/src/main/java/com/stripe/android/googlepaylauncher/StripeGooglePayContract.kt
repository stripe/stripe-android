package com.stripe.android.googlepaylauncher

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.ColorInt
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.stripe.android.R
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parcelize

internal class StripeGooglePayContract :
    ActivityResultContract<StripeGooglePayContract.Args, GooglePayLauncherResult>() {

    override fun createIntent(
        context: Context,
        input: Args?
    ): Intent {
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

internal fun GooglePayLauncherResult.Error.getErrorResourceID(): Int? {
    return when (googlePayStatus) {
        Status.RESULT_SUCCESS -> null
        Status.RESULT_INTERNAL_ERROR, Status.RESULT_CANCELED, Status.RESULT_DEAD_CLIENT, null ->
            R.string.stripe_google_pay_error_internal
        else -> {
            when (googlePayStatus.statusCode) {
                CommonStatusCodes.API_NOT_CONNECTED, // "The client attempted to call a method from an API that failed to connect."
                CommonStatusCodes.CANCELED, // -> "The result was canceled either due to client disconnect or PendingResult.cancel()."
                CommonStatusCodes.DEVELOPER_ERROR, // -> "The application is misconfigured."
                CommonStatusCodes.ERROR, // -> "The operation failed with no more detailed information."
                CommonStatusCodes.INTERRUPTED, // -> "A blocking call was interrupted while waiting and did not run to completion."
                CommonStatusCodes.INVALID_ACCOUNT, // -> "The client attempted to connect to the service with an invalid account name specified."
                CommonStatusCodes.SERVICE_DISABLED, // -> "This constant is deprecated. This case handled during connection, not during API requests. No results should be returned with this status code."
                CommonStatusCodes.SERVICE_VERSION_UPDATE_REQUIRED, // -> " This constant is deprecated.This case handled during connection, not during API requests . No results should be returned with this status code."
                CommonStatusCodes.SUCCESS, // -> "The operation was successful.SUCCESS_CACHE The operation was successful, but was used the device's cache."
                CommonStatusCodes.RESOLUTION_REQUIRED, // -> "Completing the operation requires some form of resolution."
                CommonStatusCodes.INTERNAL_ERROR -> R.string.stripe_google_pay_error_internal
                CommonStatusCodes.NETWORK_ERROR -> R.string.stripe_failure_connection_error
                CommonStatusCodes.SIGN_IN_REQUIRED -> R.string.stripe_failure_reason_authentication
                CommonStatusCodes.TIMEOUT -> R.string.stripe_failure_reason_timed_out
                else -> {
                    R.string.stripe_google_pay_error_internal
                }
            }
        }
    }
}
