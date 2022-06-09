package com.stripe.android.ui.core.elements

import com.stripe.android.ui.core.Amount
import kotlinx.serialization.Serializable

/**
 * Header that displays information about installments for Afterpay
 */
@Serializable
internal data class AfterpayClearpayTextSpec(
    override val apiPath: IdentifierSpec = IdentifierSpec.Generic("afterpay_text")
) : FormItemSpec() {
    fun transform(amount: Amount): FormElement =
        AfterpayClearpayHeaderElement(this.apiPath, amount)
}
