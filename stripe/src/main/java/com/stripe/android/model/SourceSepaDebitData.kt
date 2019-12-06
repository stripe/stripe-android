package com.stripe.android.model

import kotlinx.android.parcel.Parcelize

/**
 * Model for the SourceTypeData contained in a SEPA Debit Source object.
 */
@Parcelize
data class SourceSepaDebitData internal constructor(
    val bankCode: String?,
    val branchCode: String?,
    val country: String?,
    val fingerPrint: String?,
    val last4: String?,
    val mandateReference: String?,
    val mandateUrl: String?
) : StripeSourceTypeModel()
