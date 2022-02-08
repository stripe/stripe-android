package com.stripe.android.link

import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.repositories.LinkRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the Link account for the current user, persisting it across app usages.
 */
@Singleton
internal class LinkAccountManager @Inject constructor(
    private val linkRepository: LinkRepository
) {
    // TODO(brnunes-stripe): Persist the account.
    var linkAccount: LinkAccount? = null
        private set

    /**
     * Retrieves the Link account associated with the email and starts verification, if needed.
     */
    suspend fun lookupConsumer(email: String): Result<LinkAccount?> =
        linkRepository.lookupConsumer(email).map { consumerSessionLookup ->
            setAndReturnNullable(consumerSessionLookup.consumerSession?.let { consumerSession ->
                LinkAccount(consumerSession)
            })
        }.mapCatching {
            it?.let { account ->
                if (account.isVerified) {
                    account
                } else {
                    setAndReturn(
                        LinkAccount(
                            linkRepository.startVerification(account.clientSecret).getOrThrow()
                        )
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
        linkRepository.consumerSignUp(email, phone, country).map { consumerSession ->
            setAndReturn(LinkAccount(consumerSession))
        }.mapCatching { account ->
            if (account.isVerified) {
                account
            } else {
                setAndReturn(
                    LinkAccount(
                        linkRepository.startVerification(account.clientSecret).getOrThrow()
                    )
                )
            }
        }

    suspend fun confirmVerification(code: String): Result<LinkAccount> =
        linkAccount?.let { account ->
            linkRepository.confirmVerification(account.clientSecret, code).map { consumerSession ->
                setAndReturn(LinkAccount(consumerSession))
            }
        } ?: Result.failure(IllegalStateException("Confirming verification for null account"))

    private fun setAndReturn(linkAccount: LinkAccount): LinkAccount {
        this.linkAccount = linkAccount
        return linkAccount
    }

    private fun setAndReturnNullable(linkAccount: LinkAccount?): LinkAccount? {
        this.linkAccount = linkAccount
        return linkAccount
    }
}
