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
import com.stripe.android.checkout.CheckoutControllerStateFactory
import com.stripe.android.checkout.CheckoutControllerStateHolder
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AUTOCOMPLETE_DEFAULT_COUNTRIES
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.addresselement.AddressElementActivityContract
import com.stripe.android.paymentsheet.addresselement.AddressLauncher
import com.stripe.android.paymentsheet.addresselement.AddressLauncherResult
import com.stripe.android.testing.CoroutineTestRule
import dagger.Lazy
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

internal class ShippingAddressElementTest {
    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `present before checkout configuration does not launch`() = runScenario(configured = false) {
        shippingAddressElement.present()

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
    fun `present suppresses duplicate presentations`() = runScenario {
        shippingAddressElement.present()
        shippingAddressElement.present()

        activityLauncher.launchCalls.awaitItem()
        activityLauncher.launchCalls.expectNoEvents()
        assertThat(paymentConfiguration.getCalls.awaitItem()).isEqualTo(Unit)
    }

    @Test
    fun `result clears presentation and ignores saved address`() = runScenario {
        val originalState = stateHolder.state
        shippingAddressElement.present()
        activityLauncher.launchCalls.awaitItem()

        registration.dispatch(
            AddressLauncherResult.Succeeded(
                AddressDetails(name = "Ignored address"),
            )
        )

        shippingAddressElement.present()
        activityLauncher.launchCalls.awaitItem()
        assertThat(stateHolder.state).isSameInstanceAs(originalState)
        assertThat(paymentConfiguration.getCalls.awaitItem()).isEqualTo(Unit)
        assertThat(paymentConfiguration.getCalls.awaitItem()).isEqualTo(Unit)
    }

    @Test
    fun `lifecycle destruction unregisters the launcher`() = runScenario {
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        assertThat(activityLauncher.unregisterCalls.awaitItem()).isEqualTo(Unit)
    }

    private fun runScenario(
        configured: Boolean = true,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val activityResultCaller = RecordingActivityResultCaller()
        val activityLauncher = activityResultCaller.launcher
        val lifecycleOwner = TestLifecycleOwner()
        val stateHolder = CheckoutControllerStateFactory.createStateHolder(
            savedStateHandle = SavedStateHandle(),
        )
        if (configured) {
            stateHolder.state = CheckoutControllerStateFactory.create()
        }
        val paymentConfiguration = RecordingLazy(
            PaymentConfiguration(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY),
        )
        val shippingAddressElement = ShippingAddressElement(
            activityResultCaller = activityResultCaller,
            lifecycleOwner = lifecycleOwner,
            paymentConfiguration = paymentConfiguration,
            stateHolder = stateHolder,
        )
        val registration = activityResultCaller.registerCalls.awaitItem()
        assertThat(registration.contract).isSameInstanceAs(AddressElementActivityContract)

        Scenario(
            shippingAddressElement = shippingAddressElement,
            activityLauncher = activityLauncher,
            lifecycleOwner = lifecycleOwner,
            stateHolder = stateHolder,
            paymentConfiguration = paymentConfiguration,
            registration = registration,
        ).block()

        activityResultCaller.registerCalls.ensureAllEventsConsumed()
        activityLauncher.launchCalls.ensureAllEventsConsumed()
        activityLauncher.unregisterCalls.ensureAllEventsConsumed()
        paymentConfiguration.getCalls.ensureAllEventsConsumed()
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
        ActivityResultLauncher<AddressElementActivityContract.Args>() {
        val launchCalls = Turbine<LaunchCall>()
        val unregisterCalls = Turbine<Unit>()

        override fun launch(
            input: AddressElementActivityContract.Args,
            options: ActivityOptionsCompat?,
        ) {
            launchCalls.add(LaunchCall(input))
        }

        override fun unregister() {
            unregisterCalls.add(Unit)
        }

        override val contract: ActivityResultContract<AddressElementActivityContract.Args, *>
            get() = AddressElementActivityContract
    }

    private class RecordingLazy<T>(
        private val value: T,
    ) : Lazy<T> {
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
        fun dispatch(result: AddressLauncherResult) {
            (callback as ActivityResultCallback<AddressLauncherResult>).onActivityResult(result)
        }
    }

    private data class LaunchCall(
        val input: AddressElementActivityContract.Args,
    )

    private data class Scenario(
        val shippingAddressElement: ShippingAddressElement,
        val activityLauncher: RecordingActivityResultLauncher,
        val lifecycleOwner: TestLifecycleOwner,
        val stateHolder: CheckoutControllerStateHolder,
        val paymentConfiguration: RecordingLazy<PaymentConfiguration>,
        val registration: Registration,
    )
}
