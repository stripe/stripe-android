package com.stripe.android.link.repositories

import com.stripe.android.link.TestFactory
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerPaymentDetailsUpdateParams
import com.stripe.android.model.ConsumerSessionSignup
import com.stripe.android.model.ConsumerSignUpConsentAction
import com.stripe.android.model.EmailSource
import com.stripe.android.model.IncentiveEligibilitySession
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.StripeIntent

internal open class FakeLinkRepository : LinkRepository {
    var lookupConsumerResult = Result.success(TestFactory.CONSUMER_SESSION_LOOKUP)
    var mobileLookupConsumerResult = Result.success(TestFactory.CONSUMER_SESSION_LOOKUP)
    var consumerSignUpResult = Result.success(TestFactory.CONSUMER_SESSION_SIGN_UP)
    var createCardPaymentDetailsResult = Result.success(TestFactory.LINK_NEW_PAYMENT_DETAILS)
    var shareCardPaymentDetailsResult = Result.success(TestFactory.LINK_NEW_PAYMENT_DETAILS)
    var logOutResult = Result.success(TestFactory.CONSUMER_SESSION)
    var startVerificationResult = Result.success(TestFactory.CONSUMER_SESSION)
    var confirmVerificationResult = Result.success(TestFactory.CONSUMER_SESSION)
    var listPaymentDetailsResult = Result.success(TestFactory.CONSUMER_PAYMENT_DETAILS)
    var updatePaymentDetailsResult = Result.success(TestFactory.CONSUMER_PAYMENT_DETAILS)
    var deletePaymentDetailsResult = Result.success(Unit)

    override suspend fun lookupConsumer(email: String) = lookupConsumerResult

    override suspend fun mobileLookupConsumer(
        email: String,
        emailSource: EmailSource,
        verificationToken: String,
        appId: String,
        sessionId: String
    ) = mobileLookupConsumerResult

    override suspend fun consumerSignUp(
        email: String,
        phone: String,
        country: String,
        name: String?,
        consentAction: ConsumerSignUpConsentAction
    ) = consumerSignUpResult

    override suspend fun mobileSignUp(
        name: String?,
        email: String,
        phoneNumber: String,
        country: String,
        consentAction: ConsumerSignUpConsentAction,
        amount: Long?,
        currency: String?,
        incentiveEligibilitySession: IncentiveEligibilitySession?,
        verificationToken: String,
        appId: String
    ): Result<ConsumerSessionSignup> {
        TODO("Not yet implemented")
    }

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

    override suspend fun listPaymentDetails(
        paymentMethodTypes: Set<String>,
        consumerSessionClientSecret: String,
        consumerPublishableKey: String?
    ): Result<ConsumerPaymentDetails> = listPaymentDetailsResult

    override suspend fun deletePaymentDetails(
        paymentDetailsId: String,
        consumerSessionClientSecret: String,
        consumerPublishableKey: String?
    ): Result<Unit> = deletePaymentDetailsResult

    override suspend fun updatePaymentDetails(
        updateParams: ConsumerPaymentDetailsUpdateParams,
        consumerSessionClientSecret: String,
        consumerPublishableKey: String?
    ): Result<ConsumerPaymentDetails> = updatePaymentDetailsResult
}
