@file:OptIn(com.stripe.android.paymentelement.AppearanceAPIAdditionsPreview::class)

package com.stripe.android.paymentsheet.addresselement

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.R
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.ui.core.FormUI
import com.stripe.android.uicore.LocalFormScrollContext
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
internal class InputAddressScreenScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        includeStripeTheme = false,
        boxModifier = Modifier.fillMaxWidth(),
    )

    @Test
    fun `default appearance renders the default appearance`() {
        snapshot(PaymentSheet.Appearance())
    }

    @Test
    fun `custom appearance renders in every theme mode`(
        @TestParameter themeMode: PaymentSheet.ThemeMode,
    ) {
        snapshot(
            appearance = configuredAppearance(themeMode),
            title = "Checkout shipping address",
            primaryButtonText = "Use this address",
        )
    }

    @Test
    fun `bottom form inset is visible at the bottom of scroll`() {
        paparazziRule.snapshot {
            Box(modifier = Modifier.height(500.dp)) {
                InputAddressTestScreen(
                    appearance = PaymentSheet.Appearance(
                        formInsetValues = PaymentSheet.Insets(
                            startDp = 20f,
                            topDp = 0f,
                            endDp = 20f,
                            bottomDp = 40f,
                        ),
                    ),
                    title = "Checkout shipping address",
                    primaryButtonText = "Use this address",
                    scrollToBottom = true,
                )
            }
        }
    }

    private fun snapshot(
        appearance: PaymentSheet.Appearance,
        title: String? = null,
        primaryButtonText: String? = null,
    ) {
        paparazziRule.snapshot {
            InputAddressTestScreen(
                appearance = appearance,
                title = title ?: stringResource(
                    R.string.stripe_paymentsheet_address_element_shipping_address
                ),
                primaryButtonText = primaryButtonText ?: stringResource(
                    R.string.stripe_paymentsheet_address_element_primary_button
                ),
            )
        }
    }

    @Composable
    private fun InputAddressTestScreen(
        appearance: PaymentSheet.Appearance,
        title: String,
        primaryButtonText: String,
        scrollToBottom: Boolean = false,
    ) {
        val addressFormController = remember {
            AddressFormController(
                initialValues = emptyMap(),
                config = AddressLauncher.Configuration(),
                interactor = TestAutocompleteAddressInteractor.noOp(),
            )
        }

        InputAddressScreen(
            appearance = appearance,
            primaryButtonEnabled = addressFormController.completeFormValues.value != null,
            primaryButtonText = primaryButtonText,
            title = title,
            onPrimaryButtonClick = {},
            onDisabledButtonClick = {},
            onCloseClick = {},
            topContent = {},
            formContent = {
                if (scrollToBottom) {
                    ScrollToBottom()
                }
                FormUI(
                    hiddenIdentifiers = emptySet(),
                    enabled = true,
                    elements = addressFormController.elements,
                    lastTextFieldIdentifier = addressFormController.lastTextFieldIdentifier.value,
                )
            },
            bottomContent = {},
        )
    }

    @Composable
    private fun ScrollToBottom() {
        val scrollState = LocalFormScrollContext.current?.scrollState
        val maxScroll = scrollState?.maxValue ?: 0

        LaunchedEffect(scrollState, maxScroll) {
            scrollState?.scrollTo(maxScroll)
        }
    }

    private fun configuredAppearance(
        themeMode: PaymentSheet.ThemeMode,
    ) = PaymentSheet.Appearance(
        colorsLight = PaymentSheet.Colors(
            primary = Color(0xFF0057B8),
            surface = Color(0xFFF7F9FC),
            component = Color(0xFFE0ECFF),
            componentBorder = Color(0xFF0057B8),
            componentDivider = Color(0xFF8AA4C5),
            onComponent = Color(0xFF071A2F),
            subtitle = Color(0xFF3E5A75),
            placeholderText = Color(0xFF5F7891),
            onSurface = Color(0xFF071A2F),
            appBarIcon = Color(0xFF0057B8),
            error = Color(0xFFB00020),
        ),
        colorsDark = PaymentSheet.Colors(
            primary = Color(0xFF8CC8FF),
            surface = Color(0xFF101820),
            component = Color(0xFF203448),
            componentBorder = Color(0xFF8CC8FF),
            componentDivider = Color(0xFF64809B),
            onComponent = Color.White,
            subtitle = Color(0xFFB7C9D9),
            placeholderText = Color(0xFF9FB3C5),
            onSurface = Color.White,
            appBarIcon = Color(0xFF8CC8FF),
            error = Color(0xFFFF8A80),
        ),
        themeMode = themeMode,
        primaryButton = PaymentSheet.PrimaryButton(
            colorsLight = PaymentSheet.PrimaryButtonColors(
                background = Color(0xFF0057B8),
                onBackground = Color.White,
                border = Color(0xFF003B7A),
            ),
            colorsDark = PaymentSheet.PrimaryButtonColors(
                background = Color(0xFF8CC8FF),
                onBackground = Color(0xFF071A2F),
                border = Color.White,
            ),
            shape = PaymentSheet.PrimaryButtonShape(
                cornerRadiusDp = 28f,
                borderStrokeWidthDp = 2f,
                heightDp = 56f,
            ),
            typography = PaymentSheet.PrimaryButtonTypography(
                fontResId = null,
                fontSizeSp = 18f,
            ),
        ),
    )
}
