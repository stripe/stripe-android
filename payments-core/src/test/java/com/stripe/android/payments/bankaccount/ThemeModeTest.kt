package com.stripe.android.payments.bankaccount

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import com.stripe.android.financialconnections.ThemeMode as FinancialConnectionsThemeMode

internal class ThemeModeTest {
    @Test
    fun `automatic follows the Financial Connections default theme`() {
        assertThat(ThemeMode.Automatic.toFinancialConnectionsThemeMode())
            .isEqualTo(FinancialConnectionsThemeMode.Automatic)
    }

    @Test
    fun `forced light is passed to Financial Connections`() {
        assertThat(ThemeMode.AlwaysLight.toFinancialConnectionsThemeMode())
            .isEqualTo(FinancialConnectionsThemeMode.AlwaysLight)
    }

    @Test
    fun `forced dark is passed to Financial Connections`() {
        assertThat(ThemeMode.AlwaysDark.toFinancialConnectionsThemeMode())
            .isEqualTo(FinancialConnectionsThemeMode.AlwaysDark)
    }
}
