package com.stripe.android

import android.content.Intent
import android.os.Parcel
import android.os.Parcelable
import androidx.core.os.bundleOf
import com.stripe.android.exception.StripeException
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.Source
import com.stripe.android.model.StripeIntent
import com.stripe.android.view.AuthActivityStarter
import kotlinx.android.parcel.Parceler
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

    fun startConfirm(
        confirmStripeIntentParams: ConfirmStripeIntentParams,
        requestOptions: ApiRequest.Options,
        callback: ApiResultCallback<StripeIntent>
    )

    fun startAuth(
        host: AuthActivityStarter.Host,
        clientSecret: String,
        requestOptions: ApiRequest.Options,
        type: StripeIntentType
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
        callback: ApiResultCallback<SetupIntentResult>
    )

    fun handleSourceResult(
        data: Intent,
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

    fun authenticateAlipay(
        intent: StripeIntent,
        stripeAccountId: String?,
        authenticator: AlipayAuthenticator,
        callback: ApiResultCallback<PaymentIntentResult>
    )

    enum class StripeIntentType {
        PaymentIntent,
        SetupIntent
    }

    /**
     * Represents the result of a [PaymentController] operation.
     *
     * This class is annotated with `@Parcelize` but uses custom parceling logic due to issues
     * with parceling an `Exception` subclass. See
     * [Parcel#writeException()](https://developer.android.com/reference/android/os/Parcel#writeException(java.lang.Exception))
     * for more details.
     */
    @Parcelize
    data class Result internal constructor(
        internal val clientSecret: String? = null,
        @StripeIntentResult.Outcome internal val flowOutcome: Int = StripeIntentResult.Outcome.UNKNOWN,
        internal val exception: StripeException? = null,
        internal val shouldCancelSource: Boolean = false,
        internal val sourceId: String? = null,
        internal val source: Source? = null,
        internal val stripeAccountId: String? = null
    ) : Parcelable {
        @JvmSynthetic
        fun toBundle() = bundleOf(EXTRA to this)

        internal companion object : Parceler<Result> {
            override fun create(parcel: Parcel): Result {
                return Result(
                    clientSecret = parcel.readString(),
                    flowOutcome = parcel.readInt(),
                    exception = parcel.readSerializable() as? StripeException?,
                    shouldCancelSource = parcel.readInt() == 1,
                    sourceId = parcel.readString(),
                    source = parcel.readParcelable(Source::class.java.classLoader),
                    stripeAccountId = parcel.readString()
                )
            }

            override fun Result.write(parcel: Parcel, flags: Int) {
                parcel.writeString(clientSecret)
                parcel.writeInt(flowOutcome)
                parcel.writeSerializable(exception)
                parcel.writeInt(1.takeIf { shouldCancelSource } ?: 0)
                parcel.writeString(sourceId)
                parcel.writeParcelable(source, flags)
                parcel.writeString(stripeAccountId)
            }

            private const val EXTRA = "extra_args"

            @JvmSynthetic
            internal fun fromIntent(intent: Intent): Result? {
                return intent.getParcelableExtra(EXTRA)
            }
        }
    }
}
