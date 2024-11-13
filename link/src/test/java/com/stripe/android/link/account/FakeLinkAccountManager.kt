package com.stripe.android.link.account

import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.link.TestFactory
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.ui.inline.SignUpConsentAction
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.model.PaymentMethodCreateParams
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.mockito.kotlin.mock

internal open class FakeLinkAccountManager : LinkAccountManager {
    private val _linkAccount = MutableStateFlow<LinkAccount?>(null)
    override val linkAccount: StateFlow<LinkAccount?> = _linkAccount

    private val _accountStatus = MutableStateFlow(AccountStatus.SignedOut)
    override val accountStatus: Flow<AccountStatus> = _accountStatus

    var lookupConsumerResult: Result<LinkAccount?> = Result.success(null)
    var startVerificationResult: Result<LinkAccount> = Result.success(LinkAccount(ConsumerSession("", "", "", "")))
    var confirmVerificationResult: Result<LinkAccount> = Result.success(LinkAccount(ConsumerSession("", "", "", "")))
    var signUpResult: Result<LinkAccount> = Result.success(LinkAccount(ConsumerSession("", "", "", "")))
    var signInWithUserInputResult: Result<LinkAccount> = Result.success(LinkAccount(ConsumerSession("", "", "", "")))
    var logOutResult: Result<ConsumerSession> = Result.success(ConsumerSession("", "", "", ""))
    var createCardPaymentDetailsResult: Result<LinkPaymentDetails> = Result.success(
        value = LinkPaymentDetails.Saved(
            paymentDetails = ConsumerPaymentDetails.Card(
                id = "pm_123",
                last4 = "4242",
            ),
            paymentMethodCreateParams = mock(),
        )
    )
    var listPaymentDetailsResult: Result<ConsumerPaymentDetails> = Result.success(TestFactory.CONSUMER_PAYMENT_DETAILS)
    var linkAccountFromLookupResult: LinkAccount? = null
    override var consumerPublishableKey: String? = null

    fun setLinkAccount(account: LinkAccount?) {
        _linkAccount.value = account
    }

    fun setAccountStatus(status: AccountStatus) {
        _accountStatus.value = status
    }

    override suspend fun lookupConsumer(email: String, startSession: Boolean): Result<LinkAccount?> {
        return lookupConsumerResult
    }

    override suspend fun signUp(
        email: String,
        phone: String,
        country: String,
        name: String?,
        consentAction: SignUpConsentAction
    ): Result<LinkAccount> {
        return signUpResult
    }

    override suspend fun signInWithUserInput(userInput: UserInput): Result<LinkAccount> {
        return signInWithUserInputResult
    }

    override suspend fun logOut(): Result<ConsumerSession> {
        return logOutResult
    }

    override suspend fun createCardPaymentDetails(
        paymentMethodCreateParams: PaymentMethodCreateParams
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

    override suspend fun listPaymentDetails(): Result<ConsumerPaymentDetails> {
        return listPaymentDetailsResult
    }
}
