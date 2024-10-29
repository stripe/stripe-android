package com.stripe.android.link.repositories

import com.stripe.android.link.TestFactory
import com.stripe.android.model.ConsumerSignUpConsentAction
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.StripeIntent

open class FakeLinkRepository : LinkRepository {
    var lookupConsumerResult = Result.success(TestFactory.CONSUMER_SESSION_LOOKUP)
    var consumerSignUpResult = Result.success(TestFactory.CONSUMER_SESSION_SIGN_UP)
    var createCardPaymentDetailsResult = Result.success(TestFactory.LINK_NEW_PAYMENT_DETAILS)
    var shareCardPaymentDetailsResult = Result.success(TestFactory.LINK_NEW_PAYMENT_DETAILS)
    var logOutResult = Result.success(TestFactory.CONSUMER_SESSION)
    var startVerificationResult = Result.success(TestFactory.CONSUMER_SESSION)
    var confirmVerificationResult = Result.success(TestFactory.CONSUMER_SESSION)

    override suspend fun lookupConsumer(email: String) = lookupConsumerResult

    override suspend fun consumerSignUp(
        email: String,
        phone: String,
        country: String,
        name: String?,
        consentAction: ConsumerSignUpConsentAction
    ) = consumerSignUpResult

    override suspend fun createCardPaymentDetails(
        paymentMethodCreateParams: PaymentMethodCreateParams,
        userEmail: String,
        stripeIntent: StripeIntent,
        consumerSessionClientSecret: String,
        consumerPublishableKey: String?,
        active: Boolean
    ) = createCardPaymentDetailsResult

    override suspend fun shareCardPaymentDetails(
        paymentMethodCreateParams: PaymentMethodCreateParams,
        id: String,
        last4: String,
        consumerSessionClientSecret: String
    ) = shareCardPaymentDetailsResult

    override suspend fun logOut(
        consumerSessionClientSecret: String,
        consumerAccountPublishableKey: String?
    ) = logOutResult

    override suspend fun startVerification(
        consumerSessionClientSecret: String,
        consumerPublishableKey: String?
    ) = startVerificationResult

    override suspend fun confirmVerification(
        verificationCode: String,
        consumerSessionClientSecret: String,
        consumerPublishableKey: String?
    ) = confirmVerificationResult
}
