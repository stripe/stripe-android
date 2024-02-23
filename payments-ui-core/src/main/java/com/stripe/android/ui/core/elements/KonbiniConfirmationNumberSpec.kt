package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.R
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Parcelize
data object KonbiniConfirmationNumberSpec : FormItemSpec() {
    @IgnoredOnParcel
    override val apiPath: IdentifierSpec = IdentifierSpec.KonbiniConfirmationNumber

    @IgnoredOnParcel
    @Transient
    private val simpleTextSpec = SimpleTextSpec(
        apiPath,
        R.string.stripe_konbini_confirmation_number_label,
        keyboardType = KeyboardType.Phone,
        showOptionalLabel = true,
    )

    fun transform(initialValues: Map<IdentifierSpec, String?>): FormElement {
        return simpleTextSpec.transform(initialValues)
    }
}
