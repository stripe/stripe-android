package com.stripe.android.link.account

import app.cash.turbine.Turbine
import com.stripe.android.link.TestFactory
import com.stripe.android.link.ui.inline.SignUpConsentAction
import com.stripe.android.model.EmailSource

internal class FakeLinkAuth : LinkAuth {
    var signupResult: LinkAuthResult = LinkAuthResult.Success(TestFactory.LINK_ACCOUNT)
    var lookupResult: LinkAuthResult = LinkAuthResult.Success(TestFactory.LINK_ACCOUNT)

    private val signupTurbine = Turbine<SignUpCall>()
    private val lookupTurbine = Turbine<LookupCall>()

    override suspend fun signUp(
        email: String,
        phoneNumber: String,
        country: String,
        name: String?,
        consentAction: SignUpConsentAction
    ): LinkAuthResult {
        signupTurbine.add(
            item = SignUpCall(
                email = email,
                phone = phoneNumber,
                country = country,
                name = name,
                consentAction = consentAction
            )
        )
        return signupResult
    }

    override suspend fun lookUp(email: String, emailSource: EmailSource): LinkAuthResult {
        lookupTurbine.add(
            item = LookupCall(
                email = email,
                emailSource = emailSource
            )
        )
        return lookupResult
    }

    suspend fun awaitSignUpCall(): SignUpCall {
        return signupTurbine.awaitItem()
    }

    suspend fun awaitLookupCall(): LookupCall {
        return lookupTurbine.awaitItem()
    }

    fun ensureAllItemsConsumed() {
        lookupTurbine.ensureAllEventsConsumed()
        signupTurbine.ensureAllEventsConsumed()
    }

    data class SignUpCall(
        val email: String,
        val phone: String,
        val country: String,
        val name: String?,
        val consentAction: SignUpConsentAction
    )

    data class LookupCall(
        val email: String,
        val emailSource: EmailSource
    )
}
