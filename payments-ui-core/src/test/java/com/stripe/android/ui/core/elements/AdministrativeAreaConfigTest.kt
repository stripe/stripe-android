package com.stripe.android.ui.core.elements

import com.google.common.truth.Truth
import com.stripe.android.uicore.elements.AdministrativeAreaConfig
import org.junit.Test
import com.stripe.android.core.R as CoreR

class AdministrativeAreaConfigTest {
    @Test
    fun `display values are full names of the state`() {
        val config = AdministrativeAreaConfig(
            AdministrativeAreaConfig.Country.US()
        )
        Truth.assertThat(config.convertFromRaw("CA"))
            .isEqualTo("California")
    }

    @Test
    fun `us displays state`() {
        val config = AdministrativeAreaConfig(
            AdministrativeAreaConfig.Country.US()
        )
        Truth.assertThat(config.label)
            .isEqualTo(CoreR.string.stripe_address_label_state)
    }

    @Test
    fun `canada displays province`() {
        val config = AdministrativeAreaConfig(
            AdministrativeAreaConfig.Country.Canada()
        )
        Truth.assertThat(config.label)
            .isEqualTo(CoreR.string.stripe_address_label_province)
    }

    @Test
    fun `dropdown defaults to first element if raw value doesn't exist`() {
        val config = AdministrativeAreaConfig(
            AdministrativeAreaConfig.Country.US()
        )
        Truth.assertThat(config.convertFromRaw("ABC"))
            .isEqualTo("Alabama")
    }
}
