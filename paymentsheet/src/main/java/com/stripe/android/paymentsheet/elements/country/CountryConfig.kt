package com.stripe.android.paymentsheet.elements.country

import androidx.annotation.StringRes
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.elements.common.DropdownConfig
import java.util.Locale

internal class CountryConfig(val locale: Locale = Locale.getDefault()) : DropdownConfig {
    override val debugLabel = "country"

    @StringRes
    override val label = R.string.address_label_country

    override fun getDisplayItems(): List<String> =
        CountryUtils.getOrderedCountries(locale).map { it.name }

    override fun getPaymentMethodParams(): List<String> =
        CountryUtils.getOrderedCountries(locale).map { it.code.value }
}