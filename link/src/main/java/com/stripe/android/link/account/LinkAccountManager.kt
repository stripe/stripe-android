package com.stripe.android.link.account

import com.stripe.android.link.LinkActivityContract
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.repositories.LinkRepository
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
    args: LinkActivityContract.Args,
    private val linkRepository: LinkRepository,
    private val cookieStore: CookieStore
) {
    private val _linkAccount = MutableStateFlow<LinkAccount?>(null)
    var linkAccount: StateFlow<LinkAccount?> = _linkAccount

    val accountStatus =
        linkAccount.transform { value ->
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
     * Retrieves the Link account associated with the email and optionally starts verification, if
     * needed.
     * When the [email] parameter is null, will lookup the account for the currently stored cookie.
     */
    suspend fun lookupConsumer(
        email: String?,
        startVerification: Boolean = true
    ): Result<LinkAccount?> =
        linkRepository.lookupConsumer(email, cookie())
            .map { consumerSessionLookup ->
                setAndReturnNullable(
                    consumerSessionLookup.consumerSession?.let { consumerSession ->
                        LinkAccount(consumerSession)
                    }
                )
            }.mapCatching {
                it?.let { account ->
                    if (account.isVerified) {
                        account
                    } else {
                        setAndReturn(
                            if (startVerification) {
                                LinkAccount(
                                    linkRepository.startVerification(account.clientSecret, cookie())
                                        .getOrThrow()
                                )
                            } else {
                                account
                            }
                        )
                    }
                }
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
    suspend fun startVerification(): Result<LinkAccount> =
        linkAccount.value?.let { account ->
            linkRepository.startVerification(account.clientSecret, cookie())
                .map { consumerSession ->
                    setAndReturn(LinkAccount(consumerSession))
                }
        } ?: Result.failure(
            IllegalStateException("A non-null Link account is needed to start verification")
        )

    /**
     * Confirms a verification code sent to the user.
     */
    suspend fun confirmVerification(code: String): Result<LinkAccount> =
        linkAccount.value?.let { account ->
            linkRepository.confirmVerification(account.clientSecret, code, cookie())
                .map { consumerSession ->
                    setAndReturn(LinkAccount(consumerSession))
                }
        } ?: Result.failure(
            IllegalStateException("A non-null Link account is needed to confirm verification")
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
            _linkAccount.value = null
            userHasLoggedOut = true
            GlobalScope.launch {
                linkRepository.logout(account.clientSecret, cookie)
            }
        }

    /**
     * Whether the user has logged out from any account during this session, or the last logout was
     * from the [email] passed as parameter, even if in a previous session.
     */
    fun hasUserLoggedOut(email: String?) = userHasLoggedOut || email?.let {
        cookieStore.isEmailLoggedOut(it)
    } ?: false

    private fun setAndReturn(linkAccount: LinkAccount): LinkAccount {
        _linkAccount.value = linkAccount
        cookieStore.updateAuthSessionCookie(linkAccount.getAuthSessionCookie())
        if (cookieStore.isEmailLoggedOut(linkAccount.email)) {
            cookieStore.storeLoggedOutEmail("")
        }
        return linkAccount
    }

    private fun setAndReturnNullable(linkAccount: LinkAccount?): LinkAccount? =
        linkAccount?.let {
            setAndReturn(it)
        } ?: run {
            _linkAccount.value = null
            null
        }

    private fun cookie() = cookieStore.getAuthSessionCookie()
}
