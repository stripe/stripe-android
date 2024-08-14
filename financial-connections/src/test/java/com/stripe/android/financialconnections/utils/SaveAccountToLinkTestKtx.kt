package com.stripe.android.financialconnections.utils

import com.stripe.android.financialconnections.domain.CachedPartnerAccount
import com.stripe.android.financialconnections.domain.SaveAccountToLink
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest

@Suppress("TestFunctionName")
internal fun SaveAccountToLink(
    action: () -> FinancialConnectionsSessionManifest,
): SaveAccountToLink {
    return object : SaveAccountToLink {

        override suspend fun new(
            email: String,
            phoneNumber: String,
            selectedAccounts: List<CachedPartnerAccount>,
            country: String,
            shouldPollAccountNumbers: Boolean
        ): FinancialConnectionsSessionManifest {
            return action()
        }

        override suspend fun existing(
            consumerSessionClientSecret: String,
            selectedAccounts: List<CachedPartnerAccount>?,
            shouldPollAccountNumbers: Boolean
        ): FinancialConnectionsSessionManifest {
            return action()
        }
    }
}
