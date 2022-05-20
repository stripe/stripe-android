package com.stripe.android.ui.core.elements

import com.stripe.android.ui.core.Amount
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Header that displays information about installments for Afterpay
 */
@Serializable
@SerialName("afterpay_header")
internal data class AfterpayClearpayTextSpec(
    override val api_path: IdentifierSpec
) : FormItemSpec(), RequiredItemSpec {
    fun transform(amount: Amount): FormElement =
        AfterpayClearpayHeaderElement(this.api_path, amount)
}
