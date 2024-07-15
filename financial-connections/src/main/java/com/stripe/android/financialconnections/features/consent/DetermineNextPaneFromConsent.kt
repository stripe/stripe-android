package com.stripe.android.financialconnections.features.consent

import com.stripe.android.financialconnections.domain.LookupAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import javax.inject.Inject

internal class DetermineNextPaneFromConsent @Inject constructor(
    private val lookupAccount: LookupAccount,
) {

    suspend operator fun invoke(
        manifest: FinancialConnectionsSessionManifest,
    ): Pane {
        val emailForInstantDebits = manifest.customerEmailAddressForInstantDebits

        val customNextPane = emailForInstantDebits?.let { email ->
            val isReturningLinkConsumer = isReturningLinkConsumer(email)
            if (isReturningLinkConsumer) {
                Pane.NETWORKING_LINK_LOGIN_WARMUP
            } else {
                null
            }
        }

        return customNextPane ?: manifest.nextPane
    }

    private suspend fun isReturningLinkConsumer(email: String): Boolean {
        return runCatching { lookupAccount(email).exists }.getOrDefault(false)
    }
}

private val FinancialConnectionsSessionManifest.customerEmailAddressForInstantDebits: String?
    get() {
        return accountholderCustomerEmailAddress?.takeIf {
            isLinkWithStripe ?: false
        }
    }
