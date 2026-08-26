package com.stripe.android.link.theme

import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider
import com.stripe.android.link.LinkAppearance
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
internal class ThemeTest {
    @Test
    fun `isDarkTheme resolves expected theme`(
        @TestParameter(valuesProvider = ThemeTestCaseProvider::class)
        testCase: ThemeTestCase,
    ) {
        assertThat(testCase.style.isDarkTheme(testCase.isSystemDarkTheme))
            .isEqualTo(testCase.expected)
    }
}

internal object ThemeTestCaseProvider : TestParameterValuesProvider() {
    override fun provideValues(
        context: Context?,
    ): List<ThemeTestCase> = listOf(
        ThemeTestCase(
            name = "Automatic on light system",
            style = LinkAppearance.Style.AUTOMATIC,
            isSystemDarkTheme = false,
            expected = false,
        ),
        ThemeTestCase(
            name = "Automatic on dark system",
            style = LinkAppearance.Style.AUTOMATIC,
            isSystemDarkTheme = true,
            expected = true,
        ),
        ThemeTestCase(
            name = "Always light on light system",
            style = LinkAppearance.Style.ALWAYS_LIGHT,
            isSystemDarkTheme = false,
            expected = false,
        ),
        ThemeTestCase(
            name = "Always light on dark system",
            style = LinkAppearance.Style.ALWAYS_LIGHT,
            isSystemDarkTheme = true,
            expected = false,
        ),
        ThemeTestCase(
            name = "Always dark on light system",
            style = LinkAppearance.Style.ALWAYS_DARK,
            isSystemDarkTheme = false,
            expected = true,
        ),
        ThemeTestCase(
            name = "Always dark on dark system",
            style = LinkAppearance.Style.ALWAYS_DARK,
            isSystemDarkTheme = true,
            expected = true,
        ),
        ThemeTestCase(
            name = "Missing style on light system",
            style = null,
            isSystemDarkTheme = false,
            expected = false,
        ),
        ThemeTestCase(
            name = "Missing style on dark system",
            style = null,
            isSystemDarkTheme = true,
            expected = true,
        ),
    )
}

internal data class ThemeTestCase(
    val name: String,
    val style: LinkAppearance.Style?,
    val isSystemDarkTheme: Boolean,
    val expected: Boolean,
) {
    override fun toString(): String = name
}
