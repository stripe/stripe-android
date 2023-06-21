package com.stripe.android.uicore.elements

import com.stripe.android.uicore.utils.PaparazziRule
import com.stripe.android.uicore.utils.SystemAppearance
import org.junit.Rule
import org.junit.Test

class DropdownScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.values()
    )

    @Test
    fun testDropdownDefaultEnabled() {
        paparazziRule.snapshot {
            Dropdown(
                controller = DropdownFieldController(
                    CountryConfig(tinyMode = true)
                ),
                enabled = true
            )
        }
    }

    @Test
    fun testDropdownDefaultDisabled() {
        paparazziRule.snapshot {
            Dropdown(
                controller = DropdownFieldController(
                    CountryConfig(tinyMode = true)
                ),
                enabled = false
            )
        }
    }

    @Test
    fun testDropdownWithDisableDropdownWithSingleElement() {
        paparazziRule.snapshot {
            Dropdown(
                controller = DropdownFieldController(
                    CountryConfig(
                        onlyShowCountryCodes = setOf("US"),
                        disableDropdownWithSingleElement = true
                    )
                ),
                enabled = true
            )
        }
    }

    @Test
    fun testDropdownWithoutDisableDropdownWithSingleElement() {
        paparazziRule.snapshot {
            Dropdown(
                controller = DropdownFieldController(
                    CountryConfig(
                        onlyShowCountryCodes = setOf("US")
                    )
                ),
                enabled = true
            )
        }
    }
}
