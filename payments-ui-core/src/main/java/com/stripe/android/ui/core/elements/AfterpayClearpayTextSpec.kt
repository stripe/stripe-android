package com.stripe.android.ui.core.elements

import com.stripe.android.ui.core.Amount
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Header that displays information about installments for Afterpay
 */
@Serializable
@Parcelize
internal data class AfterpayClearpayTextSpec(
    override val apiPath: IdentifierSpec = IdentifierSpec.Generic("afterpay_clearpay_text")
) : FormItemSpec(), RequiredItemSpec {
    fun transform(amount: Amount): FormElement =
        AfterpayClearpayHeaderElement(this.apiPath, amount)
}
