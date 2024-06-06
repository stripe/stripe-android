package com.stripe.android.link.account

import androidx.annotation.VisibleForTesting
import com.stripe.android.core.Logger
import com.stripe.android.core.exception.StripeException
import com.stripe.android.link.BuildConfig
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.link.analytics.LinkEventsReporter
import com.stripe.android.link.injection.LinkScope
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.repositories.LinkRepository
import com.stripe.android.link.ui.inline.SignUpConsentAction
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.ConsumerSignUpConsentAction
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.payments.core.analytics.ErrorReporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Manages the Link account for the current user, persisting it across app usages.
 */
@LinkScope
internal class LinkAccountManager @Inject constructor(
    private val config: LinkConfiguration,
    private val linkRepository: LinkRepository,
    private val linkEventsReporter: LinkEventsReporter,
    private val errorReporter: ErrorReporter,
) {
    private val _linkAccount = MutableStateFlow<LinkAccount?>(null)
    val linkAccount: StateFlow<LinkAccount?> = _linkAccount

    /**
     * The publishable key for the signed in Link account.
     */
    @Volatile
    @VisibleForTesting
    var consumerPublishableKey: String? = null

    val accountStatus = linkAccount.map { it.fetchAccountStatus() }

    /**
     * Retrieves the Link account associated with the email if it exists.
     *
     * Optionally starts a user session, by storing the cookie for the account and starting a
     * verification if needed.
     *
     * When the [email] parameter is null, will try to fetch the account for the currently stored
     * cookie.
     */
    suspend fun lookupConsumer(
        email: String,
        startSession: Boolean = true,
    ): Result<LinkAccount?> =
        linkRepository.lookupConsumer(email)
            .onFailure { error ->
                linkEventsReporter.onAccountLookupFailure(error)
            }.map { consumerSessionLookup ->
                consumerSessionLookup.consumerSession?.let { consumerSession ->
                    if (startSession) {
                        setAccountNullable(consumerSession)
                    } else {
                        LinkAccount(consumerSession)
                    }
                }
            }

    /**
     * Use the user input in memory to sign in to an existing account or sign up for a new Link
     * account, starting verification if needed.
     */
    suspend fun signInWithUserInput(
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

    suspend fun logOut(): Result<ConsumerSession> {
        return runCatching {
            requireNotNull(linkAccount.value)
        }.mapCatching { account ->
            linkRepository.logOut(
                consumerSessionClientSecret = account.clientSecret,
                consumerAccountPublishableKey = consumerPublishableKey,
            ).getOrThrow()
        }.onSuccess {
            errorReporter.report(ErrorReporter.SuccessEvent.LINK_LOG_OUT_SUCCESS)
            Logger.getInstance(BuildConfig.DEBUG).debug("Logged out of Link successfully")
        }.onFailure { error ->
            errorReporter.report(ErrorReporter.ExpectedErrorEvent.LINK_LOG_OUT_FAILURE, StripeException.create(error))
            Logger.getInstance(BuildConfig.DEBUG).warning("Failed to log out of Link: $error")
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

        return when (val status = currentAccount.fetchAccountStatus()) {
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

    /**
     * Registers the user for a new Link account.
     */
    private suspend fun signUp(
        email: String,
        phone: String,
        country: String,
        name: String?,
        consentAction: SignUpConsentAction
    ): Result<LinkAccount> =
        linkRepository.consumerSignUp(email, phone, country, name, consentAction.consumerAction)
            .map { consumerSession ->
                setAccount(consumerSession)
            }

    /**
     * Creates a new PaymentDetails.Card attached to the current account.
     *
     * @return The parameters needed to confirm the current Stripe Intent using the newly created
     *          Payment Details.
     */
    suspend fun createCardPaymentDetails(
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

    private fun setAccount(consumerSession: ConsumerSession): LinkAccount {
        maybeUpdateConsumerPublishableKey(consumerSession)
        val newAccount = LinkAccount(consumerSession)
        _linkAccount.value = newAccount
        return newAccount
    }

    @VisibleForTesting
    fun setAccountNullable(consumerSession: ConsumerSession?): LinkAccount? =
        consumerSession?.let {
            setAccount(it)
        } ?: run {
            _linkAccount.value = null
            consumerPublishableKey = null
            null
        }

    /**
     * Update the [consumerPublishableKey] value if needed.
     *
     * Only calls to [lookupConsumer] and [signUp] return the publishable key. For other calls, we
     * want to keep using the current key unless the user signed out.
     */
    private fun maybeUpdateConsumerPublishableKey(newSession: ConsumerSession) {
        newSession.publishableKey?.let {
            // If the session has a key, start using it
            consumerPublishableKey = it
        } ?: run {
            // Keep the current key if it's the same user, reset it if the user changed
            if (_linkAccount.value?.email != newSession.emailAddress) {
                consumerPublishableKey = null
            }
        }
    }

    private suspend fun LinkAccount?.fetchAccountStatus(): AccountStatus =
        /**
         * If we already fetched an account, return its status, otherwise if a customer email was passed in,
         * lookup the account.
         */
        this?.accountStatus
            ?: config.customerInfo.email?.let { customerEmail ->
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
        }
}
