package com.stripe.android.link.account

import androidx.annotation.VisibleForTesting
import com.stripe.android.core.exception.AuthenticationException
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.link.analytics.LinkEventsReporter
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.repositories.LinkRepository
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.ConsumerSignUpConsentAction
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.StripeIntent
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the Link account for the current user, persisting it across app usages.
 */
@Singleton
internal class LinkAccountManager @Inject constructor(
    private val config: LinkConfiguration,
    private val linkRepository: LinkRepository,
    private val cookieStore: CookieStore,
    private val linkEventsReporter: LinkEventsReporter
) {
    private val _linkAccount = MutableStateFlow<LinkAccount?>(null)
    var linkAccount: StateFlow<LinkAccount?> = _linkAccount

    /**
     * The publishable key for the signed in Link account.
     */
    var consumerPublishableKey: String? = null

    val accountStatus = linkAccount.transform { value ->
        emit(
            // If we already fetched an account, return its status
            value?.accountStatus
                ?: (
                    // If consumer has previously logged in, fetch their account
                    cookieStore.getAuthSessionCookie()?.let {
                        lookupConsumer(null).map {
                            it?.accountStatus
                        }.getOrElse {
                            AccountStatus.Error
                        }
                    }
                        // If the user recently signed up on this device, use their email
                        ?: cookieStore.getNewUserEmail()?.let {
                            lookupConsumer(it).map {
                                it?.accountStatus
                            }.getOrElse {
                                AccountStatus.Error
                            }
                        }
                        // If a customer email was passed in, lookup the account,
                        // unless the user has logged out of this account
                        ?: config.customerEmail?.let { customerEmail ->
                            if (hasUserLoggedOut(customerEmail)) {
                                AccountStatus.SignedOut
                            } else {
                                lookupConsumer(customerEmail).map {
                                    it?.accountStatus
                                }.getOrElse {
                                    AccountStatus.Error
                                }
                            }
                        } ?: AccountStatus.SignedOut
                    )
        )
    }

    /**
     * Keeps track of whether the user has logged out during this session. If that's the case, we
     * want to ignore the email passed in by the merchant to avoid confusion.
     */
    private var userHasLoggedOut = false

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
        email: String?,
        startSession: Boolean = true
    ): Result<LinkAccount?> =
        linkRepository.lookupConsumer(email, cookie())
            .also {
                if (it.isFailure) {
                    linkEventsReporter.onAccountLookupFailure()
                }
            }.map { consumerSessionLookup ->
                if (email == null && !consumerSessionLookup.exists) {
                    // Lookup with cookie-only failed, so cookie is invalid
                    cookieStore.updateAuthSessionCookie("")
                }

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
     * Registers the user for a new Link account and starts verification if needed.
     */
    suspend fun signUp(
        email: String,
        phone: String,
        country: String,
        name: String?,
        consentAction: ConsumerSignUpConsentAction
    ): Result<LinkAccount> =
        linkRepository.consumerSignUp(email, phone, country, name, cookie(), consentAction)
            .map { consumerSession ->
                cookieStore.storeNewUserEmail(email)
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
    ) = retryingOnAuthError { clientSecret ->
        linkRepository.createCardPaymentDetails(
            paymentMethodCreateParams,
            userEmail,
            stripeIntent,
            clientSecret,
            consumerPublishableKey
        )
    }

    /**
     * Make an API call with the client_secret for the currently signed in account, retrying once
     * if the call fails because of an [AuthenticationException].
     */
    private suspend fun <T> retryingOnAuthError(apiCall: suspend (String) -> Result<T>): Result<T> =
        linkAccount.value?.let { account ->
            apiCall(account.clientSecret).fold(
                onSuccess = {
                    Result.success(it)
                },
                onFailure = {
                    if (it is AuthenticationException && cookie() != null) {
                        // Try fetching the user account with the stored cookie, then retry API call
                        lookupConsumer(null).fold(
                            onSuccess = {
                                it?.let { updatedAccount ->
                                    apiCall(updatedAccount.clientSecret)
                                }
                            },
                            onFailure = {
                                Result.failure(it)
                            }
                        )
                    } else {
                        Result.failure(it)
                    }
                }
            )
        } ?: Result.failure(
            IllegalStateException("User not signed in")
        )

    /**
     * Logs the current consumer out.
     *
     * Regardless of the result of the API call, the local cookie is deleted and the current account
     * is cleared. This will effectively log the user out, so there's no need to wait for the result
     * of this call to consider it done.
     */
    fun logout() =
        linkAccount.value?.let { account ->
            val cookie = cookie()
            cookieStore.logout(account.email)
            userHasLoggedOut = true
            _linkAccount.value = null
            val publishableKey = consumerPublishableKey
            consumerPublishableKey = null
            GlobalScope.launch {
                linkRepository.logout(account.clientSecret, publishableKey, cookie)
            }
        }

    /**
     * Whether the user has logged out from any account during this session, or the last logout was
     * from the [email] passed as parameter, even if in a previous session.
     */
    fun hasUserLoggedOut(email: String?) = userHasLoggedOut ||
        (email?.let { cookieStore.isEmailLoggedOut(it) } ?: false)

    private fun setAccount(consumerSession: ConsumerSession): LinkAccount {
        maybeUpdateConsumerPublishableKey(consumerSession)
        val newAccount = LinkAccount(consumerSession)
        _linkAccount.value = newAccount
        cookieStore.updateAuthSessionCookie(newAccount.getAuthSessionCookie())
        if (cookieStore.isEmailLoggedOut(newAccount.email)) {
            cookieStore.storeLoggedOutEmail("")
        }
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

    private fun cookie() = cookieStore.getAuthSessionCookie()
}
