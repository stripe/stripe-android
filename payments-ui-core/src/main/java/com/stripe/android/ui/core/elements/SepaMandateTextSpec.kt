package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import com.stripe.android.ui.core.R
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * Mandate text element spec.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
@Parcelize
internal data class SepaMandateTextSpec(
    override val api_path: IdentifierSpec = IdentifierSpec.Generic("sepa_mandate"),
    @StringRes
    val stringResId: Int = R.string.sepa_mandate,
) : FormItemSpec(), RequiredItemSpec {
    @IgnoredOnParcel
    private val mandateTextSpec = MandateTextSpec(api_path, stringResId)
    fun transform(merchantName: String): FormElement = mandateTextSpec.transform(merchantName)
}
