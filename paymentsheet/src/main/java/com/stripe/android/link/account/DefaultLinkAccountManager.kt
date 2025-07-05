package com.stripe.android.link.account

import androidx.annotation.VisibleForTesting
import com.stripe.android.core.BuildConfig
import com.stripe.android.core.Logger
import com.stripe.android.core.exception.StripeException
import com.stripe.android.link.ConsumerState
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkAccountUpdate.Value.UpdateReason
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.link.NoLinkAccountFoundException
import com.stripe.android.link.analytics.LinkEventsReporter
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.repositories.LinkRepository
import com.stripe.android.link.ui.inline.SignUpConsentAction
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerPaymentDetailsUpdateParams
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.model.ConsumerShippingAddresses
import com.stripe.android.model.ConsumerSignUpConsentAction
import com.stripe.android.model.EmailSource
import com.stripe.android.model.LinkAccountSession
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.SharePaymentDetails
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.model.amount
import com.stripe.android.paymentsheet.model.currency
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Manages the Link account for the current user, persisting it across app usages.
 */
@SuppressWarnings("TooManyFunctions")
internal class DefaultLinkAccountManager @Inject constructor(
    private val linkAccountHolder: LinkAccountHolder,
    private val config: LinkConfiguration,
    private val linkRepository: LinkRepository,
    private val linkEventsReporter: LinkEventsReporter,
    private val errorReporter: ErrorReporter,
) : LinkAccountManager {

    override val linkAccountInfo: StateFlow<LinkAccountUpdate.Value>
        get() = linkAccountHolder.linkAccountInfo

    private val _consumerState: MutableStateFlow<ConsumerState?> = MutableStateFlow(null)
    override val consumerState: StateFlow<ConsumerState?> = _consumerState.asStateFlow()

    override var cachedShippingAddresses: ConsumerShippingAddresses? = null

    override val accountStatus = linkAccountHolder.linkAccountInfo
        .map { it.account.fetchAccountStatus(it.lastUpdateReason != UpdateReason.LoggedOut) }

    override suspend fun lookupConsumer(
        email: String,
        startSession: Boolean,
    ): Result<LinkAccount?> =
        linkRepository.lookupConsumer(email)
            .onFailure { error ->
                linkEventsReporter.onAccountLookupFailure(error)
            }.map { consumerSessionLookup ->
                setLinkAccountFromLookupResult(
                    lookup = consumerSessionLookup,
                    startSession = startSession,
                )
            }

    override suspend fun mobileLookupConsumer(
        email: String,
        emailSource: EmailSource,
        verificationToken: String,
        appId: String,
        startSession: Boolean
    ): Result<LinkAccount?> {
        return linkRepository.mobileLookupConsumer(
            verificationToken = verificationToken,
            appId = appId,
            email = email,
            sessionId = config.elementsSessionId,
            emailSource = emailSource
        ).onFailure { error ->
            linkEventsReporter.onAccountLookupFailure(error)
        }.map { consumerSessionLookup ->
            setLinkAccountFromLookupResult(
                lookup = consumerSessionLookup,
                startSession = startSession
            )
        }
    }

    override suspend fun createLinkAccountSession(): Result<LinkAccountSession> {
        return runCatching {
            val linkAccount = requireNotNull(linkAccountHolder.linkAccountInfo.value.account)
            linkRepository.createLinkAccountSession(
                consumerSessionClientSecret = linkAccount.clientSecret,
                stripeIntent = config.stripeIntent,
                linkMode = config.linkMode,
                consumerPublishableKey = linkAccount.consumerPublishableKey,
            ).getOrThrow()
        }
    }

    override suspend fun signInWithUserInput(
        userInput: UserInput
    ): Result<LinkAccount> =
        when (userInput) {
            is UserInput.SignIn -> lookupConsumer(userInput.email).mapCatching {
                requireNotNull(it) { "Error fetching user account" }
            }
            is UserInput.SignUp -> signUpIfValidSessionState(
                email = userInput.email,
                country = userInput.country,
                phone = userInput.phone,
                name = userInput.name,
                consentAction = userInput.consentAction,
            )
        }

    override suspend fun logOut(): Result<ConsumerSession> {
        return runCatching {
            requireNotNull(linkAccountHolder.linkAccountInfo.value.account)
        }.mapCatching { account ->
            runCatching {
                linkRepository.logOut(
                    consumerSessionClientSecret = account.clientSecret,
                    consumerAccountPublishableKey = account.consumerPublishableKey,
                ).getOrThrow()
            }.onSuccess {
                errorReporter.report(ErrorReporter.SuccessEvent.LINK_LOG_OUT_SUCCESS)
                Logger.getInstance(BuildConfig.DEBUG).debug("Logged out of Link successfully")
            }.onFailure { error ->
                errorReporter.report(
                    ErrorReporter.ExpectedErrorEvent.LINK_LOG_OUT_FAILURE,
                    StripeException.create(error)
                )
                Logger.getInstance(BuildConfig.DEBUG).warning("Failed to log out of Link: $error")
            }.getOrThrow()
        }
    }

    /**
     * Attempts sign up if session is in valid state for sign up
     */
    private suspend fun signUpIfValidSessionState(
        email: String,
        phone: String,
        country: String,
        name: String?,
        consentAction: SignUpConsentAction
    ): Result<LinkAccount> {
        val currentAccount = linkAccountHolder.linkAccountInfo.value.account
        val currentEmail = currentAccount?.email ?: config.customerInfo.email

        return when (val status = currentAccount.fetchAccountStatus(shouldLookup = true)) {
            AccountStatus.Verified -> {
                linkEventsReporter.onInvalidSessionState(LinkEventsReporter.SessionState.Verified)

                Result.failure(
                    AlreadyLoggedInLinkException(
                        email = currentEmail,
                        accountStatus = status
                    )
                )
            }
            AccountStatus.NeedsVerification,
            AccountStatus.VerificationStarted -> {
                linkEventsReporter.onInvalidSessionState(LinkEventsReporter.SessionState.RequiresVerification)

                Result.failure(
                    AlreadyLoggedInLinkException(
                        email = currentEmail,
                        accountStatus = status
                    )
                )
            }
            AccountStatus.SignedOut,
            AccountStatus.Error -> {
                signUp(
                    email = email,
                    phone = phone,
                    country = country,
                    name = name,
                    consentAction = consentAction
                ).onSuccess {
                    linkEventsReporter.onSignupCompleted(true)
                }.onFailure { error ->
                    linkEventsReporter.onSignupFailure(true, error)
                }
            }
        }
    }

    override suspend fun signUp(
        email: String,
        phone: String,
        country: String,
        name: String?,
        consentAction: SignUpConsentAction
    ): Result<LinkAccount> =
        linkRepository.consumerSignUp(email, phone, country, name, consentAction.consumerAction)
            .map { consumerSessionSignup ->
                setAccount(
                    consumerSession = consumerSessionSignup.consumerSession,
                    publishableKey = consumerSessionSignup.publishableKey,
                    nameUsedInSignup = name,
                    phoneNumberUsedInSignup = phone
                )
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
        return linkRepository.mobileSignUp(
            name = name,
            email = email,
            phoneNumber = phone,
            country = country,
            consentAction = consentAction.consumerAction,
            verificationToken = verificationToken,
            appId = appId,
            amount = config.stripeIntent.amount,
            currency = config.stripeIntent.currency,
            incentiveEligibilitySession = null,
        ).map { consumerSessionSignUp ->
            setAccount(
                consumerSession = consumerSessionSignUp.consumerSession,
                publishableKey = consumerSessionSignUp.publishableKey,
                nameUsedInSignup = name,
                phoneNumberUsedInSignup = phone
            )
        }
    }

    override suspend fun createCardPaymentDetails(
        paymentMethodCreateParams: PaymentMethodCreateParams
    ): Result<LinkPaymentDetails> {
        val linkAccountValue = linkAccountHolder.linkAccountInfo.value.account
        return if (linkAccountValue != null) {
            linkAccountValue.let { account ->
                linkRepository.createCardPaymentDetails(
                    paymentMethodCreateParams = paymentMethodCreateParams,
                    userEmail = account.email,
                    stripeIntent = config.stripeIntent,
                    consumerSessionClientSecret = account.clientSecret,
                    consumerPublishableKey = account.consumerPublishableKey.takeIf { !config.passthroughModeEnabled },
                    active = config.passthroughModeEnabled,
                ).mapCatching {
                    if (config.passthroughModeEnabled) {
                        linkRepository.shareCardPaymentDetails(
                            id = it.paymentDetails.id,
                            last4 = paymentMethodCreateParams.cardLast4().orEmpty(),
                            consumerSessionClientSecret = account.clientSecret,
                            paymentMethodCreateParams = paymentMethodCreateParams,
                            allowRedisplay = paymentMethodCreateParams.allowRedisplay,
                        ).getOrThrow()
                    } else {
                        it
                    }
                }.onSuccess {
                    errorReporter.report(ErrorReporter.SuccessEvent.LINK_CREATE_CARD_SUCCESS)
                }
            }
        } else {
            errorReporter.report(ErrorReporter.UnexpectedErrorEvent.LINK_ATTACH_CARD_WITH_NULL_ACCOUNT)
            Result.failure(
                IllegalStateException("A non-null Link account is needed to create payment details")
            )
        }
    }

    override suspend fun createBankAccountPaymentDetails(
        bankAccountId: String
    ): Result<ConsumerPaymentDetails.PaymentDetails> {
        val linkAccount = linkAccountHolder.linkAccountInfo.value.account
        return if (linkAccount != null) {
            linkRepository.createBankAccountPaymentDetails(
                bankAccountId = bankAccountId,
                userEmail = linkAccount.email,
                consumerSessionClientSecret = linkAccount.clientSecret,
            )
        } else {
            errorReporter.report(ErrorReporter.UnexpectedErrorEvent.LINK_ATTACH_BANK_ACCOUNT_WITH_NULL_ACCOUNT)
            Result.failure(
                IllegalStateException("A non-null Link account is needed to create payment details")
            )
        }
    }

    override suspend fun sharePaymentDetails(
        paymentDetailsId: String,
        expectedPaymentMethodType: String,
        billingPhone: String?,
        cvc: String?,
    ): Result<SharePaymentDetails> {
        return runCatching {
            requireNotNull(linkAccountHolder.linkAccountInfo.value.account)
        }.mapCatching { account ->
            linkRepository.sharePaymentDetails(
                paymentDetailsId = paymentDetailsId,
                consumerSessionClientSecret = account.clientSecret,
                expectedPaymentMethodType = expectedPaymentMethodType,
                billingPhone = billingPhone,
                cvc = cvc,
            ).getOrThrow()
        }
    }

    private suspend fun setAccount(
        consumerSession: ConsumerSession,
        publishableKey: String?,
        nameUsedInSignup: String?,
        phoneNumberUsedInSignup: String?,
    ): LinkAccount {
        val currentAccount = linkAccountHolder.linkAccountInfo.value.account
        val newConsumerPublishableKey = publishableKey
            ?: currentAccount?.consumerPublishableKey
                ?.takeIf { currentAccount.email == consumerSession.emailAddress }
        val newAccount = LinkAccount(
            consumerSession = consumerSession,
            consumerPublishableKey = newConsumerPublishableKey,
            nameUsedInSignup = nameUsedInSignup,
            phoneNumberUsedInSignup = phoneNumberUsedInSignup
        )
        withContext(Dispatchers.Main.immediate) {
            linkAccountHolder.set(LinkAccountUpdate.Value(newAccount))
        }
        return newAccount
    }

    override suspend fun setLinkAccountFromLookupResult(
        lookup: ConsumerSessionLookup,
        startSession: Boolean,
    ): LinkAccount? {
        return lookup.consumerSession?.let { consumerSession ->
            if (startSession) {
                setAccountNullable(
                    consumerSession = consumerSession,
                    publishableKey = lookup.publishableKey,
                    nameUsedInSignup = null,
                    phoneNumberUsedInSignup = null
                )
            } else {
                LinkAccount(consumerSession, lookup.publishableKey)
            }
        }
    }

    override suspend fun startVerification(): Result<LinkAccount> {
        val linkAccount = linkAccountHolder.linkAccountInfo.value.account
            ?: return Result.failure(NoLinkAccountFoundException())
        linkEventsReporter.on2FAStart()
        return linkRepository.startVerification(
            consumerSessionClientSecret = linkAccount.clientSecret,
            consumerPublishableKey = linkAccount.consumerPublishableKey
        )
            .onFailure {
                linkEventsReporter.on2FAStartFailure()
            }.map { consumerSession ->
                setAccount(
                    consumerSession = consumerSession,
                    publishableKey = null,
                    nameUsedInSignup = null,
                    phoneNumberUsedInSignup = null
                )
            }
    }

    override suspend fun confirmVerification(code: String): Result<LinkAccount> {
        val linkAccount = linkAccountHolder.linkAccountInfo.value.account
            ?: return Result.failure(NoLinkAccountFoundException())
        return linkRepository.confirmVerification(
            verificationCode = code,
            consumerSessionClientSecret = linkAccount.clientSecret,
            consumerPublishableKey = linkAccount.consumerPublishableKey
        )
            .onSuccess {
                linkEventsReporter.on2FAComplete()
            }.onFailure {
                linkEventsReporter.on2FAFailure()
            }.map { consumerSession ->
                setAccount(
                    consumerSession = consumerSession,
                    publishableKey = null,
                    nameUsedInSignup = null,
                    phoneNumberUsedInSignup = null
                )
            }
    }

    override suspend fun listPaymentDetails(paymentMethodTypes: Set<String>): Result<ConsumerPaymentDetails> {
        val linkAccount = linkAccountHolder.linkAccountInfo.value.account
            ?: return Result.failure(NoLinkAccountFoundException())
        return linkRepository.listPaymentDetails(
            paymentMethodTypes = paymentMethodTypes,
            consumerSessionClientSecret = linkAccount.clientSecret,
            consumerPublishableKey = linkAccount.consumerPublishableKey
        ).onSuccess { paymentDetailsList ->
            _consumerState.value = _consumerState.value
                ?.withPaymentDetailsResponse(paymentDetailsList)
                ?: ConsumerState.fromResponse(paymentDetailsList)
        }
    }

    override suspend fun listShippingAddresses(): Result<ConsumerShippingAddresses> {
        val linkAccount = linkAccountHolder.linkAccountInfo.value.account
            ?: return Result.failure(NoLinkAccountFoundException())
        return linkRepository.listShippingAddresses(
            consumerSessionClientSecret = linkAccount.clientSecret,
            consumerPublishableKey = linkAccount.consumerPublishableKey,
        )
    }

    override suspend fun deletePaymentDetails(paymentDetailsId: String): Result<Unit> {
        val linkAccount = linkAccountHolder.linkAccountInfo.value.account
            ?: return Result.failure(NoLinkAccountFoundException())
        return linkRepository.deletePaymentDetails(
            paymentDetailsId = paymentDetailsId,
            consumerSessionClientSecret = linkAccount.clientSecret,
            consumerPublishableKey = linkAccount.consumerPublishableKey
        )
    }

    override suspend fun updatePaymentDetails(
        updateParams: ConsumerPaymentDetailsUpdateParams,
        phone: String?
    ): Result<ConsumerPaymentDetails> {
        val linkAccount = linkAccountHolder.linkAccountInfo.value.account
            ?: return Result.failure(NoLinkAccountFoundException())
        return linkRepository.updatePaymentDetails(
            updateParams = updateParams,
            consumerSessionClientSecret = linkAccount.clientSecret,
            consumerPublishableKey = linkAccount.consumerPublishableKey
        ).map { updatedPaymentDetails ->
            updatedPaymentDetails.also {
                _consumerState.value = _consumerState.value?.withUpdatedPaymentDetail(
                    updatedPayment = it.paymentDetails.first(),
                    billingPhone = phone
                )
            }
        }
    }

    @VisibleForTesting
    internal suspend fun setAccountNullable(
        consumerSession: ConsumerSession?,
        publishableKey: String?,
        nameUsedInSignup: String?,
        phoneNumberUsedInSignup: String?,
    ): LinkAccount? {
        return consumerSession?.let {
            setAccount(
                consumerSession = it,
                publishableKey = publishableKey,
                nameUsedInSignup = nameUsedInSignup,
                phoneNumberUsedInSignup = phoneNumberUsedInSignup
            )
        } ?: run {
            withContext(Dispatchers.Main.immediate) {
                linkAccountHolder.set(LinkAccountUpdate.Value(account = null))
                _consumerState.value = null
            }
            cachedShippingAddresses = null
            null
        }
    }

    private suspend fun LinkAccount?.fetchAccountStatus(shouldLookup: Boolean): AccountStatus =
        /**
         * If we already fetched an account, return its status, otherwise if a customer
         * email was passed in (and the user hasn't logged out) lookup the account.
         */
        this?.accountStatus
            ?: config.customerInfo.email
                ?.takeIf { shouldLookup }
                ?.let { customerEmail ->
                    lookupConsumer(customerEmail).map {
                        it?.accountStatus
                    }.getOrElse {
                        AccountStatus.Error
                    }
                } ?: AccountStatus.SignedOut

    private val SignUpConsentAction.consumerAction: ConsumerSignUpConsentAction
        get() = when (this) {
            SignUpConsentAction.Checkbox ->
                ConsumerSignUpConsentAction.Checkbox
            SignUpConsentAction.CheckboxWithPrefilledEmail ->
                ConsumerSignUpConsentAction.CheckboxWithPrefilledEmail
            SignUpConsentAction.CheckboxWithPrefilledEmailAndPhone ->
                ConsumerSignUpConsentAction.CheckboxWithPrefilledEmailAndPhone
            SignUpConsentAction.Implied ->
                ConsumerSignUpConsentAction.Implied
            SignUpConsentAction.ImpliedWithPrefilledEmail ->
                ConsumerSignUpConsentAction.ImpliedWithPrefilledEmail
            SignUpConsentAction.DefaultOptInWithAllPrefilled ->
                ConsumerSignUpConsentAction.PrecheckedOptInBoxPrefilledAll
            SignUpConsentAction.DefaultOptInWithSomePrefilled ->
                ConsumerSignUpConsentAction.PrecheckedOptInBoxPrefilledSome
            SignUpConsentAction.DefaultOptInWithNonePrefilled ->
                ConsumerSignUpConsentAction.PrecheckedOptInBoxPrefilledNone
        }
}
