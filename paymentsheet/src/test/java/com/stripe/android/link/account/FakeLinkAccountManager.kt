package com.stripe.android.link.account

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.Turbine
import com.stripe.android.link.ConsumerState
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.link.LinkPaymentMethod
import com.stripe.android.link.TestFactory
import com.stripe.android.link.TestFactory.CONSUMER_SHIPPING_ADDRESSES
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.ui.inline.SignUpConsentAction
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerPaymentDetailsUpdateParams
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.ConsumerSessionRefresh
import com.stripe.android.model.ConsumerShippingAddresses
import com.stripe.android.model.EmailSource
import com.stripe.android.model.LinkAccountSession
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.SharePaymentDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.yield

internal open class FakeLinkAccountManager(
    val linkAccountHolder: LinkAccountHolder = LinkAccountHolder(SavedStateHandle()),
    accountStatusOverride: Flow<AccountStatus>? = null
) : LinkAccountManager {

    var lookupResult: Result<LinkAccount?> = Result.success(null)
    var refreshConsumerResult: Result<ConsumerSessionRefresh> = Result.success(
        ConsumerSessionRefresh(
            consumerSession = TestFactory.CONSUMER_SESSION,
            linkAuthIntent = null
        )
    )
    var signupResult: Result<LinkAccount> = Result.success(TestFactory.LINK_ACCOUNT)

    data class SignUpCall(
        val email: String,
        val phoneNumber: String?,
        val country: String?,
        val countryInferringMethod: String,
        val name: String?,
        val consentAction: SignUpConsentAction
    )

    data class LookupCall(
        val email: String,
        val emailSource: EmailSource,
        val startSession: Boolean,
        val customerId: String?
    )

    val signUpCalls = mutableListOf<SignUpCall>()
    val lookupCalls = mutableListOf<LookupCall>()

    override val linkAccountInfo: StateFlow<LinkAccountUpdate.Value> = linkAccountHolder.linkAccountInfo

    private val _accountStatus = MutableStateFlow<AccountStatus>(AccountStatus.SignedOut)
    override val accountStatus: Flow<AccountStatus> = accountStatusOverride ?: _accountStatus

    private val _consumerState =
        MutableStateFlow<ConsumerState?>(
            ConsumerState(paymentDetails = TestFactory.CONSUMER_PAYMENT_DETAILS.toLinkPaymentMethod())
        )

    override val consumerState: StateFlow<ConsumerState?> =
        _consumerState.asStateFlow()

    override var cachedShippingAddresses: ConsumerShippingAddresses? = null

    var lookupConsumerByAuthIntentResult: Result<LinkAccount?> = Result.success(TestFactory.LINK_ACCOUNT)
    var lookupConsumerByLinkAuthTokenResult: Result<LinkAccount?> = Result.success(TestFactory.LINK_ACCOUNT)
    var startVerificationResult: Result<LinkAccount> = Result.success(TestFactory.LINK_ACCOUNT)
    var confirmVerificationResult: Result<LinkAccount> = Result.success(TestFactory.LINK_ACCOUNT)
    var postConsentUpdateResult: Result<Unit> = Result.success(Unit)
    var signInWithUserInputResult: Result<LinkAccount> = Result.success(TestFactory.LINK_ACCOUNT)
    var logOutResult: Result<ConsumerSession> = Result.success(ConsumerSession("", "", "", ""))
    var createCardPaymentDetailsResult: Result<LinkPaymentDetails.New> = Result.success(
        value = TestFactory.LINK_NEW_PAYMENT_DETAILS
    )
    var shareCardPaymentDetailsResult: Result<LinkPaymentDetails.Saved> = Result.success(
        value = TestFactory.LINK_SAVED_PAYMENT_DETAILS
    )
    var createBankAccountPaymentDetailsResult: Result<ConsumerPaymentDetails.BankAccount> = Result.success(
        value = TestFactory.CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT
    )
    var createLinkAccountSessionResult: Result<LinkAccountSession> = Result.success(
        value = TestFactory.LINK_ACCOUNT_SESSION
    )
    var createPaymentMethodResult: Result<com.stripe.android.model.PaymentMethod> = Result.success(
        value = PaymentMethodFixtures.CARD_PAYMENT_METHOD
    )
    var sharePaymentDetails: Result<SharePaymentDetails> = Result.success(TestFactory.LINK_SHARE_PAYMENT_DETAILS)
    var updatePaymentDetailsResult = Result.success(TestFactory.CONSUMER_PAYMENT_DETAILS)
    var updatePhoneNumberResult: Result<LinkAccount> = Result.success(TestFactory.LINK_ACCOUNT)
    var deletePaymentDetailsResult: Result<Unit> = Result.success(Unit)
    var listPaymentDetailsResult: Result<ConsumerPaymentDetails> = Result.success(TestFactory.CONSUMER_PAYMENT_DETAILS)
        set(value) {
            field = value
            _consumerState.value = value.getOrNull()?.toLinkPaymentMethod()?.let {
                ConsumerState(paymentDetails = it)
            }
        }
    var listShippingAddressesResult: Result<ConsumerShippingAddresses> = Result.success(CONSUMER_SHIPPING_ADDRESSES)
        set(value) {
            field = value
            cachedShippingAddresses = value.getOrNull()
        }

    private val lookupByAuthIntentTurbine = Turbine<LookupCallByAuthIntent>()
    private val lookupByLinkAuthTokenTurbine = Turbine<LookupCallByLinkAuthToken>()

    private val updateCardDetailsTurbine = Turbine<ConsumerPaymentDetailsUpdateParams>()
    private val startVerificationTurbine = Turbine<Boolean>()

    val confirmVerificationTurbine = Turbine<String>()

    private val logoutCall = Turbine<Unit>()

    fun setConsumerPaymentDetails(consumerPaymentDetails: ConsumerPaymentDetails?) {
        _consumerState.value = consumerPaymentDetails?.toLinkPaymentMethod()?.let {
            ConsumerState(paymentDetails = it)
        }
    }

    fun setLinkAccount(account: LinkAccountUpdate.Value) {
        linkAccountHolder.set(account)
    }

    fun setAccountStatus(status: AccountStatus) {
        _accountStatus.value = status
    }

    // Unified API methods
    override suspend fun lookupByEmail(
        email: String,
        emailSource: EmailSource,
        startSession: Boolean,
        customerId: String?
    ): Result<LinkAccount?> {
        lookupCalls.add(LookupCall(email, emailSource, startSession, customerId))
        return lookupResult
    }

    override suspend fun signUp(
        email: String,
        phoneNumber: String?,
        country: String?,
        countryInferringMethod: String,
        name: String?,
        consentAction: SignUpConsentAction
    ): Result<LinkAccount> {
        signUpCalls.add(SignUpCall(email, phoneNumber, country, countryInferringMethod, name, consentAction))
        return signupResult
    }

    override suspend fun lookupByLinkAuthIntent(
        linkAuthIntentId: String?,
        customerId: String?
    ): Result<LinkAccount?> {
        lookupByAuthIntentTurbine.add(
            item = LookupCallByAuthIntent(
                linkAuthIntentId = linkAuthIntentId
            )
        )
        return lookupConsumerByAuthIntentResult
    }

    override suspend fun lookupByLinkAuthTokenClientSecret(linkAuthTokenClientSecret: String): Result<LinkAccount?> {
        lookupByLinkAuthTokenTurbine.add(
            item = LookupCallByLinkAuthToken(
                linkAuthTokenClientSecret = linkAuthTokenClientSecret
            )
        )

        return lookupConsumerByLinkAuthTokenResult
    }

    override suspend fun refreshConsumer(): Result<ConsumerSessionRefresh> {
        return refreshConsumerResult
    }

    override suspend fun signInWithUserInput(userInput: UserInput): Result<LinkAccount> {
        return signInWithUserInputResult
    }

    override suspend fun logOut(): Result<ConsumerSession> {
        logoutCall.add(Unit)
        return logOutResult
    }

    override suspend fun createCardPaymentDetails(
        paymentMethodCreateParams: PaymentMethodCreateParams
    ): Result<LinkPaymentDetails.New> {
        return createCardPaymentDetailsResult
    }

    override suspend fun shareCardPaymentDetails(
        cardPaymentDetails: LinkPaymentDetails.New
    ): Result<LinkPaymentDetails.Saved> {
        return shareCardPaymentDetailsResult
    }

    override suspend fun createBankAccountPaymentDetails(
        bankAccountId: String
    ): Result<ConsumerPaymentDetails.PaymentDetails> {
        yield()
        return createBankAccountPaymentDetailsResult
    }

    override suspend fun sharePaymentDetails(
        paymentDetailsId: String,
        expectedPaymentMethodType: String,
        billingPhone: String?,
        cvc: String?,
        allowRedisplay: String?,
        apiKey: String?
    ): Result<SharePaymentDetails> {
        return sharePaymentDetails
    }

    override suspend fun createLinkAccountSession(): Result<LinkAccountSession> {
        yield()
        return createLinkAccountSessionResult
    }

    override suspend fun createPaymentMethod(
        linkPaymentMethod: LinkPaymentMethod
    ): Result<com.stripe.android.model.PaymentMethod> {
        return createPaymentMethodResult
    }

    override suspend fun startVerification(isResendSmsCode: Boolean): Result<LinkAccount> {
        startVerificationTurbine.add(isResendSmsCode)
        return startVerificationResult
    }

    override suspend fun confirmVerification(code: String, consentGranted: Boolean?): Result<LinkAccount> {
        confirmVerificationTurbine.add(code)
        return confirmVerificationResult
    }

    override suspend fun postConsentUpdate(consentGranted: Boolean): Result<Unit> {
        return postConsentUpdateResult
    }

    override suspend fun listPaymentDetails(paymentMethodTypes: Set<String>): Result<ConsumerPaymentDetails> {
        return listPaymentDetailsResult
    }

    override suspend fun listShippingAddresses(): Result<ConsumerShippingAddresses> {
        return listShippingAddressesResult
    }

    override suspend fun deletePaymentDetails(paymentDetailsId: String) = deletePaymentDetailsResult

    override suspend fun updatePaymentDetails(
        updateParams: ConsumerPaymentDetailsUpdateParams,
        billingPhone: String?
    ): Result<ConsumerPaymentDetails> {
        updateCardDetailsTurbine.add(
            updateParams
        )
        return updatePaymentDetailsResult
    }

    override suspend fun updatePhoneNumber(phoneNumber: String): Result<LinkAccount> {
        return updatePhoneNumberResult
    }

    suspend fun awaitUpdateCardDetailsCall(): ConsumerPaymentDetailsUpdateParams {
        return updateCardDetailsTurbine.awaitItem()
    }

    suspend fun awaitLookupByAuthIntentCall(): LookupCallByAuthIntent {
        return lookupByAuthIntentTurbine.awaitItem()
    }

    suspend fun awaitStartVerificationCall(): Boolean {
        return startVerificationTurbine.awaitItem()
    }

    suspend fun awaitConfirmVerificationCall(): String {
        return confirmVerificationTurbine.awaitItem()
    }

    suspend fun awaitLogoutCall() {
        return logoutCall.awaitItem()
    }

    fun ensureAllEventsConsumed() {
        lookupByAuthIntentTurbine.ensureAllEventsConsumed()
    }

    private fun ConsumerPaymentDetails.toLinkPaymentMethod(): List<LinkPaymentMethod.ConsumerPaymentDetails> =
        paymentDetails.map {
            LinkPaymentMethod.ConsumerPaymentDetails(
                it,
                collectedCvc = null,
                billingPhone = null
            )
        }

    data class LookupCallByAuthIntent(
        val linkAuthIntentId: String?,
    )

    data class LookupCallByLinkAuthToken(
        val linkAuthTokenClientSecret: String?
    )
}
