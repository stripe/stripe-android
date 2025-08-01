package com.stripe.android.link.repositories

import app.cash.turbine.Turbine
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.link.LinkPaymentMethod
import com.stripe.android.link.TestFactory
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerPaymentDetailsUpdateParams
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.model.ConsumerSessionSignup
import com.stripe.android.model.ConsumerShippingAddresses
import com.stripe.android.model.ConsumerSignUpConsentAction
import com.stripe.android.model.EmailSource
import com.stripe.android.model.IncentiveEligibilitySession
import com.stripe.android.model.LinkAccountSession
import com.stripe.android.model.LinkMode
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.StripeIntent

internal open class FakeLinkRepository : LinkRepository {
    var lookupConsumerResult = Result.success(TestFactory.CONSUMER_SESSION_LOOKUP)
    var lookupConsumerWithoutBackendLoggingResult = Result.success(TestFactory.CONSUMER_SESSION_LOOKUP)
    var mobileLookupConsumerResult = Result.success(TestFactory.CONSUMER_SESSION_LOOKUP)
    var consumerSignUpResult = Result.success(TestFactory.CONSUMER_SESSION_SIGN_UP)
    var mobileConsumerSignUpResult = Result.success(TestFactory.CONSUMER_SESSION_SIGN_UP)
    var createLinkAccountSessionResult = Result.success(TestFactory.LINK_ACCOUNT_SESSION)
    var createCardPaymentDetailsResult = Result.success(TestFactory.LINK_NEW_PAYMENT_DETAILS)
    var createBankAccountPaymentDetailsResult = Result.success(TestFactory.CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT)
    var shareCardPaymentDetailsResult = Result.success(TestFactory.LINK_SAVED_PAYMENT_DETAILS)
    var sharePaymentDetails = Result.success(TestFactory.LINK_SHARE_PAYMENT_DETAILS)
    var createPaymentMethod = Result.success(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
    var logOutResult = Result.success(TestFactory.CONSUMER_SESSION)
    var startVerificationResult = Result.success(TestFactory.CONSUMER_SESSION)
    var confirmVerificationResult = Result.success(TestFactory.CONSUMER_SESSION)
    var listPaymentDetailsResult = Result.success(TestFactory.CONSUMER_PAYMENT_DETAILS)
    var listShippingAddressesResult = Result.success(TestFactory.CONSUMER_SHIPPING_ADDRESSES)
    var updatePaymentDetailsResult = Result.success(TestFactory.CONSUMER_PAYMENT_DETAILS)
    var deletePaymentDetailsResult = Result.success(Unit)

    private val lookupConsumerCalls = Turbine<LookupCall>()
    private val lookupConsumerWithoutBackendLoggingCalls = Turbine<LookupCall>()
    private val mobileLookupCalls = Turbine<MobileLookupCall>()
    private val mobileSignUpCalls = Turbine<MobileSignUpCall>()

    override suspend fun lookupConsumer(
        email: String,
        customerId: String?
    ): Result<ConsumerSessionLookup> {
        lookupConsumerCalls.add(
            item = LookupCall(
                email = email
            )
        )
        return lookupConsumerResult
    }

    override suspend fun lookupConsumerWithoutBackendLoggingForExposure(
        email: String
    ): Result<ConsumerSessionLookup> {
        lookupConsumerWithoutBackendLoggingCalls.add(
            item = LookupCall(
                email = email
            )
        )
        return lookupConsumerWithoutBackendLoggingResult
    }

    override suspend fun mobileLookupConsumer(
        email: String,
        emailSource: EmailSource,
        verificationToken: String,
        appId: String,
        sessionId: String,
        customerId: String?
    ): Result<ConsumerSessionLookup> {
        mobileLookupCalls.add(
            item = MobileLookupCall(
                email = email,
                emailSource = emailSource,
                verificationToken = verificationToken,
                appId = appId,
                sessionId = sessionId
            )
        )
        return mobileLookupConsumerResult
    }

    override suspend fun consumerSignUp(
        email: String,
        phone: String?,
        country: String?,
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
        mobileSignUpCalls.add(
            item = MobileSignUpCall(
                name = name,
                email = email,
                phoneNumber = phoneNumber,
                country = country,
                consentAction = consentAction,
                amount = amount,
                currency = currency,
                incentiveEligibilitySession = incentiveEligibilitySession,
                verificationToken = verificationToken,
                appId = appId
            )
        )
        return mobileConsumerSignUpResult
    }

    override suspend fun createCardPaymentDetails(
        paymentMethodCreateParams: PaymentMethodCreateParams,
        userEmail: String,
        stripeIntent: StripeIntent,
        consumerSessionClientSecret: String,
        consumerPublishableKey: String?,
        active: Boolean
    ) = createCardPaymentDetailsResult

    override suspend fun createBankAccountPaymentDetails(
        bankAccountId: String,
        userEmail: String,
        consumerSessionClientSecret: String,
    ) = createBankAccountPaymentDetailsResult

    override suspend fun shareCardPaymentDetails(
        paymentMethodCreateParams: PaymentMethodCreateParams,
        id: String,
        consumerSessionClientSecret: String
    ): Result<LinkPaymentDetails.Saved> = shareCardPaymentDetailsResult

    override suspend fun sharePaymentDetails(
        consumerSessionClientSecret: String,
        paymentDetailsId: String,
        expectedPaymentMethodType: String,
        cvc: String?,
        billingPhone: String?
    ) = sharePaymentDetails

    override suspend fun createPaymentMethod(
        consumerSessionClientSecret: String,
        paymentMethod: LinkPaymentMethod
    ) = createPaymentMethod

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

    override suspend fun listShippingAddresses(
        consumerSessionClientSecret: String,
        consumerPublishableKey: String?
    ): Result<ConsumerShippingAddresses> = listShippingAddressesResult

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

    override suspend fun createLinkAccountSession(
        consumerSessionClientSecret: String,
        stripeIntent: StripeIntent,
        linkMode: LinkMode?,
        consumerPublishableKey: String?
    ): Result<LinkAccountSession> = createLinkAccountSessionResult

    suspend fun awaitMobileLookup(): MobileLookupCall {
        return mobileLookupCalls.awaitItem()
    }

    suspend fun awaitLookup(): LookupCall {
        return lookupConsumerCalls.awaitItem()
    }

    suspend fun awaitLookupWithoutBackendLogging(): LookupCall {
        return lookupConsumerWithoutBackendLoggingCalls.awaitItem()
    }

    suspend fun awaitMobileSignup(): MobileSignUpCall {
        return mobileSignUpCalls.awaitItem()
    }

    fun ensureAllEventsConsumed() {
        mobileSignUpCalls.ensureAllEventsConsumed()
        mobileLookupCalls.ensureAllEventsConsumed()
    }

    data class LookupCall(
        val email: String
    )

    data class MobileLookupCall(
        val email: String,
        val emailSource: EmailSource,
        val verificationToken: String,
        val appId: String,
        val sessionId: String
    )

    data class MobileSignUpCall(
        val name: String?,
        val email: String,
        val phoneNumber: String,
        val country: String,
        val consentAction: ConsumerSignUpConsentAction,
        val amount: Long?,
        val currency: String?,
        val incentiveEligibilitySession: IncentiveEligibilitySession?,
        val verificationToken: String,
        val appId: String
    )
}
