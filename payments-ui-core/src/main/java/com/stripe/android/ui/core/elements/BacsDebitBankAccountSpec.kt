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
    private val sortCodeIdentifier = IdentifierSpec.Generic(SORT_CODE_API_PATH)
    private val accountNumberIdentifier = IdentifierSpec.Generic(ACCOUNT_NUMBER_API_PATH)

    override val apiPath: IdentifierSpec = IdentifierSpec()

    fun transform(
        initialValues: Map<IdentifierSpec, String?>
    ) = createSectionElement(
        listOf(
            SimpleTextElement(
                sortCodeIdentifier,
                SimpleTextFieldController(
                    BacsDebitSortCodeConfig(),
                    initialValue = initialValues[sortCodeIdentifier]
                )
            ),
            SimpleTextElement(
                accountNumberIdentifier,
                SimpleTextFieldController(
                    BacsDebitAccountNumberConfig(),
                    initialValue = initialValues[accountNumberIdentifier]
                )
            )
        ),
        R.string.stripe_bacs_bank_account_title
    )

    private companion object {
        const val SORT_CODE_API_PATH = "bacs_debit[sort_code]"
        const val ACCOUNT_NUMBER_API_PATH = "bacs_debit[account_number]"
    }
}
