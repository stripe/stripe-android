package com.stripe.android.link.account

import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.model.AccountStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

internal class AutoLoginLinkAccountManager @Inject constructor(
    private val configuration: LinkConfiguration,
    private val linkAccountManager: DefaultLinkAccountManager
): LinkAccountManager by linkAccountManager {
    override val accountStatus: Flow<AccountStatus> = linkAccount.map { account ->
        if (account != null) return@map account.accountStatus

        val customerEmail = configuration.customerInfo.email
            ?: return@map AccountStatus.SignedOut

        val lookupResult = lookupConsumer(customerEmail)
        lookupResult.getOrNull()?.accountStatus ?: AccountStatus.Error
    }
}