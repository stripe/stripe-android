package com.stripe.android.paymentsheet.elements

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.elements.country.CountryConfig
import org.junit.Test
import java.util.Locale

class CountryConfigTest {

    @Test
    fun `Verify the displayed country list`() {
        assertThat(CountryConfig(Locale.US).getDisplayItems()[0]).isEqualTo("United States")
    }

    @Test
    fun `Verify the label`() {
        CountryConfig().label
        assertThat(CountryConfig(Locale.US).label).isEqualTo(R.string.address_label_country)
    }
}