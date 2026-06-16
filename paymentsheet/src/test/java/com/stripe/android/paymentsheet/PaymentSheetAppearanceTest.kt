package com.stripe.android.paymentsheet

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentelement.AppearanceAPIAdditionsPreview
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.ThemeMode
import org.junit.After
import org.junit.Test

class PaymentSheetAppearanceTest {

    @After
    fun tearDown() {
        PaymentSheet.Appearance().parseAppearance()
    }

    @OptIn(AppearanceAPIAdditionsPreview::class)
    @Test
    fun `Appearance builder defaults theme mode to automatic`() {
        val appearance = PaymentSheet.Appearance.Builder().build()

        assertThat(appearance.themeMode).isEqualTo(PaymentSheet.Appearance.ThemeMode.Automatic)
    }

    @OptIn(AppearanceAPIAdditionsPreview::class)
    @Test
    fun `Appearance builder stores provided theme mode`() {
        val appearance = PaymentSheet.Appearance.Builder()
            .themeMode(PaymentSheet.Appearance.ThemeMode.AlwaysDark)
            .build()

        assertThat(appearance.themeMode).isEqualTo(PaymentSheet.Appearance.ThemeMode.AlwaysDark)
    }

    @OptIn(AppearanceAPIAdditionsPreview::class)
    @Test
    fun `parseAppearance maps theme mode to StripeTheme`() {
        PaymentSheet.Appearance.Builder()
            .themeMode(PaymentSheet.Appearance.ThemeMode.AlwaysLight)
            .build()
            .parseAppearance()

        assertThat(StripeTheme.themeMode).isEqualTo(ThemeMode.AlwaysLight)
    }
}
