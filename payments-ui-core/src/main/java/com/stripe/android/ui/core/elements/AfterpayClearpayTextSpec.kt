package com.stripe.android.ui.core.elements

import com.stripe.android.ui.core.Amount
import kotlinx.parcelize.Parcelize

/**
 * Header that displays information about installments for Afterpay
 */
@Parcelize
internal data class AfterpayClearpayTextSpec(
    override val api_path: IdentifierSpec
) : FormItemSpec(), RequiredItemSpec {
    fun transform(amount: Amount): FormElement =
        AfterpayClearpayHeaderElement(this.api_path, amount)
}
