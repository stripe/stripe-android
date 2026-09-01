package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.uicore.elements.IdentifierSpec
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Serializable
@Parcelize
data class BsbSpec(
    @SerialName("api_path")
    override val apiPath: IdentifierSpec = IdentifierSpec.Generic(
        "au_becs_debit[bsb_number]"
    )
) : FormItemSpec() {
    fun transform(initialValues: Map<IdentifierSpec, String?>): BsbElement =
        BsbElement(
            this.apiPath,
            auBecsDebitBanks,
            initialValues[this.apiPath]
        )
}
