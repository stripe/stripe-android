package com.stripe.android.ui.core.elements

import android.content.res.Resources
import androidx.annotation.RestrictTo
import androidx.compose.ui.text.intl.Locale
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.CurrencyFormatter
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.forms.FormFieldEntry
import com.stripe.android.paymentsheet.elements.Controller
import com.stripe.android.paymentsheet.elements.FormElement
import com.stripe.android.paymentsheet.elements.IdentifierSpec
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.CurrencyFormatter
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.forms.FormFieldEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class AfterpayClearpayHeaderElement(
    override val identifier: IdentifierSpec,
    private val amount: Amount,
    override val controller: Controller? = null
) : FormElement() {
    override fun getFormFieldValueFlow(): Flow<List<Pair<IdentifierSpec, FormFieldEntry>>> =
        MutableStateFlow(emptyList())

    val infoUrl = url.format(Locale.current.region.lowercase())

    fun getLabel(resources: Resources) =
        resources.getString(
            R.string.afterpay_clearpay_message,
            CurrencyFormatter().format(
                amount.value / 4,
                amount.currencyCode
            )
        )

    companion object {
        const val url = "https://static-us.afterpay.com/javascript/modal/%s_rebrand_modal.html"
    }
}
