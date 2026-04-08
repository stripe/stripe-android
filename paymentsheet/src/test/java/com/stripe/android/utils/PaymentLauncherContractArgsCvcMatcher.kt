package com.stripe.android.utils

import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.payments.paymentlauncher.PaymentLauncherContract
import org.hamcrest.BaseMatcher
import org.hamcrest.Description

class PaymentLauncherContractArgsCvcMatcher(
    private val cvc: String? = null
) : BaseMatcher<PaymentLauncherContract.Args>() {
    override fun matches(item: Any?): Boolean {
        if (item !is PaymentLauncherContract.Args) {
            return false
        }

        return when (item) {
            is PaymentLauncherContract.Args.HashedPaymentIntentNextActionArgs -> cvc == null
            is PaymentLauncherContract.Args.IntentConfirmationArgs ->
                matchesCvcInIntentParams(item.confirmStripeIntentParams)
            is PaymentLauncherContract.Args.PaymentIntentNextActionArgs -> cvc == null
            is PaymentLauncherContract.Args.SetupIntentNextActionArgs -> cvc == null
            is PaymentLauncherContract.Args.StripeIntentNextActionWithIntentArgs -> cvc == null
        }
    }

    private fun matchesCvcInIntentParams(confirmStripeIntentParams: ConfirmStripeIntentParams): Boolean {
        return when (confirmStripeIntentParams) {
            is ConfirmPaymentIntentParams -> {
                matchesCvcInPaymentOptions(confirmStripeIntentParams.paymentMethodOptions)
            }
            is ConfirmSetupIntentParams -> {
                matchesCvcInPaymentOptions(confirmStripeIntentParams.paymentMethodOptions)
            }
        }
    }

    private fun matchesCvcInPaymentOptions(paymentMethodOptionsParams: PaymentMethodOptionsParams?): Boolean {
        return when (paymentMethodOptionsParams) {
            is PaymentMethodOptionsParams.Card -> cvc == paymentMethodOptionsParams.cvc
            else -> false
        }
    }

    override fun describeTo(description: Description?) {
        description?.appendText(
            "PaymentLauncherContract.Args.IntentConfirmationArgs" +
                ".confirmStripeIntentParams.paymentMethodOptions.cvc" +
                " matching: "
        )
    }
}
