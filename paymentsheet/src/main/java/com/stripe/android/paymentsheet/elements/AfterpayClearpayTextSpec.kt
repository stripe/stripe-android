package com.stripe.android.paymentsheet.elements

/**
 * Header that displays information about installments for Afterpay
 */
internal data class AfterpayClearpayTextSpec(
    override val identifier: IdentifierSpec
) : FormItemSpec(), RequiredItemSpec
