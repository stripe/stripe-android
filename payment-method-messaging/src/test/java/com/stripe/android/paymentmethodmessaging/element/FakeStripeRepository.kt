package com.stripe.android.paymentmethodmessaging.element

import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.PaymentMethodMessage
import com.stripe.android.model.PaymentMethodMessageImage
import com.stripe.android.model.PaymentMethodMessageLearnMore
import com.stripe.android.model.PaymentMethodMessageMultiPartner
import com.stripe.android.model.PaymentMethodMessageSinglePartner
import com.stripe.android.testing.AbsFakeStripeRepository

class FakeStripeRepository : AbsFakeStripeRepository() {
    override suspend fun retrievePaymentMethodMessage(
        paymentMethods: List<String>,
        amount: Int,
        currency: String,
        country: String?,
        locale: String,
        requestOptions: ApiRequest.Options
    ): Result<PaymentMethodMessage> {
        return when {
            amount > 0 -> Result.success(
                PaymentMethodMessage(
                    paymentMethods = paymentMethods,
                    singlePartner = if (paymentMethods.size == 1) singlePartner else null,
                    multiPartner = multiPartner
                )
            )
            amount == 0 -> Result.success(
                PaymentMethodMessage(
                    paymentMethods = listOf(),
                    singlePartner = null,
                    multiPartner = null
                )
            )
            else -> Result.failure(Exception("Price must be non negative"))
        }
    }

    private companion object {
        val image = PaymentMethodMessageImage(
            role = "logo",
            url = "www.test.com",
            paymentMethodType = "klarna",
            text = "howdy"
        )
        val learnMore = PaymentMethodMessageLearnMore(
            message = "learn more",
            url = "www.test.com"
        )
        val singlePartner = PaymentMethodMessageSinglePartner(
            inlinePartnerPromotion = "buy stuff",
            lightImage = image,
            darkImage = image,
            flatImage = image,
            learnMore = learnMore
        )
        val multiPartner = PaymentMethodMessageMultiPartner(
            promotion = "buy stuff",
            lightImages = listOf(image),
            darkImages = listOf(image),
            flatImages = listOf(image),
            learnMore = learnMore
        )
    }
}
