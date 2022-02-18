package com.stripe.android.ui.core.elements

import android.content.res.Resources
import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.CurrencyFormatter
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.forms.FormFieldEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class AffirmHeaderElement(
    override val identifier: IdentifierSpec,
    private val amount: Amount,
    override val controller: Controller? = null
) : FormElement() {
    override fun getFormFieldValueFlow(): Flow<List<Pair<IdentifierSpec, FormFieldEntry>>> =
        MutableStateFlow(emptyList())

    fun getLabel(resources: Resources) =
        resources.getString(
            R.string.affirm_starting_at,
            CurrencyFormatter().format(
                amount.value / 4,
                amount.currencyCode
            )
        )
}
