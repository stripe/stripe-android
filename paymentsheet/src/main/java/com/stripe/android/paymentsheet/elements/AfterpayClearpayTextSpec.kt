package com.stripe.android.paymentsheet.elements

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Header that displays information about installments for Afterpay
 */
@Parcelize
internal data class AfterpayClearpayTextSpec(
    override val identifier: IdentifierSpec
) : FormItemSpec(), RequiredItemSpec, Parcelable
