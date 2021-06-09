package com.stripe.android.paymentsheet.elements.country

import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.elements.common.DropdownConfig
import java.util.Locale

internal class CountryConfig : DropdownConfig {
    override val debugLabel = "country"
    override val label = R.string.address_label_country

    override fun getDisplayItems(): List<String> =
        CountryUtils.getOrderedCountries(Locale.getDefault()).map { it.name }
    override fun getPaymentMethodParams(): List<String> =
        CountryUtils.getOrderedCountries(Locale.getDefault()).map { it.code.value }

}