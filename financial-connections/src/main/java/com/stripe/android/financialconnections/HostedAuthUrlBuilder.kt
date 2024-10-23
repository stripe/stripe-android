package com.stripe.android.financialconnections

import com.stripe.android.core.networking.QueryStringFactory
import com.stripe.android.financialconnections.FinancialConnectionsSheet.ElementsSessionContext.BillingAddress
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.model.LinkMode

internal object HostedAuthUrlBuilder {

    fun create(
        args: FinancialConnectionsSheetActivityArgs,
        manifest: FinancialConnectionsSessionManifest,
    ): String? {
        return create(
            hostedAuthUrl = manifest.hostedAuthUrl,
            isInstantDebits = args is FinancialConnectionsSheetActivityArgs.ForInstantDebits,
            linkMode = args.elementsSessionContext?.linkMode,
            billingAddress = args.elementsSessionContext?.billingAddress,
        )
    }

    private fun create(
        hostedAuthUrl: String?,
        isInstantDebits: Boolean,
        linkMode: LinkMode?,
        billingAddress: BillingAddress?,
    ): String? {
        if (hostedAuthUrl == null) {
            return null
        }

        val queryParams = mutableListOf(hostedAuthUrl)

        if (isInstantDebits) {
            // For Instant Debits, add a query parameter to the hosted auth URL so that payment account creation
            // takes place on the web side of the flow and the payment method ID is returned to the app.
            queryParams.add("return_payment_method=true")
            billingAddress?.let { queryParams.add(makeBillingAddressQueryParams(it)) }
            linkMode?.let { queryParams.add("link_mode=${it.value}") }
        }

        return queryParams.joinToString("&")
    }

    private fun makeBillingAddressQueryParams(billingAddress: BillingAddress): String {
        val params = mapOf(
            "billing_address" to billingAddress.apiParams(),
        )
        return QueryStringFactory.createFromParamsWithEmptyValues(params)
    }
}
