package com.stripe.android.ui.core.elements

import com.stripe.android.ui.core.Amount

/**
 * Header that displays information about installments for Afterpay
 */
internal data class AfterpayClearpayTextSpec(
    override val identifier: IdentifierSpec
) : FormItemSpec(), RequiredItemSpec {
    fun transform(amount: Amount): FormElement =
        AfterpayClearpayHeaderElement(this.identifier, amount)
}
