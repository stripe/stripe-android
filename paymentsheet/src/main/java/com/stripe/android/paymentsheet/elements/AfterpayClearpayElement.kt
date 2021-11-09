package com.stripe.android.paymentsheet.elements

import android.content.res.Resources
import androidx.compose.ui.text.intl.Locale
import com.stripe.android.paymentsheet.CurrencyFormatter
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.forms.FormFieldEntry
import com.stripe.android.paymentsheet.model.Amount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal data class AfterpayClearpayElement(
    override val identifier: IdentifierSpec,
    private val amount: Amount,
    override val controller: Controller? = null
) : FormElement() {
    override fun getFormFieldValueFlow(): Flow<List<Pair<IdentifierSpec, FormFieldEntry>>> =
        MutableStateFlow(emptyList())

    val infoUrl = url.format(Locale.current.region.lowercase())

    fun getLabel(resources: Resources) =
        resources.getString(
            R.string.stripe_paymentsheet_afterpay_clearpay_message,
            CurrencyFormatter().format(
                amount.value / 4,
                amount.currencyCode
            )
        )

    companion object {
        const val url = "https://static-us.afterpay.com/javascript/modal/%s_rebrand_modal.html"
    }
}
