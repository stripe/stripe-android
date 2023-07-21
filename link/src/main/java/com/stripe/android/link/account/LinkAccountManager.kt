package com.stripe.android.link.account

import androidx.annotation.VisibleForTesting
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.link.analytics.LinkEventsReporter
import com.stripe.android.link.injection.LinkScope
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.repositories.LinkRepository
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.ConsumerSignUpConsentAction
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.StripeIntent
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
    private val linkEventsReporter: LinkEventsReporter
) {
    private val _linkAccount = MutableStateFlow<LinkAccount?>(null)
    val linkAccount: StateFlow<LinkAccount?> = _linkAccount

    @Volatile
    @VisibleForTesting
    var authSessionCookie: String? = null

    /**
     * The publishable key for the signed in Link account.
     */
    @Volatile
    @VisibleForTesting
    var consumerPublishableKey: String? = null

    val accountStatus = linkAccount.map { value ->
        // If we already fetched an account, return its status
        value?.accountStatus
            // If a customer email was passed in, lookup the account.
            ?: config.customerEmail?.let { customerEmail ->
                lookupConsumer(customerEmail).map {
                    it?.accountStatus
                }.getOrElse {
                    AccountStatus.Error
                }
            } ?: AccountStatus.SignedOut
    }

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
        linkRepository.lookupConsumer(email, authSessionCookie)
            .also {
                if (it.isFailure) {
                    linkEventsReporter.onAccountLookupFailure()
                }
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
    suspend fun signInWithUserInput(userInput: UserInput): Result<LinkAccount> =
        when (userInput) {
            is UserInput.SignIn -> lookupConsumer(userInput.email).mapCatching {
                requireNotNull(it) { "Error fetching user account" }
            }

            is UserInput.SignUp -> signUp(
                email = userInput.email,
                phone = userInput.phone,
                country = userInput.country,
                name = userInput.name,
                consentAction = ConsumerSignUpConsentAction.Checkbox
            ).also {
                if (it.isSuccess) {
                    linkEventsReporter.onSignupCompleted(true)
                } else {
                    linkEventsReporter.onSignupFailure(true)
                }
            }
        }

    /**
     * Registers the user for a new Link account.
     */
    suspend fun signUp(
        email: String,
        phone: String,
        country: String,
        name: String?,
        consentAction: ConsumerSignUpConsentAction
    ): Result<LinkAccount> =
        linkRepository.consumerSignUp(email, phone, country, name, authSessionCookie, consentAction)
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
    ): Result<LinkPaymentDetails.New> =
        linkAccount.value?.let { account ->
            createCardPaymentDetails(
                paymentMethodCreateParams,
                account.email,
                config.stripeIntent
            )
        } ?: Result.failure(
            IllegalStateException("A non-null Link account is needed to create payment details")
        )

    /**
     * Create a new Card payment method attached to the consumer account.
     */
    suspend fun createCardPaymentDetails(
        paymentMethodCreateParams: PaymentMethodCreateParams,
        userEmail: String,
        stripeIntent: StripeIntent
    ): Result<LinkPaymentDetails.New> = linkAccount.value?.let { account ->
        linkRepository.createCardPaymentDetails(
            paymentMethodCreateParams,
            userEmail,
            stripeIntent,
            account.clientSecret,
            consumerPublishableKey
        )
    } ?: Result.failure(
        IllegalStateException("User not signed in")
    )

    private fun setAccount(consumerSession: ConsumerSession): LinkAccount {
        maybeUpdateConsumerPublishableKey(consumerSession)
        val newAccount = LinkAccount(consumerSession)
        _linkAccount.value = newAccount
        authSessionCookie = newAccount.getAuthSessionCookie()
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
}
