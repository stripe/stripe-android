package com.stripe.android.paymentsheet.elements

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * This is an element that will make elements (as specified by identifier) hidden
 * when save for future use is unchecked
 */
@Parcelize
internal data class SaveForFutureUseSpec(
    val identifierRequiredForFutureUse: List<RequiredItemSpec>
) : FormItemSpec(), RequiredItemSpec, Parcelable {
    override val identifier = IdentifierSpec.SaveForFutureUse
}
