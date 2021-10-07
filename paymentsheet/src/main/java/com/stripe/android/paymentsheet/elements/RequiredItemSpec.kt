package com.stripe.android.paymentsheet.elements

import android.os.Parcelable

/**
 * Identifies a field that can be made hidden.
 */
internal interface RequiredItemSpec {
    val identifier: IdentifierSpec
}
