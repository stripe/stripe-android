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

    var lookupConsumerResult: Result<LinkAccount?> = Result.success(null)
    var mobileLookupConsumerResult: Result<LinkAccount?> = Result.success(TestFactory.LINK_ACCOUNT)
    var lookupConsumerByAuthIntentResult: Result<LinkAccount?> = Result.success(TestFactory.LINK_ACCOUNT)
    var mobileLookupConsumerByAuthIntentResult: Result<LinkAccount?> = Result.success(TestFactory.LINK_ACCOUNT)
    var startVerificationResult: Result<LinkAccount> = Result.success(TestFactory.LINK_ACCOUNT)
    var confirmVerificationResult: Result<LinkAccount> = Result.success(TestFactory.LINK_ACCOUNT)
    var postConsentUpdateResult: Result<Unit> = Result.success(Unit)
    var signUpResult: Result<LinkAccount> = Result.success(TestFactory.LINK_ACCOUNT)
    var mobileSignUpResult: Result<LinkAccount> = Result.success(TestFactory.LINK_ACCOUNT)
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

    private val lookupTurbine = Turbine<LookupCall>()
    private val mobileLookupTurbine = Turbine<MobileLookupCall>()
    private val lookupByAuthIntentTurbine = Turbine<LookupCallByAuthIntent>()
    private val mobileLookupByAuthIntentTurbine = Turbine<MobileLookupCallByAuthIntent>()

    private val signupTurbine = Turbine<SignUpCall>()
    private val mobileSignUpTurbine = Turbine<MobileSignUpCall>()
    private val updateCardDetailsTurbine = Turbine<ConsumerPaymentDetailsUpdateParams>()
    private val startVerificationTurbine = Turbine<Unit>()

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

    override suspend fun lookupConsumer(
        email: String?,
        startSession: Boolean,
        customerId: String?
    ): Result<LinkAccount?> {
        lookupTurbine.add(
            item = LookupCall(
                email = email,
                linkAuthIntentId = null,
                startSession = startSession
            )
        )
        return lookupConsumerResult
    }

    override suspend fun mobileLookupConsumer(
        email: String?,
        emailSource: EmailSource?,
        verificationToken: String,
        appId: String,
        startSession: Boolean,
        customerId: String?
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

    override suspend fun lookupConsumerByAuthIntent(
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

    override suspend fun mobileLookupConsumerByAuthIntent(
        linkAuthIntentId: String?,
        verificationToken: String,
        appId: String,
        customerId: String?
    ): Result<LinkAccount?> {
        mobileLookupByAuthIntentTurbine.add(
            item = MobileLookupCallByAuthIntent(
                linkAuthIntentId = linkAuthIntentId,
                verificationToken = verificationToken,
                appId = appId
            )
        )
        return mobileLookupConsumerByAuthIntentResult
    }

    override suspend fun signUp(
        email: String,
        phone: String?,
        country: String?,
        countryInferringMethod: String,
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
        countryInferringMethod: String,
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

    override suspend fun startVerification(): Result<LinkAccount> {
        startVerificationTurbine.add(Unit)
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

    suspend fun awaitMobileSignUpCall(): MobileSignUpCall {
        return mobileSignUpTurbine.awaitItem()
    }

    suspend fun awaitUpdateCardDetailsCall(): ConsumerPaymentDetailsUpdateParams {
        return updateCardDetailsTurbine.awaitItem()
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

    suspend fun awaitLookupByAuthIntentCall(): LookupCallByAuthIntent {
        return lookupByAuthIntentTurbine.awaitItem()
    }

    suspend fun awaitMobileLookupByAuthIntentCall(): MobileLookupCallByAuthIntent {
        return mobileLookupByAuthIntentTurbine.awaitItem()
    }

    suspend fun awaitStartVerificationCall() {
        return startVerificationTurbine.awaitItem()
    }

    suspend fun awaitConfirmVerificationCall(): String {
        return confirmVerificationTurbine.awaitItem()
    }

    suspend fun awaitLogoutCall() {
        return logoutCall.awaitItem()
    }

    fun ensureAllEventsConsumed() {
        lookupTurbine.ensureAllEventsConsumed()
        mobileLookupTurbine.ensureAllEventsConsumed()
        lookupByAuthIntentTurbine.ensureAllEventsConsumed()
        mobileLookupByAuthIntentTurbine.ensureAllEventsConsumed()
        signupTurbine.ensureAllEventsConsumed()
        mobileSignUpTurbine.ensureAllEventsConsumed()
    }

    private fun ConsumerPaymentDetails.toLinkPaymentMethod(): List<LinkPaymentMethod.ConsumerPaymentDetails> =
        paymentDetails.map {
            LinkPaymentMethod.ConsumerPaymentDetails(
                it,
                collectedCvc = null,
                billingPhone = null
            )
        }

    data class LookupCall(
        val email: String?,
        val linkAuthIntentId: String?,
        val startSession: Boolean
    )

    data class MobileLookupCall(
        val email: String?,
        val emailSource: EmailSource?,
        val verificationToken: String,
        val appId: String,
        val startSession: Boolean
    )

    data class LookupCallByAuthIntent(
        val linkAuthIntentId: String?,
    )

    data class MobileLookupCallByAuthIntent(
        val linkAuthIntentId: String?,
        val verificationToken: String,
        val appId: String,
    )

    data class SignUpCall(
        val email: String,
        val phone: String?,
        val country: String?,
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
