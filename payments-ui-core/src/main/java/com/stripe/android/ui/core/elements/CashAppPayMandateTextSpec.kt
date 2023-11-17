package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import com.stripe.android.ui.core.R
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
data class CashAppPayMandateTextSpec(
    @SerialName("api_path")
    override val apiPath: IdentifierSpec = IdentifierSpec.Generic("cashapp_mandate"),
    @StringRes
    val stringResId: Int = R.string.stripe_cash_app_pay_mandate,
) : FormItemSpec() {

    @Transient
    private val mandateTextSpec = MandateTextSpec(apiPath, stringResId)

    fun transform(merchantName: String): FormElement {
        return mandateTextSpec.transform(merchantName, merchantName)
    }
}
