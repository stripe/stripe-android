package com.stripe.android.paymentsheet.elements

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.R
import org.junit.Test
import java.util.Locale

class CountryConfigTest {

    @Test
    fun `Verify the displayed country list`() {
        assertThat(CountryConfig(locale = Locale.US).getDisplayItems()[0])
            .isEqualTo("United States")
    }

    @Test
    fun `Verify the label`() {
        assertThat(CountryConfig(locale = Locale.US).label)
            .isEqualTo(R.string.address_label_country)
    }

    @Test
    fun `Verify only show countries requested`() {
        assertThat(
            CountryConfig(
                onlyShowCountryCodes = setOf("AT"),
                locale = Locale.US
            ).getDisplayItems()[0]
        ).isEqualTo("Austria")
    }
}
