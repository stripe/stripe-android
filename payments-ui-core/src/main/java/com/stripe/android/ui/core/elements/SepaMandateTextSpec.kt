package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import com.stripe.android.ui.core.R
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Mandate text element spec.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
internal data class SepaMandateTextSpec(
    @SerialName("api_path")
    override val apiPath: IdentifierSpec = IdentifierSpec.Generic("sepa_mandate"),
    @StringRes
    val stringResId: Int = R.string.sepa_mandate,
) : FormItemSpec() {
    @Transient
    private val mandateTextSpec = MandateTextSpec(apiPath, stringResId)
    fun transform(merchantName: String): FormElement = mandateTextSpec.transform(merchantName)
}
