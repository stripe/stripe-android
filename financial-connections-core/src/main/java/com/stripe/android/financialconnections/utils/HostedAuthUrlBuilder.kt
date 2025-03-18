package com.stripe.android.financialconnections.utils

import androidx.annotation.RestrictTo
import com.stripe.android.core.networking.QueryStringFactory
import com.stripe.android.financialconnections.ElementsSessionContext
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs
import com.stripe.android.model.IncentiveEligibilitySession
import com.stripe.android.model.LinkMode

@RestrictTo(RestrictTo.Scope.LIBRARY)
object HostedAuthUrlBuilder {

    fun create(
        args: FinancialConnectionsSheetActivityArgs,
        hostedAuthUrl: String?,
        prefillDetails: ElementsSessionContext.PrefillDetails? = args.elementsSessionContext?.prefillDetails,
    ): String? {
        return create(
            hostedAuthUrl = hostedAuthUrl,
            isInstantDebits = args is FinancialConnectionsSheetActivityArgs.ForInstantDebits,
            linkMode = args.elementsSessionContext?.linkMode,
            billingDetails = args.elementsSessionContext?.billingDetails,
            prefillDetails = prefillDetails,
            incentiveEligibilitySession = args.elementsSessionContext?.incentiveEligibilitySession,
        )
    }

    private fun create(
        hostedAuthUrl: String?,
        isInstantDebits: Boolean,
        linkMode: LinkMode?,
        billingDetails: ElementsSessionContext.BillingDetails?,
        prefillDetails: ElementsSessionContext.PrefillDetails?,
        incentiveEligibilitySession: IncentiveEligibilitySession?,
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
            queryParams.add("instantDebitsIncentive=${incentiveEligibilitySession != null}")
            linkMode?.let { queryParams.add("link_mode=${it.value}") }
            billingDetails?.let { queryParams.add(makeBillingDetailsQueryParams(it)) }
            incentiveEligibilitySession?.let { queryParams.add("incentiveEligibilitySession=${it.id}") }
        }

        prefillDetails?.run {
            email?.let { queryParams.add("email=$it") }
            phone?.let { queryParams.add("linkMobilePhone=$it") }
            phoneCountryCode?.let { queryParams.add("linkMobilePhoneCountry=$it") }
        }

        // hint for frontend to understand the surface launching the web flow.
        queryParams.add("launched_by=android_sdk")

        return queryParams.joinToString("&")
    }

    private fun makeBillingDetailsQueryParams(billingAddress: ElementsSessionContext.BillingDetails): String {
        val params = mapOf(
            "billingDetails" to billingAddress.toApiParams(),
        )
        return QueryStringFactory.createFromParamsWithEmptyValues(params)
    }
}
