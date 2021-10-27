package com.stripe.android.paymentsheet.elements

/**
 * Header that displays a string based on the selected payment method
 */
internal data class SimpleTextHeaderSpec(
    override val identifier: IdentifierSpec
) : FormItemSpec(), RequiredItemSpec
