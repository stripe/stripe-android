package com.stripe.android.ui.core.elements

import com.stripe.android.ui.core.Amount
import kotlinx.serialization.Serializable

/**
 * Header that displays information about installments for Afterpay
 */
@Serializable
internal data class AfterpayClearpayTextSpec(
    override val apiPath: IdentifierSpec = DEFAULT_API_PATH
) : FormItemSpec() {
    fun transform(amount: Amount): FormElement =
        AfterpayClearpayHeaderElement(this.apiPath, amount)

    companion object {
        val DEFAULT_API_PATH = IdentifierSpec.Generic("afterpay_text")
    }
}
