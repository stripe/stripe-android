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
data class SaveForFutureUseSpec(
    @SerialName("api_path")
    override val apiPath: IdentifierSpec = IdentifierSpec.SaveForFutureUse
) : FormItemSpec() {
    fun transform(initialValue: Boolean, merchantName: String): FormElement =
        SaveForFutureUseElement(
            this.apiPath,
            SaveForFutureUseController(
                initialValue
            ),
            merchantName
        )
}
