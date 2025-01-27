package com.stripe.android.link.account

import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.model.LinkAccount
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class AutoLoginLinkAccountManager @Inject constructor(
    private val configuration: LinkConfiguration,
    private val defaultLinkAccountManager: LinkAccountManager
) : LinkAccountManager by defaultLinkAccountManager {
    override val accountStatus = linkAccount.map { it.fetchAccountStatus() }

    private suspend fun LinkAccount?.fetchAccountStatus(): AccountStatus {
        if (this != null) return accountStatus

        val customerEmail = configuration.customerInfo.email ?: return AccountStatus.SignedOut

        val lookupAccountStatus = lookupConsumer(
            email = customerEmail,
            startSession = true
        ).map {
            it?.accountStatus
        }.getOrElse {
            AccountStatus.Error
        }
        return lookupAccountStatus ?: AccountStatus.SignedOut
    }
}