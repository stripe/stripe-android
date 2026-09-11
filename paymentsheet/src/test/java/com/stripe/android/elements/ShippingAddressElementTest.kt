@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.elements

import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.ui.graphics.Color
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.testing.TestLifecycleOwner
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.checkout.CheckoutController
import com.stripe.android.checkout.CheckoutControllerStateFactory
import com.stripe.android.checkout.CheckoutControllerStateHolder
import com.stripe.android.checkout.ShippingAddressElementStateHolder
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AUTOCOMPLETE_DEFAULT_COUNTRIES
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.addresselement.AddressElementActivityContract
import com.stripe.android.paymentsheet.addresselement.AddressLauncher
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.FakeErrorReporter
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import javax.inject.Provider

internal class ShippingAddressElementTest {
    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `present before checkout configuration reports and does not launch`() = runScenario(configured = false) {
        shippingAddressElement.present()

        val call = errorReporter.awaitCall()
        assertThat(call.errorEvent).isEqualTo(
            ErrorReporter.ExpectedErrorEvent.CHECKOUT_SHIPPING_ADDRESS_ELEMENT_PRESENT_NOT_CONFIGURED
        )
        assertThat(call.errorEvent.eventName).isEqualTo(
            "checkout.shipping_address_element.present.not_configured"
        )
        assertThat(call.stripeException).isNull()
        assertThat(call.additionalNonPiiParams).isEmpty()
        activityLauncher.launchCalls.expectNoEvents()
        paymentConfiguration.getCalls.expectNoEvents()
    }

    @Test
    fun `present launches a blank address form with hosted autocomplete`() = runScenario {
        shippingAddressElement.present()

        val launch = activityLauncher.launchCalls.awaitItem()
        assertThat(launch.input.publishableKey).isEqualTo(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY)

        val config = requireNotNull(launch.input.config)
        assertThat(config.appearance).isEqualTo(PaymentSheet.Appearance())
        assertThat(config.address).isNull()
        assertThat(config.allowedCountries).isEmpty()
        assertThat(config.buttonTitle).isNull()
        assertThat(config.additionalFields?.phone)
            .isEqualTo(AddressLauncher.AdditionalFieldsConfiguration.FieldConfiguration.HIDDEN)
        assertThat(config.additionalFields?.checkboxLabel).isNull()
        assertThat(config.title).isNull()
        assertThat(config.googlePlacesApiKey).isNull()
        assertThat(config.autocompleteCountries).isEqualTo(AUTOCOMPLETE_DEFAULT_COUNTRIES)
        assertThat(config.billingAddress).isNull()
        assertThat(config.useStripeHostedAutocomplete).isTrue()
        assertThat(paymentConfiguration.getCalls.awaitItem()).isEqualTo(Unit)
    }

    @Test
    fun `present passes full ShippingAddressElement configuration to the address form`() {
        val appearance = configuredAppearance()
        val configuration = CheckoutController.Configuration()
            .shippingAddressElement(
                ShippingAddressElement.Configuration()
                    .title("Shipping address")
                    .buttonTitle("Use this address")
                    .appearance(appearance)
            )
            .build()

        runScenario(configuration = configuration) {
            shippingAddressElement.present()

            val config = requireNotNull(activityLauncher.launchCalls.awaitItem().input.config)
            assertThat(config.title).isEqualTo("Shipping address")
            assertThat(config.buttonTitle).isEqualTo("Use this address")

            assertAppearance(config.appearance)
            assertThat(paymentConfiguration.getCalls.awaitItem()).isEqualTo(Unit)
        }
    }

    @Test
    fun `present suppresses duplicate presentations`() = runScenario {
        shippingAddressElement.present()
        shippingAddressElement.present()

        activityLauncher.launchCalls.awaitItem()
        activityLauncher.launchCalls.expectNoEvents()
        assertThat(paymentConfiguration.getCalls.awaitItem()).isEqualTo(Unit)
    }

    @Test
    fun `recreated element suppresses presentation while original is active`() = runScenario {
        shippingAddressElement.present()
        activityLauncher.launchCalls.awaitItem()
        assertThat(paymentConfiguration.getCalls.awaitItem()).isEqualTo(Unit)

        val recreated = createElement()
        recreated.shippingAddressElement.present()

        recreated.activityLauncher.launchCalls.expectNoEvents()
        assertThat(shippingAddressElementStateHolder.isPresenting).isTrue()
        recreated.ensureAllEventsConsumed()
    }

    @Test
    fun `present resolves the latest payment configuration`() = runScenario {
        shippingAddressElement.present()

        val firstLaunch = activityLauncher.launchCalls.awaitItem()
        assertThat(firstLaunch.input.publishableKey).isEqualTo(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY)
        assertThat(paymentConfiguration.getCalls.awaitItem()).isEqualTo(Unit)

        registration.dispatch(AddressElementActivityContract.Result.Canceled)
        paymentConfiguration.value = PaymentConfiguration(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)

        shippingAddressElement.present()

        val secondLaunch = activityLauncher.launchCalls.awaitItem()
        assertThat(secondLaunch.input.publishableKey).isEqualTo(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        assertThat(paymentConfiguration.getCalls.awaitItem()).isEqualTo(Unit)
    }

    @Test
    fun `successful result clears presentation and commits complete address`() = runScenario {
        shippingAddressElement.present()
        activityLauncher.launchCalls.awaitItem()

        val addressDetails = AddressDetails(
            name = "Jenny Rosen",
            address = PaymentSheet.Address(
                city = "San Francisco",
                country = "US",
                line1 = "510 Townsend St",
                line2 = "Floor 2",
                postalCode = "94103",
                state = "CA",
            ),
        )
        registration.dispatch(
            AddressElementActivityContract.Result.CheckoutShippingSucceeded(addressDetails)
        )

        assertThat(commitShippingAddress.calls.awaitItem()).isEqualTo(
            FakeCommitShippingAddress.Call(
                name = addressDetails.name,
                address = CheckoutController.Address.State(
                    city = "San Francisco",
                    country = "US",
                    line1 = "510 Townsend St",
                    line2 = "Floor 2",
                    postalCode = "94103",
                    state = "CA",
                ),
            )
        )
        assertThat(shippingAddressElementStateHolder.isPresenting).isFalse()

        shippingAddressElement.present()
        activityLauncher.launchCalls.awaitItem()
        assertThat(paymentConfiguration.getCalls.awaitItem()).isEqualTo(Unit)
        assertThat(paymentConfiguration.getCalls.awaitItem()).isEqualTo(Unit)
    }

    @Test
    fun `successful result suppresses presentation until commit completes`() {
        val commitResult = CompletableDeferred<Result<Unit>>()

        runScenario(
            configured = true,
            configuration = CheckoutControllerStateFactory.create().configuration,
            commitShippingAddress = FakeCommitShippingAddress(commitResult),
        ) {
            shippingAddressElement.present()
            activityLauncher.launchCalls.awaitItem()
            assertThat(paymentConfiguration.getCalls.awaitItem()).isEqualTo(Unit)

            registration.dispatch(
                AddressElementActivityContract.Result.CheckoutShippingSucceeded(
                    AddressDetails(
                        name = "Jenny Rosen",
                        address = PaymentSheet.Address(
                            city = "San Francisco",
                            country = "US",
                            line1 = "510 Townsend St",
                            postalCode = "94103",
                            state = "CA",
                        ),
                    ),
                )
            )
            commitShippingAddress.calls.awaitItem()
            assertThat(shippingAddressElementStateHolder.isPresenting).isTrue()

            shippingAddressElement.present()
            activityLauncher.launchCalls.expectNoEvents()

            commitResult.complete(Result.success(Unit))
            assertThat(shippingAddressElementStateHolder.isPresenting).isFalse()

            shippingAddressElement.present()
            activityLauncher.launchCalls.awaitItem()
            assertThat(paymentConfiguration.getCalls.awaitItem()).isEqualTo(Unit)
        }
    }

    @Test
    fun `canceled result clears presentation without committing`() = runScenario {
        shippingAddressElement.present()
        activityLauncher.launchCalls.awaitItem()
        assertThat(paymentConfiguration.getCalls.awaitItem()).isEqualTo(Unit)

        registration.dispatch(AddressElementActivityContract.Result.Canceled)

        assertThat(shippingAddressElementStateHolder.isPresenting).isFalse()
        commitShippingAddress.calls.expectNoEvents()
    }

    @Test
    fun `malformed successful result clears presentation without committing`() = runScenario {
        shippingAddressElement.present()
        activityLauncher.launchCalls.awaitItem()
        assertThat(paymentConfiguration.getCalls.awaitItem()).isEqualTo(Unit)

        registration.dispatch(
            AddressElementActivityContract.Result.CheckoutShippingSucceeded(
                AddressDetails(
                    name = "Missing country",
                    address = PaymentSheet.Address(
                        line1 = "510 Townsend St",
                        country = " ",
                    ),
                ),
            )
        )

        assertThat(shippingAddressElementStateHolder.isPresenting).isFalse()
        commitShippingAddress.calls.expectNoEvents()
    }

    @Test
    fun `recreated element result clears presentation after host destruction`() = runScenario {
        shippingAddressElement.present()
        activityLauncher.launchCalls.awaitItem()
        assertThat(paymentConfiguration.getCalls.awaitItem()).isEqualTo(Unit)

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        activityLauncher.unregisterCalls.awaitItem()
        assertThat(shippingAddressElementStateHolder.isPresenting).isTrue()

        val recreated = createElement()
        recreated.shippingAddressElement.present()
        recreated.activityLauncher.launchCalls.expectNoEvents()

        recreated.registration.dispatch(AddressElementActivityContract.Result.Canceled)
        assertThat(shippingAddressElementStateHolder.isPresenting).isFalse()

        recreated.shippingAddressElement.present()
        recreated.activityLauncher.launchCalls.awaitItem()
        assertThat(paymentConfiguration.getCalls.awaitItem()).isEqualTo(Unit)
        recreated.ensureAllEventsConsumed()
    }

    @Test
    fun `lifecycle destruction unregisters the launcher`() = runScenario {
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        assertThat(activityLauncher.unregisterCalls.awaitItem()).isEqualTo(Unit)
    }

    private fun runScenario(
        configured: Boolean = true,
        configuration: CheckoutController.Configuration.State =
            CheckoutControllerStateFactory.create().configuration,
        block: suspend Scenario.() -> Unit,
    ) = runScenario(
        configured = configured,
        configuration = configuration,
        commitShippingAddress = FakeCommitShippingAddress(
            CompletableDeferred(Result.success(Unit)),
        ),
        block = block,
    )

    private fun runScenario(
        configured: Boolean,
        configuration: CheckoutController.Configuration.State,
        commitShippingAddress: FakeCommitShippingAddress,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val savedStateHandle = SavedStateHandle()
        val stateHolder = CheckoutControllerStateFactory.createStateHolder(
            savedStateHandle = savedStateHandle,
        )
        if (configured) {
            stateHolder.state = CheckoutControllerStateFactory.create(configuration = configuration)
        }
        val shippingAddressElementStateHolder = ShippingAddressElementStateHolder(savedStateHandle)
        val paymentConfiguration = RecordingProvider(
            PaymentConfiguration(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY),
        )
        val errorReporter = FakeErrorReporter()
        val coroutineScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        suspend fun createElement(): ElementScenario {
            val activityResultCaller = RecordingActivityResultCaller()
            val lifecycleOwner = TestLifecycleOwner()
            val shippingAddressElement = ShippingAddressElement(
                activityResultCaller = activityResultCaller,
                lifecycleOwner = lifecycleOwner,
                paymentConfiguration = paymentConfiguration,
                coroutineScope = coroutineScope,
                commitShippingAddress = commitShippingAddress,
                stateHolder = stateHolder,
                shippingAddressElementStateHolder = shippingAddressElementStateHolder,
                errorReporter = errorReporter,
            )
            val registration = activityResultCaller.registerCalls.awaitItem()
            assertThat(registration.contract).isSameInstanceAs(AddressElementActivityContract.CheckoutShipping)
            return ElementScenario(
                shippingAddressElement = shippingAddressElement,
                activityResultCaller = activityResultCaller,
                activityLauncher = activityResultCaller.launcher,
                lifecycleOwner = lifecycleOwner,
                registration = registration,
            )
        }

        val element = createElement()

        Scenario(
            shippingAddressElement = element.shippingAddressElement,
            activityLauncher = element.activityLauncher,
            lifecycleOwner = element.lifecycleOwner,
            stateHolder = stateHolder,
            shippingAddressElementStateHolder = shippingAddressElementStateHolder,
            commitShippingAddress = commitShippingAddress,
            paymentConfiguration = paymentConfiguration,
            errorReporter = errorReporter,
            registration = element.registration,
            createElement = ::createElement,
        ).block()

        element.ensureAllEventsConsumed()
        paymentConfiguration.getCalls.ensureAllEventsConsumed()
        errorReporter.ensureAllEventsConsumed()
        commitShippingAddress.ensureAllEventsConsumed()
    }

    private class RecordingActivityResultCaller : ActivityResultCaller {
        val registerCalls = Turbine<Registration>()
        val launcher = RecordingActivityResultLauncher()

        override fun <I : Any?, O : Any?> registerForActivityResult(
            contract: ActivityResultContract<I, O>,
            callback: ActivityResultCallback<O>,
        ): ActivityResultLauncher<I> {
            registerCalls.add(Registration(contract, callback))
            @Suppress("UNCHECKED_CAST")
            return launcher as ActivityResultLauncher<I>
        }

        override fun <I : Any?, O : Any?> registerForActivityResult(
            contract: ActivityResultContract<I, O>,
            registry: ActivityResultRegistry,
            callback: ActivityResultCallback<O>,
        ): ActivityResultLauncher<I> = error("The registry overload is not used in this test")
    }

    private class RecordingActivityResultLauncher :
        ActivityResultLauncher<AddressElementActivityContract.Args.CheckoutShipping>() {
        val launchCalls = Turbine<LaunchCall>()
        val unregisterCalls = Turbine<Unit>()

        override fun launch(
            input: AddressElementActivityContract.Args.CheckoutShipping,
            options: ActivityOptionsCompat?,
        ) {
            launchCalls.add(LaunchCall(input))
        }

        override fun unregister() {
            unregisterCalls.add(Unit)
        }

        override val contract:
            ActivityResultContract<AddressElementActivityContract.Args.CheckoutShipping, *>
            get() = AddressElementActivityContract.CheckoutShipping
    }

    private class RecordingProvider<T>(
        var value: T,
    ) : Provider<T> {
        val getCalls = Turbine<Unit>()

        override fun get(): T {
            getCalls.add(Unit)
            return value
        }
    }

    private data class Registration(
        val contract: ActivityResultContract<*, *>,
        val callback: ActivityResultCallback<*>,
    ) {
        @Suppress("UNCHECKED_CAST")
        fun dispatch(result: AddressElementActivityContract.CheckoutShippingResult) {
            (callback as ActivityResultCallback<AddressElementActivityContract.CheckoutShippingResult>)
                .onActivityResult(result)
        }
    }

    private data class LaunchCall(
        val input: AddressElementActivityContract.Args.CheckoutShipping,
    )

    private data class ElementScenario(
        val shippingAddressElement: ShippingAddressElement,
        val activityResultCaller: RecordingActivityResultCaller,
        val activityLauncher: RecordingActivityResultLauncher,
        val lifecycleOwner: TestLifecycleOwner,
        val registration: Registration,
    ) {
        fun ensureAllEventsConsumed() {
            activityResultCaller.registerCalls.ensureAllEventsConsumed()
            activityLauncher.launchCalls.ensureAllEventsConsumed()
            activityLauncher.unregisterCalls.ensureAllEventsConsumed()
        }
    }

    private data class Scenario(
        val shippingAddressElement: ShippingAddressElement,
        val activityLauncher: RecordingActivityResultLauncher,
        val lifecycleOwner: TestLifecycleOwner,
        val stateHolder: CheckoutControllerStateHolder,
        val shippingAddressElementStateHolder: ShippingAddressElementStateHolder,
        val commitShippingAddress: FakeCommitShippingAddress,
        val paymentConfiguration: RecordingProvider<PaymentConfiguration>,
        val errorReporter: FakeErrorReporter,
        val registration: Registration,
        val createElement: suspend () -> ElementScenario,
    )

    private fun assertAppearance(appearance: PaymentSheet.Appearance) {
        assertFormColors(appearance)
        assertThat(appearance.themeMode).isEqualTo(PaymentSheet.ThemeMode.AlwaysDark)
        assertPrimaryButton(appearance)
        assertThat(appearance.formInsetValues).isEqualTo(
            PaymentSheet.Insets(
                startDp = 1f,
                topDp = 2f,
                endDp = 3f,
                bottomDp = 4f,
            )
        )
    }

    private fun assertFormColors(appearance: PaymentSheet.Appearance) {
        assertThat(appearance.colorsLight).isEqualTo(
            PaymentSheet.Colors(
                primary = Color.Red,
                surface = Color.Green,
                component = Color.Blue,
                componentBorder = Color.Yellow,
                componentDivider = Color.Cyan,
                onComponent = Color.Magenta,
                subtitle = Color.Gray,
                placeholderText = Color.DarkGray,
                onSurface = Color.White,
                appBarIcon = Color.Black,
                error = Color.LightGray,
            )
        )
        assertThat(appearance.colorsDark).isEqualTo(
            PaymentSheet.Colors(
                primary = Color.Magenta,
                surface = Color.Cyan,
                component = Color.Yellow,
                componentBorder = Color.Red,
                componentDivider = Color.Green,
                onComponent = Color.Blue,
                subtitle = Color.DarkGray,
                placeholderText = Color.Gray,
                onSurface = Color.Black,
                appBarIcon = Color.White,
                error = Color.LightGray,
            )
        )
    }

    private fun assertPrimaryButton(appearance: PaymentSheet.Appearance) {
        assertThat(appearance.primaryButton.colorsLight).isEqualTo(
            PaymentSheet.PrimaryButtonColors(
                background = Color.Green,
                onBackground = Color.White,
                border = Color.Black,
            )
        )
        assertThat(appearance.primaryButton.colorsDark).isEqualTo(
            PaymentSheet.PrimaryButtonColors(
                background = Color.Blue,
                onBackground = Color.Yellow,
                border = Color.Red,
            )
        )
        assertThat(appearance.primaryButton.shape).isEqualTo(
            PaymentSheet.PrimaryButtonShape(
                cornerRadiusDp = 12f,
                borderStrokeWidthDp = 2f,
                heightDp = 48f,
            )
        )
        assertThat(appearance.primaryButton.typography).isEqualTo(
            PaymentSheet.PrimaryButtonTypography(
                fontResId = 123,
                fontSizeSp = 18f,
            )
        )
    }

    private fun configuredAppearance() = ShippingAddressElement.Configuration.Appearance()
        .colorsLight(configuredLightColors())
        .colorsDark(configuredDarkColors())
        .themeMode(ShippingAddressElement.Configuration.Appearance.ThemeMode.AlwaysDark)
        .primaryButton(configuredPrimaryButton())
        .formInsetValues(
            ShippingAddressElement.Configuration.Appearance.Insets(1f, 2f, 3f, 4f)
        )

    private fun configuredLightColors() =
        ShippingAddressElement.Configuration.Appearance.Colors.light()
            .primary(Color.Red)
            .surface(Color.Green)
            .component(Color.Blue)
            .componentBorder(Color.Yellow)
            .componentDivider(Color.Cyan)
            .onComponent(Color.Magenta)
            .subtitle(Color.Gray)
            .placeholderText(Color.DarkGray)
            .onSurface(Color.White)
            .appBarIcon(Color.Black)
            .error(Color.LightGray)

    private fun configuredDarkColors() =
        ShippingAddressElement.Configuration.Appearance.Colors.dark()
            .primary(Color.Magenta)
            .surface(Color.Cyan)
            .component(Color.Yellow)
            .componentBorder(Color.Red)
            .componentDivider(Color.Green)
            .onComponent(Color.Blue)
            .subtitle(Color.DarkGray)
            .placeholderText(Color.Gray)
            .onSurface(Color.Black)
            .appBarIcon(Color.White)
            .error(Color.LightGray)

    private fun configuredPrimaryButton() =
        ShippingAddressElement.Configuration.Appearance.PrimaryButton()
            .colorsLight(
                ShippingAddressElement.Configuration.Appearance.PrimaryButton.Colors.light()
                    .background(Color.Green)
                    .onBackground(Color.White)
                    .border(Color.Black)
            )
            .colorsDark(
                ShippingAddressElement.Configuration.Appearance.PrimaryButton.Colors.dark()
                    .background(Color.Blue)
                    .onBackground(Color.Yellow)
                    .border(Color.Red)
            )
            .shape(
                ShippingAddressElement.Configuration.Appearance.PrimaryButton.Shape()
                    .cornerRadiusDp(12f)
                    .borderStrokeWidthDp(2f)
                    .heightDp(48f)
            )
            .typography(
                ShippingAddressElement.Configuration.Appearance.PrimaryButton.Typography()
                    .fontResId(123)
                    .fontSizeSp(18f)
            )
}
