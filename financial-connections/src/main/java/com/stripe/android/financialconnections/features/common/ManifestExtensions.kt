package com.stripe.android.financialconnections.features.common

import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.SynchronizeSessionResponse

internal val FinancialConnectionsSessionManifest.isDataFlow: Boolean
    get() = paymentMethodType == null

internal val FinancialConnectionsSessionManifest.canSaveAccountsToLink: Boolean
    get() = accountholderIsLinkConsumer == true && isNetworkingUserFlow == true

/**
 * Get the business name from the manifest.
 * If the business name is not available, use the connect platform name.
 */
internal fun FinancialConnectionsSessionManifest.getBusinessName(): String? {
    return businessName ?: connectPlatformName
}

internal fun FinancialConnectionsSessionManifest.enableRetrieveAuthSession(): Boolean =
    features
        ?.get("bank_connections_disable_defensive_auth_session_retrieval_on_complete") != true

internal fun FinancialConnectionsSessionManifest.useContinueWithMerchantText(): Boolean =
    features
        ?.get("bank_connections_continue_with_merchant_text") == true

internal fun FinancialConnectionsSessionManifest.enableWorkManager(): Boolean {
    return features?.get("bank_connections_android_enable_work_manager") == true
}

internal fun SynchronizeSessionResponse.showManualEntryInErrors(): Boolean {
    return manifest.allowManualEntry && visual.reducedManualEntryProminenceInErrors.not()
}
