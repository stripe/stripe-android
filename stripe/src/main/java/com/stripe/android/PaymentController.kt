package com.stripe.android

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import com.stripe.android.exception.StripeException
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.Source
import com.stripe.android.model.StripeIntent
import com.stripe.android.view.AuthActivityStarter
import kotlinx.android.parcel.Parcelize

internal interface PaymentController {
    /**
     * Confirm the Stripe Intent and resolve any next actions
     */
    fun startConfirmAndAuth(
        host: AuthActivityStarter.Host,
        confirmStripeIntentParams: ConfirmStripeIntentParams,
        requestOptions: ApiRequest.Options
    )

    fun startAuth(
        host: AuthActivityStarter.Host,
        clientSecret: String,
        requestOptions: ApiRequest.Options
    )

    fun startAuthenticateSource(
        host: AuthActivityStarter.Host,
        source: Source,
        requestOptions: ApiRequest.Options
    )

    /**
     * Decide whether [handlePaymentResult] should be called.
     */
    fun shouldHandlePaymentResult(requestCode: Int, data: Intent?): Boolean

    /**
     * Decide whether [handleSetupResult] should be called.
     */
    fun shouldHandleSetupResult(requestCode: Int, data: Intent?): Boolean

    fun shouldHandleSourceResult(requestCode: Int, data: Intent?): Boolean

    /**
     * If payment authentication triggered an exception, get the exception object and pass to
     * [ApiResultCallback.onError].
     *
     * Otherwise, get the PaymentIntent's client_secret from {@param data} and use to retrieve
     * the PaymentIntent object with updated status.
     *
     * @param data the result Intent
     */
    fun handlePaymentResult(
        data: Intent,
        requestOptions: ApiRequest.Options,
        callback: ApiResultCallback<PaymentIntentResult>
    )

    /**
     * If setup authentication triggered an exception, get the exception object and pass to
     * [ApiResultCallback.onError].
     *
     * Otherwise, get the SetupIntent's client_secret from {@param data} and use to retrieve the
     * SetupIntent object with updated status.
     *
     * @param data the result Intent
     */
    fun handleSetupResult(
        data: Intent,
        requestOptions: ApiRequest.Options,
        callback: ApiResultCallback<SetupIntentResult>
    )

    fun handleSourceResult(
        data: Intent,
        requestOptions: ApiRequest.Options,
        callback: ApiResultCallback<Source>
    )

    /**
     * Determine which authentication mechanism should be used, or bypass authentication
     * if it is not needed.
     */
    fun handleNextAction(
        host: AuthActivityStarter.Host,
        stripeIntent: StripeIntent,
        requestOptions: ApiRequest.Options
    )

    /**
     * Represents the result of a [PaymentController] operation
     */
    @Parcelize
    data class Result internal constructor(
        internal val clientSecret: String? = null,
        @StripeIntentResult.Outcome internal val flowOutcome: Int = StripeIntentResult.Outcome.UNKNOWN,
        internal val exception: StripeException? = null,
        internal val shouldCancelSource: Boolean = false,
        internal val sourceId: String? = null,
        internal val source: Source? = null
    ) : Parcelable {
        @JvmSynthetic
        fun toBundle(): Bundle {
            return Bundle().also {
                it.putParcelable(EXTRA, this)
            }
        }

        internal companion object {
            private const val EXTRA = "extra_args"

            @JvmSynthetic
            internal fun fromIntent(intent: Intent): Result? {
                return intent.getParcelableExtra(EXTRA)
            }
        }
    }
}
