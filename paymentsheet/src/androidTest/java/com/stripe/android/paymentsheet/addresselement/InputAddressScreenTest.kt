@file:OptIn(com.stripe.android.paymentelement.AppearanceAPIAdditionsPreview::class)

package com.stripe.android.paymentsheet.addresselement

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.height
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.uicore.stripeThemeIsDark
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InputAddressScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun clicking_primary_button_triggers_callback_when_enabled() {
        var counter = 0
        setContent(primaryButtonEnabled = true, primaryButtonCallback = { counter++ })
        composeTestRule.onNodeWithText("Save Address").performClick()
        assertThat(counter).isEqualTo(1)
    }

    @Test
    fun clicking_primary_button_does_not_trigger_callback_when_not_enabled() {
        var counter = 0
        setContent(primaryButtonEnabled = false, primaryButtonCallback = { counter++ })
        composeTestRule.onNodeWithText("Save Address").performClick()
        assertThat(counter).isEqualTo(0)
    }

    @Test
    fun clicking_close_button_triggers_callback() {
        var counter = 0
        setContent(onCloseCallback = { counter++ })
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        assertThat(counter).isEqualTo(1)
    }

    @Test
    fun always_dark_theme_is_applied_to_the_address_screen() {
        var isDark = false
        setContent(
            appearance = PaymentSheet.Appearance(
                themeMode = PaymentSheet.ThemeMode.AlwaysDark,
            ),
            formContent = {
                isDark = MaterialTheme.stripeThemeIsDark
            },
        )

        composeTestRule.waitForIdle()

        assertThat(isDark).isTrue()
    }

    @Test
    fun always_light_theme_is_applied_to_the_address_screen() {
        var isDark = true
        setContent(
            appearance = PaymentSheet.Appearance(
                themeMode = PaymentSheet.ThemeMode.AlwaysLight,
            ),
            formContent = {
                isDark = MaterialTheme.stripeThemeIsDark
            },
        )

        composeTestRule.waitForIdle()

        assertThat(isDark).isFalse()
    }

    @Test
    fun form_bottom_inset_is_applied_to_the_form_content() {
        val heightWithoutBottomInset = formContentHeight(bottomInsetDp = 0f)
        val heightWithBottomInset = formContentHeight(bottomInsetDp = BOTTOM_INSET_DP)

        assertThat(heightWithBottomInset - heightWithoutBottomInset).isEqualTo(
            with(composeTestRule.density) {
                BOTTOM_INSET_DP.dp.roundToPx()
            }
        )
    }

    private fun formContentHeight(bottomInsetDp: Float): Int {
        setContent(
            appearance = PaymentSheet.Appearance(
                formInsetValues = PaymentSheet.Insets(0f, 0f, 0f, bottomInsetDp),
            ),
            formContent = {
                Box(modifier = Modifier.height(100.dp))
            },
        )
        composeTestRule.waitForIdle()
        return composeTestRule
            .onNodeWithTag(INPUT_ADDRESS_FORM_CONTENT_TEST_TAG)
            .fetchSemanticsNode()
            .size
            .height
    }

    private companion object {
        const val BOTTOM_INSET_DP = 40f
    }

    private fun setContent(
        appearance: PaymentSheet.Appearance = PaymentSheet.Appearance(),
        primaryButtonEnabled: Boolean = true,
        primaryButtonCallback: () -> Unit = {},
        onCloseCallback: () -> Unit = {},
        formContent: @Composable ColumnScope.() -> Unit = {},
    ) {
        composeTestRule.setContent {
            InputAddressScreen(
                appearance = appearance,
                primaryButtonEnabled = primaryButtonEnabled,
                primaryButtonText = "Save Address",
                title = "Address",
                onPrimaryButtonClick = primaryButtonCallback,
                onDisabledButtonClick = {},
                onCloseClick = onCloseCallback,
                topContent = {},
                formContent = formContent,
                bottomContent = {},
            )
        }
    }
}
