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
@SerialName("afterpay_header")
internal data class AfterpayClearpayTextSpec(
    override val api_path: IdentifierSpec = IdentifierSpec.Generic("afterpay_clearpay_text")
) : FormItemSpec(), RequiredItemSpec {
    fun transform(amount: Amount): FormElement =
        AfterpayClearpayHeaderElement(this.api_path, amount)
}
