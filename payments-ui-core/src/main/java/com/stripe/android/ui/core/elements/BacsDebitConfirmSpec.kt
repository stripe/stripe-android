package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.R
import com.stripe.android.uicore.elements.CheckboxFieldController
import com.stripe.android.uicore.elements.CheckboxFieldElement
import com.stripe.android.uicore.elements.IdentifierSpec
import kotlinx.serialization.Serializable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Serializable
class BacsDebitConfirmSpec : FormItemSpec() {
    override val apiPath: IdentifierSpec = IdentifierSpec(
        v1 = "bacs_debit[confirmed]",
        ignoreField = true
    )

    fun transform(merchantName: String) = CheckboxFieldElement(
        apiPath,
        CheckboxFieldController(
            labelResource = CheckboxFieldController.LabelResource(
                R.string.stripe_bacs_confirm_mandate_label,
                merchantName
            ),
            debugTag = "BACS_MANDATE_CHECKBOX"
        )
    )
}
