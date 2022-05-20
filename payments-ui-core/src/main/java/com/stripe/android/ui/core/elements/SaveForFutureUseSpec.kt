package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * This is an element that will make elements (as specified by identifier) hidden
 * when save for future use is unchecked
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
@SerialName("save_for_future")
data class SaveForFutureUseSpec(
    override val api_path: IdentifierSpec = IdentifierSpec.SaveForFutureUse
) : FormItemSpec(), RequiredItemSpec {
    fun transform(initialValue: Boolean, merchantName: String): FormElement =
        SaveForFutureUseElement(
            this.api_path,
            SaveForFutureUseController(
                initialValue
            ),
            merchantName
        )
}
