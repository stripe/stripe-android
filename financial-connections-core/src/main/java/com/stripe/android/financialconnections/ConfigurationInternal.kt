package com.stripe.android.financialconnections

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.model.IncentiveEligibilitySession
import com.stripe.android.model.LinkMode
import kotlinx.parcelize.Parcelize
import java.io.Serializable

@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY)
data class FinancialConnectionsSheetConfiguration(
    val financialConnectionsSessionClientSecret: String,
    val publishableKey: String,
    val stripeAccountId: String? = null
) : Parcelable

/**
 * Context for sessions created from Stripe Elements. This isn't intended to be
 * part of the public API, but solely to be used by the Mobile Payment Element and
 * CustomerSheet.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
data class ElementsSessionContext(
    val amount: Long?,
    val currency: String?,
    val linkMode: LinkMode?,
    val billingDetails: BillingDetails?,
    val prefillDetails: PrefillDetails,
    val incentiveEligibilitySession: IncentiveEligibilitySession?
) : Parcelable {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    data class BillingDetails(
        val name: String? = null,
        val phone: String? = null,
        val email: String? = null,
        val address: Address? = null,
    ) : Parcelable {

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Parcelize
        data class Address(
            val line1: String? = null,
            val line2: String? = null,
            val postalCode: String? = null,
            val city: String? = null,
            val state: String? = null,
            val country: String? = null,
        ) : Parcelable
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    data class PrefillDetails(
        val email: String?,
        val phone: String?,
        val phoneCountryCode: String?,
    ) : Parcelable, Serializable {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        companion object {
            private const val serialVersionUID: Long = 626669472462415908L
        }
    }
}
