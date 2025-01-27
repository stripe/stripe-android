package com.stripe.android.link.account

import androidx.annotation.VisibleForTesting
import com.stripe.android.core.Logger
import com.stripe.android.core.exception.StripeException
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
import com.stripe.android.model.EmailSource
import com.stripe.android.model.IncentiveEligibilitySession
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.SetupIntent
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.BuildConfig
import com.stripe.android.paymentsheet.model.amount
import com.stripe.android.paymentsheet.model.currency
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Manages the Link account for the current user, persisting it across app usages.
 */
@SuppressWarnings("TooManyFunctions")
internal class DefaultLinkAccountManager @Inject constructor(
    private val config: LinkConfiguration,
    private val linkRepository: LinkRepository,
    private val linkEventsReporter: LinkEventsReporter,
    private val errorReporter: ErrorReporter,
) : LinkAccountManager {
    private val _linkAccount = MutableStateFlow<LinkAccount?>(null)
    override val linkAccount: StateFlow<LinkAccount?> = _linkAccount

    /**
     * The publishable key for the signed in Link account.
     */
    @Volatile
    @VisibleForTesting
    override var consumerPublishableKey: String? = null

    override val accountStatus = linkAccount.map { it.safeAccountStatus }

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
        return runCatching {
            val lookupResult = linkRepository.mobileLookupConsumer(
                verificationToken = verificationToken,
                appId = appId,
                email = email,
                sessionId = config.elementSessionId,
                emailSource = emailSource
            )
            val lookup = lookupResult.getOrThrow()
            setLinkAccountFromLookupResult(
                lookup = lookup,
                startSession = startSession
            )
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
            requireNotNull(linkAccount.value)
        }.mapCatching { account ->
            runCatching {
                linkRepository.logOut(
                    consumerSessionClientSecret = account.clientSecret,
                    consumerAccountPublishableKey = consumerPublishableKey,
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
        val currentAccount = _linkAccount.value
        val currentEmail = currentAccount?.email ?: config.customerInfo.email

        return when (val status = currentAccount.safeAccountStatus) {
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
                setAccountHelper(
                    consumerSession = consumerSessionSignup.consumerSession,
                    publishableKey = consumerSessionSignup.publishableKey,
                )
            }

    override suspend fun mobileSignUp(
        email: String,
        phone: String,
        country: String,
        name: String?,
        emailSource: EmailSource,
        verificationToken: String,
        appId: String,
        consentAction: SignUpConsentAction
    ): Result<LinkAccount> {
        return runCatching {
            val consumerSessionSignUpResult = linkRepository.mobileSignUp(
                name = name,
                email = email,
                phoneNumber = phone,
                country = country,
                consentAction = consentAction.consumerAction,
                verificationToken = verificationToken,
                appId = appId,
                amount = config.stripeIntent.amount,
                currency = config.stripeIntent.currency,
                incentiveEligibilitySession = incentiveEligibilitySession(),
            )
            val consumerSessionSignUp = consumerSessionSignUpResult.getOrThrow()
            setAccountHelper(
                consumerSession = consumerSessionSignUp.consumerSession,
                publishableKey = consumerSessionSignUp.publishableKey,
            )
        }
    }

    override suspend fun createCardPaymentDetails(
        paymentMethodCreateParams: PaymentMethodCreateParams
    ): Result<LinkPaymentDetails> {
        val linkAccountValue = linkAccount.value
        return if (linkAccountValue != null) {
            linkAccountValue.let { account ->
                linkRepository.createCardPaymentDetails(
                    paymentMethodCreateParams = paymentMethodCreateParams,
                    userEmail = account.email,
                    stripeIntent = config.stripeIntent,
                    consumerSessionClientSecret = account.clientSecret,
                    consumerPublishableKey = if (config.passthroughModeEnabled) null else consumerPublishableKey,
                    active = config.passthroughModeEnabled,
                ).mapCatching {
                    if (config.passthroughModeEnabled) {
                        linkRepository.shareCardPaymentDetails(
                            id = it.paymentDetails.id,
                            last4 = paymentMethodCreateParams.cardLast4().orEmpty(),
                            consumerSessionClientSecret = account.clientSecret,
                            paymentMethodCreateParams = paymentMethodCreateParams,
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

    private fun setAccountHelper(
        consumerSession: ConsumerSession,
        publishableKey: String?,
    ): LinkAccount {
        maybeUpdateConsumerPublishableKey(consumerSession.emailAddress, publishableKey)
        val newAccount = LinkAccount(consumerSession)
        _linkAccount.value = newAccount
        return newAccount
    }

    override fun setLinkAccountFromLookupResult(
        lookup: ConsumerSessionLookup,
        startSession: Boolean,
    ): LinkAccount? {
        return lookup.consumerSession?.let { consumerSession ->
            if (startSession) {
                setAccount(
                    consumerSession = consumerSession,
                    publishableKey = lookup.publishableKey,
                )
            } else {
                LinkAccount(consumerSession)
            }
        }
    }

    override suspend fun startVerification(): Result<LinkAccount> {
        val clientSecret = linkAccount.value?.clientSecret ?: return Result.failure(Throwable("no link account found"))
        linkEventsReporter.on2FAStart()
        return linkRepository.startVerification(clientSecret, consumerPublishableKey)
            .onFailure {
                linkEventsReporter.on2FAStartFailure()
            }.map { consumerSession ->
                setAccountHelper(consumerSession, null)
            }
    }

    override suspend fun confirmVerification(code: String): Result<LinkAccount> {
        val clientSecret = linkAccount.value?.clientSecret ?: return Result.failure(Throwable("no link account found"))
        return linkRepository.confirmVerification(code, clientSecret, consumerPublishableKey)
            .onSuccess {
                linkEventsReporter.on2FAComplete()
            }.onFailure {
                linkEventsReporter.on2FAFailure()
            }.map { consumerSession ->
                setAccountHelper(consumerSession, null)
            }
    }

    override suspend fun listPaymentDetails(paymentMethodTypes: Set<String>): Result<ConsumerPaymentDetails> {
        val clientSecret = linkAccount.value?.clientSecret ?: return Result.failure(NoLinkAccountFoundException())
        return linkRepository.listPaymentDetails(
            paymentMethodTypes = paymentMethodTypes,
            consumerSessionClientSecret = clientSecret,
            consumerPublishableKey = consumerPublishableKey
        )
    }

    override suspend fun deletePaymentDetails(paymentDetailsId: String): Result<Unit> {
        val clientSecret = linkAccount.value?.clientSecret ?: return Result.failure(NoLinkAccountFoundException())
        return linkRepository.deletePaymentDetails(
            paymentDetailsId = paymentDetailsId,
            consumerSessionClientSecret = clientSecret,
            consumerPublishableKey = consumerPublishableKey
        )
    }

    override suspend fun updatePaymentDetails(
        updateParams: ConsumerPaymentDetailsUpdateParams
    ): Result<ConsumerPaymentDetails> {
        val clientSecret = linkAccount.value?.clientSecret ?: return Result.failure(NoLinkAccountFoundException())
        return linkRepository.updatePaymentDetails(
            updateParams = updateParams,
            consumerSessionClientSecret = clientSecret,
            consumerPublishableKey = consumerPublishableKey
        )
    }

    private fun setAccount(
        consumerSession: ConsumerSession?,
        publishableKey: String?,
    ): LinkAccount? {
        return consumerSession?.let {
            setAccountHelper(consumerSession = it, publishableKey = publishableKey)
        } ?: run {
            _linkAccount.value = null
            consumerPublishableKey = null
            null
        }
    }

    /**
     * Update the [consumerPublishableKey] value if needed.
     *
     * Only calls to [lookupConsumer] and [signUp] return the publishable key. For other calls, we
     * want to keep using the current key unless the user signed out.
     */
    private fun maybeUpdateConsumerPublishableKey(
        newEmail: String,
        publishableKey: String?,
    ) {
        if (publishableKey != null) {
            // If the session has a key, start using it
            consumerPublishableKey = publishableKey
        } else {
            // Keep the current key if it's the same user, reset it if the user changed
            if (_linkAccount.value?.email != newEmail) {
                consumerPublishableKey = null
            }
        }
    }

    private fun incentiveEligibilitySession(): IncentiveEligibilitySession? {
        val id = config.stripeIntent.id ?: return null
        if (config.stripeIntent.clientSecret == null) {
            return IncentiveEligibilitySession.DeferredIntent(id)
        }
        return when (config.stripeIntent) {
            is PaymentIntent -> IncentiveEligibilitySession.PaymentIntent(id)
            is SetupIntent -> IncentiveEligibilitySession.SetupIntent(id)
        }
    }

    private val LinkAccount?.safeAccountStatus
        get() = this?.accountStatus ?: AccountStatus.SignedOut
}
