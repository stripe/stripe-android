package com.stripe.android.uicore.elements

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import org.junit.Rule
import org.junit.Test

class DropdownScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        boxModifier = Modifier.padding(PaddingValues(vertical = 16.dp))
            .fillMaxWidth()
    )

    @Test
    fun testDropdownDefaultEnabled() {
        paparazziRule.snapshot {
            DropDown(
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
            DropDown(
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
            DropDown(
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
            DropDown(
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
