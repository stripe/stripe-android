package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import com.stripe.android.ui.core.R
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Mandate text element spec.
 */
@Serializable
@SerialName("sepa_mandate")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
internal data class MandateTextSpec(
    override val api_path: IdentifierSpec = IdentifierSpec.Generic("sepa_mandate"),
    @StringRes
    val stringResId: Int = R.string.sepa_mandate,
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
