package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.R
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SimpleTextElement
import com.stripe.android.uicore.elements.SimpleTextFieldController
import kotlinx.serialization.Serializable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Serializable
class BacsDebitBankAccountSpec : FormItemSpec() {
    override val apiPath: IdentifierSpec = IdentifierSpec()

    fun transform(
        initialValues: Map<IdentifierSpec, String?>
    ) = createSectionElement(
        listOf(
            SimpleTextElement(
                IdentifierSpec.Generic("bacs_debit[sort_code]"),
                SimpleTextFieldController(
                    BacsDebitSortCodeConfig(),
                    initialValue = initialValues[this.apiPath]
                )
            ),
            SimpleTextElement(
                IdentifierSpec.Generic("bacs_debit[account_number]"),
                SimpleTextFieldController(
                    BacsDebitAccountNumberConfig(),
                    initialValue = initialValues[this.apiPath]
                )
            )
        ),
        R.string.stripe_bacs_bank_account_title
    )
}
