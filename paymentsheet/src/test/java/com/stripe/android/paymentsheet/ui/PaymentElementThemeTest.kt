package com.stripe.android.paymentsheet.ui

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.PaymentSheet
import org.junit.Test

internal class PaymentElementThemeTest {
    @Test
    fun `automatic follows system theme`() {
        assertThat(PaymentSheet.ThemeMode.Automatic.isDarkTheme(isSystemDark = true)).isTrue()
        assertThat(PaymentSheet.ThemeMode.Automatic.isDarkTheme(isSystemDark = false)).isFalse()
    }

    @Test
    fun `always light ignores system theme`() {
        assertThat(PaymentSheet.ThemeMode.AlwaysLight.isDarkTheme(isSystemDark = true)).isFalse()
        assertThat(PaymentSheet.ThemeMode.AlwaysLight.isDarkTheme(isSystemDark = false)).isFalse()
    }

    @Test
    fun `always dark ignores system theme`() {
        assertThat(PaymentSheet.ThemeMode.AlwaysDark.isDarkTheme(isSystemDark = true)).isTrue()
        assertThat(PaymentSheet.ThemeMode.AlwaysDark.isDarkTheme(isSystemDark = false)).isTrue()
    }
}
