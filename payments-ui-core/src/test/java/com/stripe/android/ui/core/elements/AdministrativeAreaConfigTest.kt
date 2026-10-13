package com.stripe.android.ui.core.elements

import com.google.common.truth.Truth
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.uicore.elements.AdministrativeAreaConfig
import com.stripe.android.uicore.elements.DropdownConfig
import org.junit.Test
import com.stripe.android.core.R as CoreR

class AdministrativeAreaConfigTest {
    @Test
    fun `uses full dropdown mode with first option not selected`() {
        val config = AdministrativeAreaConfig(
            AdministrativeAreaConfig.Country.US()
        )
        Truth.assertThat(config.mode)
            .isEqualTo(DropdownConfig.Mode.Full(selectsFirstOptionAsDefault = false))
    }

    @Test
    fun `display values are full names of the state`() {
        val config = AdministrativeAreaConfig(
            AdministrativeAreaConfig.Country.US()
        )
        Truth.assertThat(config.convertFromRaw("CA"))
            .isEqualTo("California")
    }

    @Test
    fun `matches state codes without regard to case`() {
        val config = AdministrativeAreaConfig(
            AdministrativeAreaConfig.Country.US()
        )

        Truth.assertThat(config.convertFromRaw("wa"))
            .isEqualTo("Washington")
    }

    @Test
    fun `matches state names without regard to case`() {
        val config = AdministrativeAreaConfig(
            AdministrativeAreaConfig.Country.US()
        )

        Truth.assertThat(config.convertFromRaw("washington"))
            .isEqualTo("Washington")
    }

    @Test
    fun `us displays state`() {
        val config = AdministrativeAreaConfig(
            AdministrativeAreaConfig.Country.US()
        )
        Truth.assertThat(config.label)
            .isEqualTo(resolvableString(CoreR.string.stripe_address_label_state))
    }

    @Test
    fun `canada displays province`() {
        val config = AdministrativeAreaConfig(
            AdministrativeAreaConfig.Country.Canada()
        )
        Truth.assertThat(config.label)
            .isEqualTo(resolvableString(CoreR.string.stripe_address_label_province))
    }

    @Test
    fun `brazil displays state`() {
        val config = AdministrativeAreaConfig(
            AdministrativeAreaConfig.Country.Brazil()
        )
        Truth.assertThat(config.label)
            .isEqualTo(resolvableString(CoreR.string.stripe_address_label_state))
    }

    @Test
    fun `brazil converts raw state code to full name`() {
        val config = AdministrativeAreaConfig(
            AdministrativeAreaConfig.Country.Brazil()
        )
        Truth.assertThat(config.convertFromRaw("SP"))
            .isEqualTo("São Paulo")
    }

    @Test
    fun `brazil matches state codes without regard to case`() {
        val config = AdministrativeAreaConfig(
            AdministrativeAreaConfig.Country.Brazil()
        )

        Truth.assertThat(config.convertFromRaw("sp"))
            .isEqualTo("São Paulo")
    }

    @Test
    fun `brazil matches state names without regard to case`() {
        val config = AdministrativeAreaConfig(
            AdministrativeAreaConfig.Country.Brazil()
        )

        Truth.assertThat(config.convertFromRaw("são paulo"))
            .isEqualTo("São Paulo")
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
