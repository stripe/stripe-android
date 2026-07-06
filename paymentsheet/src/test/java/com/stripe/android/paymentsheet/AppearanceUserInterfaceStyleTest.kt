package com.stripe.android.paymentsheet

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentelement.AppearanceAPIAdditionsPreview
import com.stripe.android.uicore.StripeTheme
import org.junit.After
import org.junit.Test

@OptIn(AppearanceAPIAdditionsPreview::class)
class AppearanceUserInterfaceStyleTest {

    @After
    fun tearDown() {
        StripeTheme.nightModeOverride = null
    }

    @Test
    fun `default appearance style is Automatic`() {
        assertThat(PaymentSheet.Appearance().style)
            .isEqualTo(PaymentSheet.UserInterfaceStyle.Automatic)
    }

    @Test
    fun `builder sets the style`() {
        val appearance = PaymentSheet.Appearance.Builder()
            .style(PaymentSheet.UserInterfaceStyle.AlwaysDark)
            .build()

        assertThat(appearance.style).isEqualTo(PaymentSheet.UserInterfaceStyle.AlwaysDark)
    }

    @Test
    fun `parseAppearance leaves the override null for Automatic`() {
        // Pre-condition: a stale override that Automatic should clear back to system-driven.
        StripeTheme.nightModeOverride = true

        PaymentSheet.Appearance.Builder()
            .style(PaymentSheet.UserInterfaceStyle.Automatic)
            .build()
            .parseAppearance()

        assertThat(StripeTheme.nightModeOverride).isNull()
    }

    @Test
    fun `parseAppearance forces light for AlwaysLight`() {
        PaymentSheet.Appearance.Builder()
            .style(PaymentSheet.UserInterfaceStyle.AlwaysLight)
            .build()
            .parseAppearance()

        assertThat(StripeTheme.nightModeOverride).isFalse()
    }

    @Test
    fun `parseAppearance forces dark for AlwaysDark`() {
        PaymentSheet.Appearance.Builder()
            .style(PaymentSheet.UserInterfaceStyle.AlwaysDark)
            .build()
            .parseAppearance()

        assertThat(StripeTheme.nightModeOverride).isTrue()
    }

    @Test
    fun `resolveIsDark follows the system flag for Automatic`() {
        val appearance = appearanceWithStyle(PaymentSheet.UserInterfaceStyle.Automatic)

        assertThat(appearance.resolveIsDark(systemInDarkTheme = true)).isTrue()
        assertThat(appearance.resolveIsDark(systemInDarkTheme = false)).isFalse()
    }

    @Test
    fun `resolveIsDark forces light regardless of the system flag for AlwaysLight`() {
        val appearance = appearanceWithStyle(PaymentSheet.UserInterfaceStyle.AlwaysLight)

        assertThat(appearance.resolveIsDark(systemInDarkTheme = true)).isFalse()
        assertThat(appearance.resolveIsDark(systemInDarkTheme = false)).isFalse()
    }

    @Test
    fun `resolveIsDark forces dark regardless of the system flag for AlwaysDark`() {
        val appearance = appearanceWithStyle(PaymentSheet.UserInterfaceStyle.AlwaysDark)

        assertThat(appearance.resolveIsDark(systemInDarkTheme = true)).isTrue()
        assertThat(appearance.resolveIsDark(systemInDarkTheme = false)).isTrue()
    }

    private fun appearanceWithStyle(style: PaymentSheet.UserInterfaceStyle): PaymentSheet.Appearance {
        return PaymentSheet.Appearance.Builder().style(style).build()
    }
}
