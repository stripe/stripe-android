package com.stripe.android.link

import com.stripe.android.core.networking.ApiRequest
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

    val signUpCalls = arrayListOf<SignUpCall>()
    val mobileSignUpCalls = arrayListOf<SignUpCall>()
    val lookupCalls = arrayListOf<LookupCall>()
    val mobileLookupCalls = arrayListOf<MobileLookupCall>()

    override suspend fun signUp(
        params: SignUpParams,
        requestOptions: ApiRequest.Options
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
        requestOptions: ApiRequest.Options
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
        requestOptions: ApiRequest.Options,
        customerId: String?
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
        requestOptions: ApiRequest.Options,
        sessionId: String,
        customerId: String?
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
        requestOptions: ApiRequest.Options
    ): ConsumerSession {
        TODO("Not yet implemented")
    }

    override suspend fun confirmConsumerVerification(
        consumerSessionClientSecret: String,
        verificationCode: String,
        requestSurface: String,
        type: VerificationType,
        consentGranted: Boolean?,
        requestOptions: ApiRequest.Options
    ): ConsumerSession {
        TODO("Not yet implemented")
    }

    override suspend fun postConsentUpdate(
        consumerSessionClientSecret: String,
        consentGranted: Boolean,
        requestSurface: String,
        requestOptions: ApiRequest.Options
    ): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun attachLinkConsumerToLinkAccountSession(
        consumerSessionClientSecret: String,
        clientSecret: String,
        requestSurface: String,
        requestOptions: ApiRequest.Options
    ): AttachConsumerToLinkAccountSession {
        TODO("Not yet implemented")
    }

    override suspend fun createPaymentDetails(
        consumerSessionClientSecret: String,
        paymentDetailsCreateParams: ConsumerPaymentDetailsCreateParams,
        requestSurface: String,
        requestOptions: ApiRequest.Options
    ): Result<ConsumerPaymentDetails> {
        TODO("Not yet implemented")
    }

    override suspend fun sharePaymentDetails(
        consumerSessionClientSecret: String,
        paymentDetailsId: String,
        expectedPaymentMethodType: String,
        billingPhone: String?,
        requestSurface: String,
        requestOptions: ApiRequest.Options,
        extraParams: Map<String, Any?>
    ): Result<SharePaymentDetails> {
        TODO("Not yet implemented")
    }

    override suspend fun updateAvailableIncentives(
        sessionId: String,
        paymentDetailsId: String,
        consumerSessionClientSecret: String,
        requestSurface: String,
        requestOptions: ApiRequest.Options
    ): Result<UpdateAvailableIncentives> {
        TODO("Not yet implemented")
    }

    override suspend fun createLinkAccountSession(
        consumerSessionClientSecret: String,
        intentToken: String?,
        linkMode: LinkMode?,
        requestSurface: String,
        requestOptions: ApiRequest.Options
    ): Result<LinkAccountSession> {
        TODO("Not yet implemented")
    }

    override suspend fun refreshConsumerSession(
        appId: String,
        consumerSessionClientSecret: String,
        supportedVerificationTypes: List<String>?,
        requestSurface: String,
        requestOptions: ApiRequest.Options
    ): ConsumerSessionRefresh = refreshConsumerSessionResult

    override suspend fun updatePhoneNumber(
        consumerSessionClientSecret: String,
        phoneNumber: String,
        requestSurface: String,
        requestOptions: ApiRequest.Options
    ): Result<ConsumerSession> {
        TODO("Not yet implemented")
    }

    data class LookupCall(
        val email: String?,
        val linkAuthIntentId: String?,
        val requestSurface: String,
        val sessionId: String,
        val requestOptions: ApiRequest.Options
    )

    data class MobileLookupCall(
        val email: String?,
        val emailSource: EmailSource?,
        val linkAuthIntentId: String?,
        val requestSurface: String,
        val verificationToken: String,
        val appId: String,
        val requestOptions: ApiRequest.Options,
        val sessionId: String
    )

    data class SignUpCall(
        val params: SignUpParams,
        val requestOptions: ApiRequest.Options
    )
}
