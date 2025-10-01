package com.stripe.android.link.account

import com.stripe.android.link.TestFactory
import com.stripe.android.link.ui.inline.SignUpConsentAction
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.model.ConsumerSessionRefresh
import com.stripe.android.model.ConsumerSessionSignup
import com.stripe.android.model.EmailSource

internal class FakeLinkAuth : LinkAuth {
    var lookupResult: Result<ConsumerSessionLookup> = Result.success(TestFactory.CONSUMER_SESSION_LOOKUP)
    var signupResult: Result<ConsumerSessionSignup> = Result.success(TestFactory.CONSUMER_SESSION_SIGN_UP)
    var refreshConsumerResult: Result<ConsumerSessionRefresh> = Result.success(
        ConsumerSessionRefresh(
            consumerSession = TestFactory.CONSUMER_SESSION,
            linkAuthIntent = null
        )
    )

    // Track calls for verification
    var lookupCalls = mutableListOf<LookupCall>()
    var signupCalls = mutableListOf<SignupCall>()

    data class LookupCall(
        val email: String?,
        val emailSource: EmailSource?,
        val linkAuthIntentId: String?,
        val customerId: String?,
        val sessionId: String,
        val supportedVerificationTypes: List<String>?
    )

    data class SignupCall(
        val email: String,
        val phoneNumber: String?,
        val country: String?,
        val countryInferringMethod: String,
        val name: String?,
        val consentAction: SignUpConsentAction
    )

    override suspend fun lookup(
        email: String?,
        emailSource: EmailSource?,
        linkAuthIntentId: String?,
        customerId: String?,
        sessionId: String,
        supportedVerificationTypes: List<String>?
    ): Result<ConsumerSessionLookup> {
        lookupCalls.add(
            LookupCall(
                email,
                emailSource,
                linkAuthIntentId,
                customerId,
                sessionId,
                supportedVerificationTypes
            )
        )
        return lookupResult
    }

    override suspend fun signup(
        email: String,
        phoneNumber: String?,
        country: String?,
        countryInferringMethod: String,
        name: String?,
        consentAction: SignUpConsentAction
    ): Result<ConsumerSessionSignup> {
        signupCalls.add(SignupCall(email, phoneNumber, country, countryInferringMethod, name, consentAction))
        return signupResult
    }

    override suspend fun refreshConsumer(
        consumerSessionClientSecret: String,
        supportedVerificationTypes: List<String>?
    ): Result<ConsumerSessionRefresh> {
        return refreshConsumerResult
    }

    fun reset() {
        lookupCalls.clear()
        signupCalls.clear()
        lookupResult = Result.success(TestFactory.CONSUMER_SESSION_LOOKUP)
        signupResult = Result.success(TestFactory.CONSUMER_SESSION_SIGN_UP)
        refreshConsumerResult = Result.success(
            ConsumerSessionRefresh(
                consumerSession = TestFactory.CONSUMER_SESSION,
                linkAuthIntent = null
            )
        )
    }
}
