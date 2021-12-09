package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo

/**
 * This is an element that will make elements (as specified by identifier) hidden
 * when save for future use is unchecked
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class SaveForFutureUseSpec(
    val identifierRequiredForFutureUse: List<RequiredItemSpec>
) : FormItemSpec(), RequiredItemSpec {
    override val identifier = IdentifierSpec.SaveForFutureUse

    fun transform(initialValue: Boolean, merchantName: String): FormElement =
        SaveForFutureUseElement(
            this.identifier,
            SaveForFutureUseController(
                this.identifierRequiredForFutureUse.map { requiredItemSpec ->
                    requiredItemSpec.identifier
                },
                initialValue
            ),
            merchantName
        )
}
