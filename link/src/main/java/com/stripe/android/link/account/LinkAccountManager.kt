package com.stripe.android.link.account

import androidx.annotation.VisibleForTesting
import com.stripe.android.core.exception.AuthenticationException
import com.stripe.android.link.LinkActivityContract
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.repositories.LinkRepository
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.link.ui.paymentmethod.SupportedPaymentMethod
import com.stripe.android.model.ConsumerPaymentDetailsCreateParams
import com.stripe.android.model.ConsumerPaymentDetailsUpdateParams
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
    private val args: LinkActivityContract.Args,
    private val linkRepository: LinkRepository,
    private val cookieStore: CookieStore
) {
    private val _linkAccount = MutableStateFlow<LinkAccount?>(null)
    var linkAccount: StateFlow<LinkAccount?> = _linkAccount

    val accountStatus = linkAccount.transform { value ->
        emit(
            // If we already fetched an account, return its status
            value?.accountStatus
                ?: (
                    // If consumer has previously logged in, fetch their account
                    cookieStore.getAuthSessionCookie()?.let {
                        lookupConsumer(null).getOrNull()?.accountStatus
                    }
                        // If a customer email was passed in, lookup the account,
                        // unless the user has logged out of this account
                        ?: args.customerEmail?.let {
                            if (hasUserLoggedOut(it)) {
                                AccountStatus.SignedOut
                            } else {
                                lookupConsumer(args.customerEmail).getOrNull()?.accountStatus
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
            .map { consumerSessionLookup ->
                consumerSessionLookup.consumerSession?.let { consumerSession ->
                    LinkAccount(consumerSession)
                }
            }.map { account ->
                if (startSession) {
                    setAndReturnNullable(account)
                } else {
                    account
                }
            }.mapCatching {
                it?.let { account ->
                    if (startSession && !account.isVerified) {
                        setAndReturn(
                            LinkAccount(
                                linkRepository
                                    .startVerification(account.clientSecret, cookie())
                                    .getOrThrow()
                            )
                        )
                    } else {
                        account
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
            is UserInput.SignUp -> signUp(userInput.email, userInput.phone, userInput.country)
        }

    /**
     * Registers the user for a new Link account and starts verification if needed.
     */
    suspend fun signUp(
        email: String,
        phone: String,
        country: String
    ): Result<LinkAccount> =
        linkRepository.consumerSignUp(email, phone, country, cookie())
            .map { consumerSession ->
                setAndReturn(LinkAccount(consumerSession))
            }.mapCatching { account ->
                if (account.isVerified) {
                    account
                } else {
                    setAndReturn(
                        LinkAccount(
                            linkRepository.startVerification(account.clientSecret, cookie())
                                .getOrThrow()
                        )
                    )
                }
            }

    /**
     * Triggers sending a verification code to the user.
     */
    suspend fun startVerification(): Result<LinkAccount> = retryingOnAuthError {
        linkRepository.startVerification(it, cookie())
            .map { consumerSession ->
                setAndReturn(LinkAccount(consumerSession))
            }
    }

    /**
     * Confirms a verification code sent to the user.
     */
    suspend fun confirmVerification(code: String): Result<LinkAccount> = retryingOnAuthError {
        linkRepository.confirmVerification(it, code, cookie())
            .map { consumerSession ->
                setAndReturn(LinkAccount(consumerSession))
            }
    }

    /**
     * Fetch all saved payment methods for the signed in consumer.
     */
    suspend fun listPaymentDetails() = retryingOnAuthError {
        linkRepository.listPaymentDetails(it)
    }

    /**
     * Creates a new PaymentDetails attached to the current account.
     *
     * @return The parameters needed to confirm the current Stripe Intent using the newly created
     *          Payment Details.
     */
    suspend fun createPaymentDetails(
        paymentMethod: SupportedPaymentMethod,
        paymentMethodCreateParams: PaymentMethodCreateParams
    ): Result<LinkPaymentDetails> =
        linkAccount.value?.let { account ->
            createPaymentDetails(
                paymentMethod.createParams(paymentMethodCreateParams, account.email),
                args.stripeIntent,
                paymentMethod.extraConfirmationParams(paymentMethodCreateParams)
            )
        } ?: Result.failure(
            IllegalStateException("A non-null Link account is needed to create payment details")
        )

    /**
     * Create a new payment method in the signed in consumer account.
     */
    suspend fun createPaymentDetails(
        paymentDetails: ConsumerPaymentDetailsCreateParams,
        stripeIntent: StripeIntent,
        extraConfirmationParams: Map<String, Any>?
    ) = retryingOnAuthError {
        linkRepository.createPaymentDetails(
            paymentDetails,
            it,
            stripeIntent,
            extraConfirmationParams
        )
    }

    /**
     * Update an existing payment method in the signed in consumer account.
     */
    suspend fun updatePaymentDetails(updateParams: ConsumerPaymentDetailsUpdateParams) =
        retryingOnAuthError {
            linkRepository.updatePaymentDetails(updateParams, it)
        }

    /**
     * Delete the payment method from the signed in consumer account.
     */
    suspend fun deletePaymentDetails(paymentDetailsId: String) = retryingOnAuthError {
        linkRepository.deletePaymentDetails(it, paymentDetailsId)
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
                    if (it is AuthenticationException) {
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
            GlobalScope.launch {
                linkRepository.logout(account.clientSecret, cookie)
            }
        }

    /**
     * Whether the user has logged out from any account during this session, or the last logout was
     * from the [email] passed as parameter, even if in a previous session.
     */
    fun hasUserLoggedOut(email: String?) = userHasLoggedOut ||
        (email?.let { cookieStore.isEmailLoggedOut(it) } ?: false)

    private fun setAndReturn(linkAccount: LinkAccount): LinkAccount {
        _linkAccount.value = linkAccount
        cookieStore.updateAuthSessionCookie(linkAccount.getAuthSessionCookie())
        if (cookieStore.isEmailLoggedOut(linkAccount.email)) {
            cookieStore.storeLoggedOutEmail("")
        }
        return linkAccount
    }

    @VisibleForTesting
    fun setAndReturnNullable(linkAccount: LinkAccount?): LinkAccount? =
        linkAccount?.let {
            setAndReturn(it)
        } ?: run {
            _linkAccount.value = null
            null
        }

    private fun cookie() = cookieStore.getAuthSessionCookie()
}
