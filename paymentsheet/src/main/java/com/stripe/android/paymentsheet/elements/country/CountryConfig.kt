package com.stripe.android.paymentsheet.elements.country

import androidx.annotation.StringRes
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.elements.common.DropdownConfig
import java.util.Locale

internal class CountryConfig(
    val onlyShowCountryCodes: Set<String> = emptySet(),
    val locale: Locale = Locale.getDefault()
) : DropdownConfig {
    override val debugLabel = "country"

    @StringRes
    override val label = R.string.address_label_country

    override fun getDisplayItems(): List<String> = if (onlyShowCountryCodes.isNotEmpty()) {
        CountryUtils.getOrderedCountries(locale)
            .filter { onlyShowCountryCodes.contains(it.code.value) }
            .map { it.name }
    } else {
        CountryUtils.getOrderedCountries(locale)
            .map { it.name }
    }
}
