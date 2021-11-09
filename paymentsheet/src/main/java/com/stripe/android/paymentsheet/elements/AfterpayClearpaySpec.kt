package com.stripe.android.paymentsheet.elements

/**
 * Header that displays information about installments for Afterpay
 */
internal data class AfterpayClearpaySpec(
    override val identifier: IdentifierSpec
) : FormItemSpec(), RequiredItemSpec
