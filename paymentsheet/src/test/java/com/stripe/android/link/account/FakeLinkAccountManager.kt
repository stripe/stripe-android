package com.stripe.android.link.account

import app.cash.turbine.Turbine
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.link.TestFactory
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.ui.inline.SignUpConsentAction
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerPaymentDetailsUpdateParams
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.model.EmailSource
import com.stripe.android.model.PaymentMethodCreateParams
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal open class FakeLinkAccountManager : LinkAccountManager {
    private val _linkAccount = MutableStateFlow<LinkAccount?>(null)
    override val linkAccount: StateFlow<LinkAccount?> = _linkAccount

    private val _accountStatus = MutableStateFlow(AccountStatus.SignedOut)
    override val accountStatus: Flow<AccountStatus> = _accountStatus

    var lookupConsumerResult: Result<LinkAccount?> = Result.success(null)
    var mobileLookupConsumerResult: Result<LinkAccount?> = Result.success(TestFactory.LINK_ACCOUNT)
    var startVerificationResult: Result<LinkAccount> = Result.success(LinkAccount(ConsumerSession("", "", "", "")))
    var confirmVerificationResult: Result<LinkAccount> = Result.success(LinkAccount(ConsumerSession("", "", "", "")))
    var signUpResult: Result<LinkAccount> = Result.success(TestFactory.LINK_ACCOUNT)
    var mobileSignUpResult: Result<LinkAccount> = Result.success(TestFactory.LINK_ACCOUNT)
    var signInWithUserInputResult: Result<LinkAccount> = Result.success(LinkAccount(ConsumerSession("", "", "", "")))
    var logOutResult: Result<ConsumerSession> = Result.success(ConsumerSession("", "", "", ""))
    var createCardPaymentDetailsResult: Result<LinkPaymentDetails> = Result.success(
        value = TestFactory.LINK_NEW_PAYMENT_DETAILS
    )
    var listPaymentDetailsResult: Result<ConsumerPaymentDetails> = Result.success(TestFactory.CONSUMER_PAYMENT_DETAILS)
    var updatePaymentDetailsResult = Result.success(TestFactory.CONSUMER_PAYMENT_DETAILS)
    var deletePaymentDetailsResult: Result<Unit> = Result.success(Unit)
    var linkAccountFromLookupResult: LinkAccount? = null

    private val lookupTurbine = Turbine<LookupCall>()
    private val mobileLookupTurbine = Turbine<MobileLookupCall>()

    private val signupTurbine = Turbine<SignUpCall>()
    private val mobileSignUpTurbine = Turbine<MobileSignUpCall>()

    override var consumerPublishableKey: String? = null

    fun setLinkAccount(account: LinkAccount?) {
        _linkAccount.value = account
    }

    fun setAccountStatus(status: AccountStatus) {
        _accountStatus.value = status
    }

    override suspend fun lookupConsumer(email: String, startSession: Boolean): Result<LinkAccount?> {
        lookupTurbine.add(
            item = LookupCall(
                email = email,
                startSession = startSession
            )
        )
        return lookupConsumerResult
    }

    override suspend fun mobileLookupConsumer(
        email: String,
        emailSource: EmailSource,
        verificationToken: String,
        appId: String,
        startSession: Boolean
    ): Result<LinkAccount?> {
        mobileLookupTurbine.add(
            item = MobileLookupCall(
                email = email,
                emailSource = emailSource,
                verificationToken = verificationToken,
                appId = appId,
                startSession = startSession
            )
        )
        return mobileLookupConsumerResult
    }

    override suspend fun signUp(
        email: String,
        phone: String,
        country: String,
        name: String?,
        consentAction: SignUpConsentAction
    ): Result<LinkAccount> {
        signupTurbine.add(
            item = SignUpCall(
                email = email,
                phone = phone,
                country = country,
                name = name,
                consentAction = consentAction
            )
        )
        return signUpResult
    }

    override suspend fun mobileSignUp(
        email: String,
        phone: String,
        country: String,
        name: String?,
        verificationToken: String,
        appId: String,
        consentAction: SignUpConsentAction
    ): Result<LinkAccount> {
        mobileSignUpTurbine.add(
            item = MobileSignUpCall(
                email = email,
                phone = phone,
                country = country,
                name = name,
                verificationToken = verificationToken,
                appId = appId,
                consentAction = consentAction
            )
        )
        return mobileSignUpResult
    }

    override suspend fun signInWithUserInput(userInput: UserInput): Result<LinkAccount> {
        return signInWithUserInputResult
    }

    override suspend fun logOut(): Result<ConsumerSession> {
        return logOutResult
    }

    override suspend fun createCardPaymentDetails(
        paymentMethodCreateParams: PaymentMethodCreateParams,
        shouldShareCardPaymentDetails: Boolean
    ): Result<LinkPaymentDetails> {
        return createCardPaymentDetailsResult
    }

    override fun setLinkAccountFromLookupResult(lookup: ConsumerSessionLookup, startSession: Boolean): LinkAccount? {
        return linkAccountFromLookupResult
    }

    override suspend fun startVerification(): Result<LinkAccount> {
        return startVerificationResult
    }

    override suspend fun confirmVerification(code: String): Result<LinkAccount> {
        return confirmVerificationResult
    }

    override suspend fun listPaymentDetails(paymentMethodTypes: Set<String>): Result<ConsumerPaymentDetails> {
        return listPaymentDetailsResult
    }

    override suspend fun deletePaymentDetails(paymentDetailsId: String) = deletePaymentDetailsResult
    override suspend fun updatePaymentDetails(
        updateParams: ConsumerPaymentDetailsUpdateParams
    ): Result<ConsumerPaymentDetails> {
        return updatePaymentDetailsResult
    }

    suspend fun awaitMobileSignUpCall(): MobileSignUpCall {
        return mobileSignUpTurbine.awaitItem()
    }

    suspend fun awaitSignUpCall(): SignUpCall {
        return signupTurbine.awaitItem()
    }

    suspend fun awaitMobileLookupCall(): MobileLookupCall {
        return mobileLookupTurbine.awaitItem()
    }

    suspend fun awaitLookupCall(): LookupCall {
        return lookupTurbine.awaitItem()
    }

    fun ensureAllEventsConsumed() {
        lookupTurbine.ensureAllEventsConsumed()
        mobileLookupTurbine.ensureAllEventsConsumed()
        signupTurbine.ensureAllEventsConsumed()
        mobileSignUpTurbine.ensureAllEventsConsumed()
    }

    data class LookupCall(
        val email: String,
        val startSession: Boolean
    )

    data class MobileLookupCall(
        val email: String,
        val emailSource: EmailSource,
        val verificationToken: String,
        val appId: String,
        val startSession: Boolean
    )

    data class SignUpCall(
        val email: String,
        val phone: String,
        val country: String,
        val name: String?,
        val consentAction: SignUpConsentAction
    )

    data class MobileSignUpCall(
        val email: String,
        val phone: String,
        val country: String,
        val name: String?,
        val verificationToken: String,
        val appId: String,
        val consentAction: SignUpConsentAction
    )
}
