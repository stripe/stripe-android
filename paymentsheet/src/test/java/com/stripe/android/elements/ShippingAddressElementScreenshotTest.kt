@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.elements

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.testing.TestLifecycleOwner
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.checkout.CheckoutController
import com.stripe.android.checkout.CheckoutControllerStateFactory
import com.stripe.android.checkout.ShippingAddressElementStateHolder
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.addresselement.AddressElementActivityContract
import com.stripe.android.paymentsheet.addresselement.AddressFormController
import com.stripe.android.paymentsheet.addresselement.AddressLauncher
import com.stripe.android.paymentsheet.addresselement.InputAddressScreen
import com.stripe.android.paymentsheet.addresselement.TestAutocompleteAddressInteractor
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.DummyActivityResultCaller
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.ui.core.FormUI
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Provider

@RunWith(TestParameterInjector::class)
internal class ShippingAddressElementScreenshotTest {
    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        includeStripeTheme = false,
        boxModifier = Modifier.fillMaxWidth(),
    )

    @Test
    fun `default configuration renders the default appearance`() {
        snapshot(ShippingAddressElement.Configuration())
    }

    @Test
    fun `configured appearance renders in every theme mode`(
        @TestParameter themeMode: ShippingAddressElement.Configuration.Appearance.ThemeMode,
    ) {
        snapshot(configuredShippingAddressElement(themeMode))
    }

    private fun snapshot(configuration: ShippingAddressElement.Configuration) = runTest {
        val coroutineScope: CoroutineScope = this

        DummyActivityResultCaller.test {
            val savedStateHandle = SavedStateHandle()
            val stateHolder = CheckoutControllerStateFactory.createStateHolder(savedStateHandle)
            stateHolder.state = CheckoutControllerStateFactory.create(
                configuration = CheckoutController.Configuration()
                    .shippingAddressElement(configuration)
                    .build(),
            )
            val errorReporter = FakeErrorReporter()
            val shippingAddressElement = ShippingAddressElement(
                activityResultCaller = activityResultCaller,
                lifecycleOwner = TestLifecycleOwner(),
                paymentConfiguration = Provider {
                    PaymentConfiguration(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY)
                },
                coroutineScope = coroutineScope,
                commitShippingAddress = CommitShippingAddress { _, _, _ -> Result.success(Unit) },
                stateHolder = stateHolder,
                shippingAddressElementStateHolder = ShippingAddressElementStateHolder(savedStateHandle),
                errorReporter = errorReporter,
            )

            val registerCall = awaitRegisterCall()
            assertThat(registerCall.contract).isSameInstanceAs(AddressElementActivityContract.CheckoutShipping)
            awaitNextRegisteredLauncher()

            shippingAddressElement.present()

            val launchArgs = awaitLaunchCall()
            assertThat(launchArgs).isInstanceOf(AddressElementActivityContract.Args.CheckoutShipping::class.java)
            val launchedConfiguration = requireNotNull(
                (launchArgs as AddressElementActivityContract.Args.CheckoutShipping).config
            )

            paparazziRule.snapshot {
                ShippingAddressElementScreen(launchedConfiguration)
            }

            errorReporter.ensureAllEventsConsumed()
        }
    }

    @Composable
    private fun ShippingAddressElementScreen(
        configuration: AddressLauncher.Configuration,
    ) {
        val addressFormController = remember(configuration) {
            AddressFormController(
                initialValues = emptyMap(),
                config = configuration,
                interactor = TestAutocompleteAddressInteractor.noOp(
                    autocompleteConfig = AutocompleteAddressInteractor.Config(
                        googlePlacesApiKey = configuration.googlePlacesApiKey,
                        autocompleteCountries = configuration.autocompleteCountries,
                        isInlineAutocompleteEnabled = true,
                        shouldUseStripeHostedAutocomplete = configuration.useStripeHostedAutocomplete,
                    )
                ),
            )
        }

        InputAddressScreen(
            appearance = configuration.appearance,
            primaryButtonEnabled = addressFormController.completeFormValues.value != null,
            primaryButtonText = configuration.buttonTitle ?: stringResource(
                R.string.stripe_paymentsheet_address_element_primary_button
            ),
            title = configuration.title ?: stringResource(
                R.string.stripe_paymentsheet_address_element_shipping_address
            ),
            onPrimaryButtonClick = {},
            onDisabledButtonClick = {},
            onCloseClick = {},
            topContent = {},
            formContent = {
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

    private fun configuredShippingAddressElement(
        themeMode: ShippingAddressElement.Configuration.Appearance.ThemeMode,
    ): ShippingAddressElement.Configuration {
        return ShippingAddressElement.Configuration()
            .title("Checkout shipping address")
            .buttonTitle("Use this address")
            .appearance(configuredAppearance(themeMode))
    }

    private fun configuredAppearance(
        themeMode: ShippingAddressElement.Configuration.Appearance.ThemeMode,
    ): ShippingAddressElement.Configuration.Appearance {
        return ShippingAddressElement.Configuration.Appearance()
            .colorsLight(configuredLightColors())
            .colorsDark(configuredDarkColors())
            .themeMode(themeMode)
            .primaryButton(configuredPrimaryButton())
            .formInsetValues(
                ShippingAddressElement.Configuration.Appearance.Insets(
                    startDp = 24f,
                    topDp = 12f,
                    endDp = 36f,
                    bottomDp = 28f,
                )
            )
    }

    private fun configuredLightColors() =
        ShippingAddressElement.Configuration.Appearance.Colors.light()
            .primary(Color(0xFF0057B8))
            .surface(Color(0xFFF7F9FC))
            .component(Color(0xFFE0ECFF))
            .componentBorder(Color(0xFF0057B8))
            .componentDivider(Color(0xFF8AA4C5))
            .onComponent(Color(0xFF071A2F))
            .subtitle(Color(0xFF3E5A75))
            .placeholderText(Color(0xFF5F7891))
            .onSurface(Color(0xFF071A2F))
            .appBarIcon(Color(0xFF0057B8))
            .error(Color(0xFFB00020))

    private fun configuredDarkColors() =
        ShippingAddressElement.Configuration.Appearance.Colors.dark()
            .primary(Color(0xFF8CC8FF))
            .surface(Color(0xFF101820))
            .component(Color(0xFF203448))
            .componentBorder(Color(0xFF8CC8FF))
            .componentDivider(Color(0xFF64809B))
            .onComponent(Color.White)
            .subtitle(Color(0xFFB7C9D9))
            .placeholderText(Color(0xFF9FB3C5))
            .onSurface(Color.White)
            .appBarIcon(Color(0xFF8CC8FF))
            .error(Color(0xFFFF8A80))

    private fun configuredPrimaryButton() =
        ShippingAddressElement.Configuration.Appearance.PrimaryButton()
            .colorsLight(
                ShippingAddressElement.Configuration.Appearance.PrimaryButton.Colors.light()
                    .background(Color(0xFF0057B8))
                    .onBackground(Color.White)
                    .border(Color(0xFF003B7A))
            )
            .colorsDark(
                ShippingAddressElement.Configuration.Appearance.PrimaryButton.Colors.dark()
                    .background(Color(0xFF8CC8FF))
                    .onBackground(Color(0xFF071A2F))
                    .border(Color.White)
            )
            .shape(
                ShippingAddressElement.Configuration.Appearance.PrimaryButton.Shape()
                    .cornerRadiusDp(28f)
                    .borderStrokeWidthDp(2f)
                    .heightDp(56f)
            )
            .typography(
                ShippingAddressElement.Configuration.Appearance.PrimaryButton.Typography()
                    .fontSizeSp(18f)
            )
}
