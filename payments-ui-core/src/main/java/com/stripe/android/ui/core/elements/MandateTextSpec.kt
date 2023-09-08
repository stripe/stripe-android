package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Mandate text element spec.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
data class MandateTextSpec(
    @SerialName("api_path")
    override val apiPath: IdentifierSpec = IdentifierSpec.Generic("mandate"),
    @StringRes
    val stringResId: Int
) : FormItemSpec() {

    fun transform(vararg args: String): FormElement {
        return MandateTextElement(
            identifier = apiPath,
            stringResId = stringResId,
            args = args.toList(),
        )
    }
}
