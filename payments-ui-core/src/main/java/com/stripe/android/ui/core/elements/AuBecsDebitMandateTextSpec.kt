package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class AuBecsDebitMandateTextSpec(
    @SerialName("api_path")
    override val apiPath: IdentifierSpec = IdentifierSpec.Generic("au_becs_mandate")
) : FormItemSpec() {
    fun transform(merchantName: String): FormElement =
        AuBecsDebitMandateTextElement(
            this.apiPath,
            merchantName
        )
}
