package com.stripe.android.financialconnections.features.common

import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.SynchronizeSessionResponse

/**
 * Get the business name from the manifest.
 * If the business name is not available, use the connect platform name.
 */
internal fun FinancialConnectionsSessionManifest.getBusinessName(): String? {
    return businessName ?: connectPlatformName
}

/**
 * Mask the email address to show only the first [EMAIL_LENGTH] characters.
 */
internal fun FinancialConnectionsSessionManifest.getRedactedEmail(): String? =
    accountholderCustomerEmailAddress?.let { email ->
        val content = email.split('@')[0]
        return if (content.length <= EMAIL_LENGTH) {
            email
        } else {
            val domain = email.split('@')[1]
            content.substring(0, EMAIL_LENGTH) + "•••@" + domain
        }
    }

internal fun SynchronizeSessionResponse.showManualEntryInErrors(): Boolean {
    return manifest.allowManualEntry && visual.reducedManualEntryProminenceForErrors.not()
}

private const val EMAIL_LENGTH = 15
