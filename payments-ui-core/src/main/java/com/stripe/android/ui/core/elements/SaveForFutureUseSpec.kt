package com.stripe.android.paymentsheet.elements

/**
 * This is an element that will make elements (as specified by identifier) hidden
 * when save for future use is unchecked
 */
internal data class SaveForFutureUseSpec(
    val identifierRequiredForFutureUse: List<RequiredItemSpec>
) : FormItemSpec(), RequiredItemSpec {
    override val identifier = IdentifierSpec.SaveForFutureUse
}
