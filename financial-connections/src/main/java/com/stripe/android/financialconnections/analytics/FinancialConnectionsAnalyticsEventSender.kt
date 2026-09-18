package com.stripe.android.financialconnections.analytics

import android.content.Context
import com.stripe.android.core.networking.AnalyticsRequestV2Executor
import com.stripe.android.core.networking.AnalyticsRequestV2Factory
import com.stripe.android.financialconnections.FinancialConnectionsSheetConfiguration
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import java.util.Locale

internal fun interface FinancialConnectionsAnalyticsEventSender {
    suspend fun send(event: FinancialConnectionsAnalyticsEvent, manifest: FinancialConnectionsSessionManifest)
}

internal class DefaultFinancialConnectionsAnalyticsEventSender(
    configuration: FinancialConnectionsSheetConfiguration,
    locale: Locale,
    context: Context,
    private val requestExecutor: AnalyticsRequestV2Executor
) : FinancialConnectionsAnalyticsEventSender {

    private val requestFactory = AnalyticsRequestV2Factory(
        context = context,
        clientId = FinancialConnectionsAnalyticsTrackerImpl.CLIENT_ID,
        origin = FinancialConnectionsAnalyticsTrackerImpl.ORIGIN
    )

    private val configurationParams = mapOf(
        "key" to configuration.publishableKey,
        "stripe_account" to configuration.stripeAccountId,
        "navigator_language" to locale.toLanguageTag(),
        "is_webview" to false.toString()
    )

    override suspend fun send(
        event: FinancialConnectionsAnalyticsEvent,
        manifest: FinancialConnectionsSessionManifest
    ) {
        val commonParams = configurationParams + mapOf(
            "las_id" to manifest.id,
            "livemode" to manifest.livemode.toString(),
            "product" to manifest.product.value,
            "is_stripe_direct" to manifest.isStripeDirect.toString(),
            "single_account" to manifest.singleAccount.toString(),
            "allow_manual_entry" to manifest.allowManualEntry.toString(),
            "app_verification_enabled" to manifest.appVerificationEnabled.toString(),
            "account_holder_id" to manifest.accountholderToken
        )
        val request = requestFactory.createRequest(
            eventName = event.eventName,
            additionalParams = event.params.orEmpty() + commonParams,
            includeSDKParams = true
        )
        requestExecutor.enqueue(request)
    }
}
