@file:OptIn(
    com.stripe.android.paymentelement.AppearanceAPIAdditionsPreview::class,
    com.stripe.android.paymentelement.CheckoutSessionPreview::class,
)

package com.stripe.android.paymentsheet.addresselement

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.checkout.asPaymentSheet
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.elements.ShippingAddressElement
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.ui.PaymentElementTheme
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.ui.core.FormUI
import com.stripe.android.uicore.LocalFormScrollContext
import com.stripe.android.uicore.elements.FormFieldId
import com.stripe.android.uicore.utils.collectAsState
import kotlinx.coroutines.delay
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.stripe.android.elements.ShippingAddressElement.Configuration.Appearance as ShippingAppearance

@RunWith(TestParameterInjector::class)
internal class InputAddressScreenScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        includeStripeTheme = false,
        boxModifier = Modifier.fillMaxWidth(),
    )

    @Test
    fun `default appearance renders the default appearance`(
        @TestParameter appearanceSource: AppearanceSource,
    ) {
        val appearance = when (appearanceSource) {
            AppearanceSource.LegacyAddressElement -> PaymentSheet.Appearance()
            AppearanceSource.ShippingAddressElement ->
                ShippingAddressElement.Configuration().build().appearance.asPaymentSheet()
        }

        snapshot(appearance = appearance, appearanceSource = appearanceSource)
    }

    @Test
    fun `custom appearance renders in every theme mode`(
        @TestParameter appearanceSource: AppearanceSource,
        @TestParameter themeMode: PaymentSheet.ThemeMode,
    ) {
        val appearance = when (appearanceSource) {
            AppearanceSource.LegacyAddressElement -> configuredLegacyAppearance(themeMode)
            AppearanceSource.ShippingAddressElement -> configuredShippingAppearance(themeMode)
        }

        snapshot(
            appearance = appearance,
            appearanceSource = appearanceSource,
            title = "Checkout shipping address",
            primaryButtonText = "Use this address",
        )
    }

    @Test
    fun `default form layout is visible at the bottom of scroll`() {
        paparazziRule.snapshot {
            Box(modifier = Modifier.height(500.dp)) {
                InputAddressTestScreen(
                    config = AddressLauncher.Configuration(),
                    title = "Checkout shipping address",
                    primaryButtonText = "Use this address",
                    scrollToBottom = true,
                )
            }
        }
    }

    @Test
    fun `save error renders above the primary button`() {
        paparazziRule.snapshot {
            val formController = remember { createFilledFormController() }

            SaveErrorTestScreen(
                formController = formController,
                saveError = SAVE_ERROR,
            )
        }
    }

    @Test
    fun `save error clears on edit before validation error shows`() {
        paparazziRule.gif(end = 3000L) {
            val formController = remember { createFilledFormController() }
            var saveError by remember { mutableStateOf<ResolvableString?>(SAVE_ERROR) }

            LaunchedEffect(Unit) {
                delay(1000L)
                formController.setRawValues(FILLED_VALUES + (FormFieldId.Line1 to ""))
                saveError = null
                delay(1000L)
                formController.elements.forEach { it.onValidationStateChanged(true) }
            }

            SaveErrorTestScreen(
                formController = formController,
                saveError = saveError,
            )
        }
    }

    private fun snapshot(
        appearance: PaymentSheet.Appearance,
        appearanceSource: AppearanceSource,
        title: String? = null,
        primaryButtonText: String? = null,
    ) {
        val config = AddressLauncher.Configuration(
            appearance = appearance,
            additionalFields = when (appearanceSource) {
                AppearanceSource.LegacyAddressElement -> null
                AppearanceSource.ShippingAddressElement -> AddressLauncher.AdditionalFieldsConfiguration(
                    phone = AddressLauncher.AdditionalFieldsConfiguration.FieldConfiguration.HIDDEN,
                )
            },
        )

        paparazziRule.snapshot {
            InputAddressTestScreen(
                config = config,
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
        config: AddressLauncher.Configuration,
        title: String,
        primaryButtonText: String,
        scrollToBottom: Boolean = false,
    ) {
        val addressFormController = remember {
            AddressFormController(
                initialValues = emptyMap(),
                config = config,
                interactor = TestAutocompleteAddressInteractor.noOp(),
            )
        }

        PaymentElementTheme(appearance = config.appearance) {
            InputAddressScreen(
                primaryButtonEnabled = addressFormController.completeFormValues.value != null,
                primaryButtonLoading = false,
                closeButtonEnabled = true,
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
                saveError = null,
            )
        }
    }

    @Composable
    private fun SaveErrorTestScreen(
        formController: AddressFormController,
        saveError: ResolvableString?,
    ) {
        val completeValues by formController.completeFormValues.collectAsState()

        PaymentElementTheme(appearance = PaymentSheet.Appearance()) {
            InputAddressScreen(
                primaryButtonEnabled = completeValues != null,
                primaryButtonLoading = false,
                closeButtonEnabled = true,
                primaryButtonText = stringResource(R.string.stripe_paymentsheet_address_element_primary_button),
                title = stringResource(R.string.stripe_paymentsheet_address_element_shipping_address),
                onPrimaryButtonClick = {},
                onDisabledButtonClick = {},
                onCloseClick = {},
                topContent = {},
                formContent = {
                    FormUI(
                        hiddenIdentifiers = emptySet(),
                        enabled = true,
                        elements = formController.elements,
                        lastTextFieldIdentifier = formController.lastTextFieldIdentifier.value,
                    )
                },
                bottomContent = {},
                saveError = saveError,
            )
        }
    }

    private fun createFilledFormController() = AddressFormController(
        initialValues = FILLED_VALUES,
        config = AddressLauncher.Configuration(),
        interactor = TestAutocompleteAddressInteractor.noOp(),
    )

    @Composable
    private fun ScrollToBottom() {
        val scrollState = LocalFormScrollContext.current?.scrollState
        val maxScroll = scrollState?.maxValue ?: 0

        LaunchedEffect(scrollState, maxScroll) {
            scrollState?.scrollTo(maxScroll)
        }
    }

    private fun configuredLegacyAppearance(
        themeMode: PaymentSheet.ThemeMode,
    ) = PaymentSheet.Appearance(
        // Match Material's background colors so contentColorFor would choose onBackground
        // instead of our custom onSurface unless the screen sets contentColor explicitly.
        colorsLight = configuredLegacyColorsLight(),
        colorsDark = configuredLegacyColorsDark(),
        themeMode = themeMode,
        shapes = PaymentSheet.Shapes(cornerRadiusDp = 16f, borderStrokeWidthDp = 3f),
        typography = PaymentSheet.Typography(
            sizeScaleFactor = 1.2f,
            fontResId = R.font.cursive,
            custom = PaymentSheet.Typography.Custom(
                h1 = PaymentSheet.Typography.Font(
                    fontFamily = R.font.cursive,
                    fontSizeSp = 26f,
                    fontWeight = 700,
                    letterSpacingSp = 1f,
                )
            ),
        ),
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

    private fun configuredLegacyColorsLight() = PaymentSheet.Colors(
        primary = Color(0xFF0057B8),
        surface = Color.White,
        component = Color(0xFFE0ECFF),
        componentBorder = Color(0xFF0057B8),
        componentDivider = Color(0xFF8AA4C5),
        onComponent = Color(0xFF071A2F),
        subtitle = Color(0xFF3E5A75),
        placeholderText = Color(0xFF5F7891),
        onSurface = Color(0xFFAD167E),
        appBarIcon = Color(0xFF0057B8),
        error = Color(0xFFB00020),
    )

    private fun configuredLegacyColorsDark() = PaymentSheet.Colors(
        primary = Color(0xFF8CC8FF),
        surface = Color(0xFF121212),
        component = Color(0xFF203448),
        componentBorder = Color(0xFF8CC8FF),
        componentDivider = Color(0xFF64809B),
        onComponent = Color.White,
        subtitle = Color(0xFFB7C9D9),
        placeholderText = Color(0xFF9FB3C5),
        onSurface = Color.Cyan,
        appBarIcon = Color(0xFF8CC8FF),
        error = Color(0xFFFF8A80),
    )

    private fun configuredShippingAppearance(
        themeMode: PaymentSheet.ThemeMode,
    ): PaymentSheet.Appearance {
        val appearance = ShippingAppearance()
            .colorsLight(configuredShippingColorsLight())
            .colorsDark(configuredShippingColorsDark())
            .themeMode(
                when (themeMode) {
                    PaymentSheet.ThemeMode.Automatic -> ShippingAppearance.ThemeMode.Automatic
                    PaymentSheet.ThemeMode.AlwaysLight -> ShippingAppearance.ThemeMode.AlwaysLight
                    PaymentSheet.ThemeMode.AlwaysDark -> ShippingAppearance.ThemeMode.AlwaysDark
                }
            )
            .primaryButton(configuredShippingPrimaryButton())

        return ShippingAddressElement.Configuration().appearance(appearance).build().appearance.asPaymentSheet()
    }

    private fun configuredShippingColorsLight() = ShippingAppearance.Colors.light()
        .primary(Color(0xFF0057B8))
        .surface(Color.White)
        .component(Color(0xFFE0ECFF))
        .componentBorder(Color(0xFF0057B8))
        .componentDivider(Color(0xFF8AA4C5))
        .onComponent(Color(0xFF071A2F))
        .subtitle(Color(0xFF3E5A75))
        .placeholderText(Color(0xFF5F7891))
        .onSurface(Color(0xFFAD167E))
        .appBarIcon(Color(0xFF0057B8))
        .error(Color(0xFFB00020))

    private fun configuredShippingColorsDark() = ShippingAppearance.Colors.dark()
        .primary(Color(0xFF8CC8FF))
        .surface(Color(0xFF121212))
        .component(Color(0xFF203448))
        .componentBorder(Color(0xFF8CC8FF))
        .componentDivider(Color(0xFF64809B))
        .onComponent(Color.White)
        .subtitle(Color(0xFFB7C9D9))
        .placeholderText(Color(0xFF9FB3C5))
        .onSurface(Color.Cyan)
        .appBarIcon(Color(0xFF8CC8FF))
        .error(Color(0xFFFF8A80))

    private fun configuredShippingPrimaryButton() = ShippingAppearance.PrimaryButton()
        .colorsLight(
            ShippingAppearance.PrimaryButton.Colors.light()
                .background(Color(0xFF0057B8))
                .onBackground(Color.White)
                .border(Color(0xFF003B7A))
        )
        .colorsDark(
            ShippingAppearance.PrimaryButton.Colors.dark()
                .background(Color(0xFF8CC8FF))
                .onBackground(Color(0xFF071A2F))
                .border(Color.White)
        )
        .shape(
            ShippingAppearance.PrimaryButton.Shape()
                .cornerRadiusDp(28f)
                .borderStrokeWidthDp(2f)
                .heightDp(56f)
        )
        .typography(ShippingAppearance.PrimaryButton.Typography().fontSizeSp(18f))

    enum class AppearanceSource {
        LegacyAddressElement,
        ShippingAddressElement,
    }

    private companion object {
        val SAVE_ERROR = "Taxes can't be calculated for this address.".resolvableString
        val FILLED_VALUES = mapOf(
            FormFieldId.Name to "Jenny Rosen",
            FormFieldId.Country to "US",
            FormFieldId.Line1 to "510 Townsend St",
            FormFieldId.City to "San Francisco",
            FormFieldId.State to "CA",
            FormFieldId.PostalCode to "94103",
        )
    }
}
