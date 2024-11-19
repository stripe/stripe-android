package com.stripe.android.financialconnections

import com.stripe.android.core.networking.QueryStringFactory
import com.stripe.android.financialconnections.FinancialConnectionsSheet.ElementsSessionContext.BillingDetails
import com.stripe.android.financialconnections.FinancialConnectionsSheet.ElementsSessionContext.PrefillDetails
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.utils.toApiParams
import com.stripe.android.model.LinkMode

internal object HostedAuthUrlBuilder {

    fun create(
        args: FinancialConnectionsSheetActivityArgs,
        manifest: FinancialConnectionsSessionManifest,
    ): String? {
        val elementsSessionContext = args.elementsSessionContext
        return create(
            hostedAuthUrl = manifest.hostedAuthUrl,
            isInstantDebits = args is FinancialConnectionsSheetActivityArgs.ForInstantDebits,
            linkMode = elementsSessionContext?.linkMode,
            billingDetails = elementsSessionContext?.billingDetails,
            prefillDetails = elementsSessionContext?.prefillDetails,
            incentive = elementsSessionContext?.incentive ?: false,
            incentiveEligibilitySession = elementsSessionContext?.let {
                it.paymentIntentId ?: it.setupIntentId ?: it.sessionId
            },
        )
    }

    private fun create(
        hostedAuthUrl: String?,
        isInstantDebits: Boolean,
        linkMode: LinkMode?,
        billingDetails: BillingDetails?,
        prefillDetails: PrefillDetails?,
        incentive: Boolean,
        incentiveEligibilitySession: String?,
    ): String? {
        if (hostedAuthUrl == null) {
            return null
        }

        val queryParams = mutableListOf(hostedAuthUrl)
        if (isInstantDebits) {
            // For Instant Debits, add a query parameter to the hosted auth URL so that payment account creation
            // takes place on the web side of the flow and the payment method ID is returned to the app.
            queryParams.add("return_payment_method=true")
            queryParams.add("expand_payment_method=true")
            linkMode?.let { queryParams.add("link_mode=${it.value}") }
            billingDetails?.let { queryParams.add(makeBillingDetailsQueryParams(it)) }

            queryParams.add("instantDebitsIncentive=$incentive")
            if (incentive) {
                queryParams.add("incentiveEligibilitySession=$incentiveEligibilitySession")
            }
        }

        prefillDetails?.run {
            email?.let { queryParams.add("email=$it") }
            phone?.let { queryParams.add("linkMobilePhone=$it") }
            phoneCountryCode?.let { queryParams.add("linkMobilePhoneCountry=$it") }
        }

        return queryParams.joinToString("&")
    }

    private fun makeBillingDetailsQueryParams(billingAddress: BillingDetails): String {
        val params = mapOf(
            "billingDetails" to billingAddress.toApiParams(),
        )
        return QueryStringFactory.createFromParamsWithEmptyValues(params)
    }
}
