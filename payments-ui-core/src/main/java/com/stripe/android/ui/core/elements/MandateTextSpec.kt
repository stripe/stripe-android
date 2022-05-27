package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import kotlinx.parcelize.Parcelize

/**
 * Mandate text element spec.
 */
@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class MandateTextSpec(
    override val api_path: IdentifierSpec,
    @StringRes
    val stringResId: Int,
) : FormItemSpec(), RequiredItemSpec {
    fun transform(merchantName: String): FormElement =
        // It could be argued that the static text should have a controller, but
        // since it doesn't provide a form field we leave it out for now
        MandateTextElement(
            this.api_path,
            this.stringResId,
            merchantName
        )
}
