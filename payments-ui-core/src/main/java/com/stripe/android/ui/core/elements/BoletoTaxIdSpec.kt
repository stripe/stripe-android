package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.R
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
data class BoletoTaxIdSpec(
    @SerialName("api_path")
    override val apiPath: IdentifierSpec = IdentifierSpec.Generic("boleto[tax_id]")
) : FormItemSpec() {
    @Transient
    private val simpleTextSpec =
        SimpleTextSpec(
            apiPath,
            R.string.stripe_boleto_tax_id_label
        )

    fun transform(initialValues: Map<IdentifierSpec, String?>): FormElement {
        return simpleTextSpec.transform(initialValues)
    }
}
