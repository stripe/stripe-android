package com.stripe.android.link

import com.stripe.android.core.networking.RequestCredentials
import com.stripe.android.model.AttachConsumerToLinkAccountSession
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerPaymentDetailsCreateParams
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.model.ConsumerSessionRefresh
import com.stripe.android.model.ConsumerSessionSignup
import com.stripe.android.model.CustomEmailType
import com.stripe.android.model.EmailSource
import com.stripe.android.model.LinkAccountSession
import com.stripe.android.model.LinkMode
import com.stripe.android.model.SharePaymentDetails
import com.stripe.android.model.SignUpParams
import com.stripe.android.model.UpdateAvailableIncentives
import com.stripe.android.model.VerificationType
import com.stripe.android.repository.ConsumersApiService
import java.util.Locale

internal open class FakeConsumersApiService : ConsumersApiService {
    var signUpResult = Result.success(TestFactory.CONSUMER_SESSION_SIGN_UP)
    var mobileSignUpResult = Result.success(TestFactory.CONSUMER_SESSION_SIGN_UP)
    var lookupConsumerSessionResult = TestFactory.CONSUMER_SESSION_LOOKUP
    var mobileLookupConsumerSessionResult = TestFactory.CONSUMER_SESSION_LOOKUP
    var refreshConsumerSessionResult = ConsumerSessionRefresh(
        consumerSession = TestFactory.CONSUMER_SESSION,
        linkAuthIntent = null
    )
    var createPaymentDetailsResult = Result.success(
        ConsumerPaymentDetails(paymentDetails = listOf(TestFactory.CONSUMER_PAYMENT_DETAILS_CARD))
    )
    var createPaymentDetailsFromPaymentMethodResult = Result.success(
        ConsumerPaymentDetails(paymentDetails = listOf(TestFactory.CONSUMER_PAYMENT_DETAILS_CARD))
    )

    val signUpCalls = arrayListOf<SignUpCall>()
    val mobileSignUpCalls = arrayListOf<SignUpCall>()
    val lookupCalls = arrayListOf<LookupCall>()
    val mobileLookupCalls = arrayListOf<MobileLookupCall>()
    val sharePaymentDetailsCalls = arrayListOf<SharePaymentDetailsCall>()
    val createPaymentDetailsFromPaymentMethodCalls = arrayListOf<CreatePaymentDetailsFromPaymentMethodCall>()

    override suspend fun signUp(
        params: SignUpParams,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<ConsumerSessionSignup> {
        signUpCalls.add(
            SignUpCall(
                params = params,
                requestOptions = requestOptions
            )
        )
        return signUpResult
    }

    override suspend fun mobileSignUp(
        params: SignUpParams,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<ConsumerSessionSignup> {
        mobileSignUpCalls.add(
            SignUpCall(
                params = params,
                requestOptions = requestOptions
            )
        )
        return mobileSignUpResult
    }

    override suspend fun lookupConsumerSession(
        email: String?,
        linkAuthIntentId: String?,
        requestSurface: String,
        sessionId: String,
        doNotLogConsumerFunnelEvent: Boolean,
        supportedVerificationTypes: List<String>?,
        requestOptions: RequestCredentials,
        customerId: String?,
        idempotencyKey: String?,
    ): ConsumerSessionLookup {
        lookupCalls.add(
            LookupCall(
                email = email,
                linkAuthIntentId = linkAuthIntentId,
                requestOptions = requestOptions,
                requestSurface = requestSurface,
                sessionId = sessionId,
            )
        )
        return lookupConsumerSessionResult
    }

    override suspend fun mobileLookupConsumerSession(
        email: String?,
        emailSource: EmailSource?,
        linkAuthIntentId: String?,
        requestSurface: String,
        verificationToken: String,
        appId: String,
        supportedVerificationTypes: List<String>?,
        requestOptions: RequestCredentials,
        sessionId: String,
        customerId: String?,
        linkAuthTokenClientSecret: String?,
        idempotencyKey: String?,
    ): ConsumerSessionLookup {
        mobileLookupCalls.add(
            MobileLookupCall(
                email = email,
                emailSource = emailSource,
                linkAuthIntentId = linkAuthIntentId,
                requestOptions = requestOptions,
                verificationToken = verificationToken,
                appId = appId,
                requestSurface = requestSurface,
                sessionId = sessionId
            )
        )
        return mobileLookupConsumerSessionResult
    }

    override suspend fun startConsumerVerification(
        consumerSessionClientSecret: String,
        locale: Locale,
        requestSurface: String,
        type: VerificationType,
        customEmailType: CustomEmailType?,
        connectionsMerchantName: String?,
        accountPhoneNumber: String?,
        emailAddress: String?,
        requestOptions: RequestCredentials,
        isResendSmsCode: Boolean,
        idempotencyKey: String?,
    ): ConsumerSession {
        TODO("Not yet implemented")
    }

    override suspend fun confirmConsumerVerification(
        consumerSessionClientSecret: String,
        verificationCode: String,
        requestSurface: String,
        type: VerificationType,
        consentGranted: Boolean?,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): ConsumerSession {
        TODO("Not yet implemented")
    }

    override suspend fun postConsentUpdate(
        consumerSessionClientSecret: String,
        consentGranted: Boolean,
        requestSurface: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun attachLinkConsumerToLinkAccountSession(
        consumerSessionClientSecret: String,
        clientSecret: String,
        requestSurface: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): AttachConsumerToLinkAccountSession {
        TODO("Not yet implemented")
    }

    override suspend fun createPaymentDetails(
        consumerSessionClientSecret: String,
        paymentDetailsCreateParams: ConsumerPaymentDetailsCreateParams,
        requestSurface: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<ConsumerPaymentDetails> {
        return createPaymentDetailsResult
    }

    override suspend fun createPaymentDetails(
        consumerSessionClientSecret: String,
        paymentMethodId: String,
        requestSurface: String,
        requestOptions: RequestCredentials,
        customerEphemeralKey: String,
        idempotencyKey: String?,
    ): Result<ConsumerPaymentDetails> {
        createPaymentDetailsFromPaymentMethodCalls.add(
            CreatePaymentDetailsFromPaymentMethodCall(
                consumerSessionClientSecret = consumerSessionClientSecret,
                paymentMethodId = paymentMethodId,
                requestSurface = requestSurface,
                requestOptions = requestOptions,
                customerEphemeralKey = customerEphemeralKey,
            )
        )
        return createPaymentDetailsFromPaymentMethodResult
    }

    override suspend fun sharePaymentDetails(
        consumerSessionClientSecret: String,
        paymentDetailsId: String,
        expectedPaymentMethodType: String?,
        billingPhone: String?,
        requestSurface: String,
        requestOptions: RequestCredentials,
        extraParams: Map<String, Any?>,
        idempotencyKey: String?,
    ): Result<SharePaymentDetails> {
        sharePaymentDetailsCalls.add(SharePaymentDetailsCall(extraParams, requestOptions))
        return Result.success(TestFactory.LINK_SHARE_PAYMENT_DETAILS)
    }

    override suspend fun updateAvailableIncentives(
        sessionId: String,
        paymentDetailsId: String,
        consumerSessionClientSecret: String,
        requestSurface: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<UpdateAvailableIncentives> {
        TODO("Not yet implemented")
    }

    override suspend fun createLinkAccountSession(
        consumerSessionClientSecret: String,
        intentToken: String?,
        linkMode: LinkMode?,
        requestSurface: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<LinkAccountSession> {
        TODO("Not yet implemented")
    }

    override suspend fun refreshConsumerSession(
        appId: String,
        consumerSessionClientSecret: String,
        supportedVerificationTypes: List<String>?,
        requestSurface: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): ConsumerSessionRefresh = refreshConsumerSessionResult

    override suspend fun updatePhoneNumber(
        consumerSessionClientSecret: String,
        phoneNumber: String,
        requestSurface: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<ConsumerSession> {
        TODO("Not yet implemented")
    }

    data class LookupCall(
        val email: String?,
        val linkAuthIntentId: String?,
        val requestSurface: String,
        val sessionId: String,
        val requestOptions: RequestCredentials
    )

    data class MobileLookupCall(
        val email: String?,
        val emailSource: EmailSource?,
        val linkAuthIntentId: String?,
        val requestSurface: String,
        val verificationToken: String,
        val appId: String,
        val requestOptions: RequestCredentials,
        val sessionId: String
    )

    data class SignUpCall(
        val params: SignUpParams,
        val requestOptions: RequestCredentials
    )

    data class SharePaymentDetailsCall(
        val extraParams: Map<String, Any?>,
        val requestOptions: RequestCredentials
    )

    data class CreatePaymentDetailsFromPaymentMethodCall(
        val consumerSessionClientSecret: String,
        val paymentMethodId: String,
        val requestSurface: String,
        val requestOptions: RequestCredentials,
        val customerEphemeralKey: String,
    )
}
