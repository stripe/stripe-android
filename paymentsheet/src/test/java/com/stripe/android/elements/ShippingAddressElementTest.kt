@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.elements

import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
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
import com.stripe.android.paymentelement.embedded.content.SheetStateHolder
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
import kotlinx.coroutines.flow.MutableStateFlow
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
    fun `present launches ready with a blank address form and hosted autocomplete`() = runScenario {
        shippingAddressElement.present()

        val launch = activityLauncher.launchCalls.awaitItem()
        val input = launch.input as AddressElementActivityContract.Args.CheckoutShipping.Ready
        assertThat(input.publishableKey).isEqualTo(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY)

        val config = requireNotNull(input.config)
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
    fun `retained state transitions loading to ready when updating stops`() = runScenario(isUpdating = true) {
        shippingAddressElement.present()

        val loading = activityLauncher.launchCalls.awaitItem().input
        assertThat(loading).isInstanceOf(
            AddressElementActivityContract.Args.CheckoutShipping.Loading::class.java
        )
        assertThat(shippingAddressElementStateHolder.isAwaitingReady).isTrue()
        assertThat(sheetStateHolder.sheetIsOpen).isTrue()
        assertThat(paymentConfiguration.getCalls.awaitItem()).isEqualTo(Unit)

        paymentConfiguration.value = PaymentConfiguration(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        isUpdating.value = false
        runCurrent()

        val ready = activityLauncher.launchCalls.awaitItem().input
        assertThat(ready)
            .isInstanceOf(AddressElementActivityContract.Args.CheckoutShipping.Ready::class.java)
        assertThat(ready.publishableKey).isEqualTo(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        assertThat(shippingAddressElementStateHolder.isAwaitingReady).isFalse()
        assertThat(sheetStateHolder.sheetIsOpen).isTrue()
        assertThat(paymentConfiguration.getCalls.awaitItem()).isEqualTo(Unit)
    }

    @Test
    fun `canceling loading clears gate and suppresses ready`() = runScenario(isUpdating = true) {
        shippingAddressElement.present()
        activityLauncher.launchCalls.awaitItem()
        assertThat(paymentConfiguration.getCalls.awaitItem()).isEqualTo(Unit)

        registration.dispatch(AddressElementActivityContract.Result.Canceled)
        isUpdating.value = false
        runCurrent()

        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
        assertThat(shippingAddressElementStateHolder.isAwaitingReady).isFalse()
        activityLauncher.launchCalls.expectNoEvents()
        paymentConfiguration.getCalls.expectNoEvents()
    }

    @Test
    fun `recreated element resumes ready launch after update`() = runScenario(isUpdating = true) {
        shippingAddressElement.present()
        activityLauncher.launchCalls.awaitItem()
        assertThat(paymentConfiguration.getCalls.awaitItem()).isEqualTo(Unit)

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        activityLauncher.unregisterCalls.awaitItem()
        val recreated = createElement()
        isUpdating.value = false
        runCurrent()

        val ready = recreated.activityLauncher.launchCalls.awaitItem().input
        assertThat(ready).isInstanceOf(
            AddressElementActivityContract.Args.CheckoutShipping.Ready::class.java
        )
        assertThat(paymentConfiguration.getCalls.awaitItem()).isEqualTo(Unit)
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
        assertThat(shippingAddressElementStateHolder.isAwaitingReady).isFalse()
        assertThat(sheetStateHolder.sheetIsOpen).isFalse()

        shippingAddressElement.present()
        activityLauncher.launchCalls.awaitItem()
        assertThat(paymentConfiguration.getCalls.awaitItem()).isEqualTo(Unit)
        assertThat(paymentConfiguration.getCalls.awaitItem()).isEqualTo(Unit)
    }

    @Test
    fun `successful result clears presentation before commit completes`() {
        val commitResult = CompletableDeferred<Result<Unit>>()

        runScenario(
            configured = true,
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
            assertThat(shippingAddressElementStateHolder.isAwaitingReady).isFalse()
            assertThat(sheetStateHolder.sheetIsOpen).isFalse()
            commitShippingAddress.calls.awaitItem()

            shippingAddressElement.present()
            activityLauncher.launchCalls.awaitItem()
            assertThat(paymentConfiguration.getCalls.awaitItem()).isEqualTo(Unit)

            commitResult.complete(Result.success(Unit))
        }
    }

    @Test
    fun `canceled result clears presentation without committing`() = runScenario {
        shippingAddressElement.present()
        activityLauncher.launchCalls.awaitItem()
        assertThat(paymentConfiguration.getCalls.awaitItem()).isEqualTo(Unit)

        registration.dispatch(AddressElementActivityContract.Result.Canceled)

        assertThat(shippingAddressElementStateHolder.isAwaitingReady).isFalse()
        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
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

        assertThat(shippingAddressElementStateHolder.isAwaitingReady).isFalse()
        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
        commitShippingAddress.calls.expectNoEvents()
    }

    @Test
    fun `recreated element result clears presentation after host destruction`() = runScenario {
        shippingAddressElement.present()
        activityLauncher.launchCalls.awaitItem()
        assertThat(paymentConfiguration.getCalls.awaitItem()).isEqualTo(Unit)

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        activityLauncher.unregisterCalls.awaitItem()
        assertThat(shippingAddressElementStateHolder.isAwaitingReady).isFalse()
        assertThat(sheetStateHolder.sheetIsOpen).isTrue()

        val recreated = createElement()
        recreated.shippingAddressElement.present()
        recreated.activityLauncher.launchCalls.expectNoEvents()

        recreated.registration.dispatch(AddressElementActivityContract.Result.Canceled)
        assertThat(shippingAddressElementStateHolder.isAwaitingReady).isFalse()
        assertThat(sheetStateHolder.sheetIsOpen).isFalse()

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
        isUpdating: Boolean = false,
        block: suspend Scenario.() -> Unit,
    ) = runScenario(
        configured = configured,
        isUpdating = isUpdating,
        commitShippingAddress = FakeCommitShippingAddress(
            CompletableDeferred(Result.success(Unit)),
        ),
        block = block,
    )

    private fun runScenario(
        configured: Boolean,
        isUpdating: Boolean = false,
        commitShippingAddress: FakeCommitShippingAddress,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val savedStateHandle = SavedStateHandle()
        val stateHolder = CheckoutControllerStateFactory.createStateHolder(
            savedStateHandle = savedStateHandle,
        )
        if (configured) {
            stateHolder.state = CheckoutControllerStateFactory.create()
        }
        val shippingAddressElementStateHolder = ShippingAddressElementStateHolder(savedStateHandle)
        val sheetStateHolder = SheetStateHolder(savedStateHandle)
        val updating = MutableStateFlow(isUpdating)
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
                sheetStateHolder = sheetStateHolder,
                isUpdating = updating,
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
            sheetStateHolder = sheetStateHolder,
            isUpdating = updating,
            commitShippingAddress = commitShippingAddress,
            paymentConfiguration = paymentConfiguration,
            errorReporter = errorReporter,
            registration = element.registration,
            createElement = ::createElement,
            runCurrent = testScheduler::runCurrent,
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
        val sheetStateHolder: SheetStateHolder,
        val isUpdating: MutableStateFlow<Boolean>,
        val commitShippingAddress: FakeCommitShippingAddress,
        val paymentConfiguration: RecordingProvider<PaymentConfiguration>,
        val errorReporter: FakeErrorReporter,
        val registration: Registration,
        val createElement: suspend () -> ElementScenario,
        private val runCurrent: () -> Unit,
    ) {
        fun runCurrent() = runCurrent.invoke()
    }
}
