package com.stripe.android.paymentelement.confirmation.cpms

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.RestrictTo
import androidx.core.os.bundleOf
import com.stripe.android.core.exception.LocalStripeException
import com.stripe.android.paymentelement.CustomPaymentMethodResult
import com.stripe.android.paymentelement.ExperimentalCustomPaymentMethodsApi
import kotlinx.parcelize.Parcelize

internal sealed class InternalCustomPaymentMethodResult : Parcelable {
    @Parcelize
    data object Completed : InternalCustomPaymentMethodResult()

    @Parcelize
    class Failed(val throwable: Throwable) : InternalCustomPaymentMethodResult()

    @Parcelize
    data object Canceled : InternalCustomPaymentMethodResult()

    @JvmSynthetic
    fun toBundle(): Bundle = bundleOf(EXTRA_ARGS to this)

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        private const val EXTRA_ARGS = "CUSTOM_PAYMENT_METHOD_RESULT"

        internal fun fromIntent(intent: Intent?): InternalCustomPaymentMethodResult {
            @Suppress("DEPRECATION")
            return intent?.extras?.getParcelable(EXTRA_ARGS)
                ?: Failed(IllegalStateException("Failed to find custom payment method result!"))
        }

        @OptIn(ExperimentalCustomPaymentMethodsApi::class)
        fun fromCustomPaymentMethodResult(result: CustomPaymentMethodResult): InternalCustomPaymentMethodResult {
            return when (result) {
                is CustomPaymentMethodResult.Completed -> Completed
                is CustomPaymentMethodResult.Canceled -> Canceled
                is CustomPaymentMethodResult.Failed -> Failed(
                    throwable = LocalStripeException(
                        displayMessage = result.displayMessage,
                        analyticsValue = "customPaymentMethodFailure"
                    )
                )
            }
        }
    }
}
