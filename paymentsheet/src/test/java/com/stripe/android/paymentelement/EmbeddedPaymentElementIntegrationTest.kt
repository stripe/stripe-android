package com.stripe.android.paymentelement

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Parcel
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.DisplayableCustomPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbacks
import com.stripe.android.paymentelement.confirmation.cpms.CustomPaymentMethodProxyActivity
import com.stripe.android.paymentelement.confirmation.cpms.InternalCustomPaymentMethodResult
import com.stripe.android.paymentelement.embedded.content.EmbeddedConfirmationStateHolder
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.createCustomerState
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.testing.CleanupTestRule
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.PaymentConfigurationTestRule
import com.stripe.android.utils.PaymentElementCallbackTestRule
import com.stripe.android.utils.simulateProcessDeath
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
@Suppress("RestrictedApi")
internal class EmbeddedPaymentElementIntegrationTest {
    private val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())
    private val integrationCleanupRule = CleanupTestRule<EmbeddedPaymentElementIntegration> { destroy() }
    private val activityCleanupRule = CleanupTestRule<ActivityController<IntegrationTestActivity>> {
        if (get().lifecycle.currentState != Lifecycle.State.DESTROYED) {
            pause().stop().destroy()
        }
        get().launches.ensureAllEventsConsumed()
    }

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(coroutineTestRule)
        .around(
            PaymentConfigurationTestRule(
                context = ApplicationProvider.getApplicationContext(),
                publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
                stripeAccountId = null,
            )
        )
        .around(PaymentElementCallbackTestRule())
        .around(activityCleanupRule)
        .around(integrationCleanupRule)

    @Test
    fun `repeated loaded integrations leave no saved namespaces or Activity ViewModels`() = runScenario {
        val originalViewModelKeys = activity.viewModelStore.keys()

        repeat(20) { index ->
            val integrationName = "integration_$index"
            val integration = createIntegration(activity, parentHandle, integrationName)
            val element = integration.createElement(activity, firstCallbacks.builder)
            element.state = loadedState(PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD))
            assertThat(element.paymentOption.value).isNotNull()
            assertThat(parentHandle.simulateProcessDeath().keys()).containsExactly(integrationName)

            integration.destroy()

            assertThat(parentHandle.simulateProcessDeath().keys()).isEmpty()
            assertThat(activity.viewModelStore.keys()).containsExactlyElementsIn(originalViewModelKeys)
        }
    }

    @Test
    fun `host destruction and reattachment preserve loaded state and replace callbacks`() = runScenario {
        val integration = createIntegration(activity, parentHandle, "retained")
        val original = integration.createElement(activity, firstCallbacks.builder)
        original.state = loadedState(PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD))
        val originalOption = requireNotNull(original.paymentOption.value)
        val originalState = requireNotNull(original.state)

        activityController.pause().stop().destroy()
        assertThat(PaymentElementCallbackReferences["retained"]?.createIntentCallback)
            .isSameInstanceAs(firstCallbacks.createIntentCallback)
        val replacementActivity = createActivity(savedState = null).get()
        val replacement = integration.createElement(replacementActivity, secondCallbacks.builder)

        assertThat(replacement.state?.confirmationState).isEqualTo(originalState.confirmationState)
        assertThat(replacement.paymentOption.value).isSameInstanceAs(originalOption)
        assertThat(PaymentElementCallbackReferences["retained"]?.createIntentCallback)
            .isSameInstanceAs(secondCallbacks.createIntentCallback)
        replacement.clearPaymentOption()
        assertThat(replacement.paymentOption.value).isNull()
        assertThat(replacement.state?.confirmationState?.selection).isNull()
    }

    @Test
    fun `Activity saved state restores a loaded integration after process death`() = runScenario {
        val integration = createIntegration(activity, parentHandle, "restored")
        val original = integration.createElement(activity, firstCallbacks.builder)
        original.state = loadedState(PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD))
        val originalOption = requireNotNull(original.paymentOption.value)
        val originalSelection = requireNotNull(original.state).confirmationState.selection
        val savedActivityState = saveActivityState(activityController)

        integration.destroy()
        activityController.pause().stop().destroy()
        val replacementActivity = createActivity(savedActivityState).get()
        val restoredParent = parentHandle(replacementActivity)
        val restored = createIntegration(replacementActivity, restoredParent, "restored")
            .createElement(replacementActivity, secondCallbacks.builder)

        assertThat(restored.state?.confirmationState?.selection).isEqualTo(originalSelection)
        assertThat(restored.paymentOption.value?.label).isEqualTo(originalOption.label)
        assertThat(restored.paymentOption.value?.paymentMethodType).isEqualTo(originalOption.paymentMethodType)
        assertThat(PaymentElementCallbackReferences["restored"]?.createIntentCallback)
            .isSameInstanceAs(secondCallbacks.createIntentCallback)
    }

    @Test
    fun `destroying one named integration preserves another integration and merchant state`() = runScenario {
        parentHandle["merchant_state"] = "preserved"
        val firstIntegration = createIntegration(activity, parentHandle, "first")
        val first = firstIntegration.createElement(activity, firstCallbacks.builder)
        val secondIntegration = createIntegration(activity, parentHandle, "second")
        val second = secondIntegration.createElement(activity, secondCallbacks.builder)
        first.state = loadedState(PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD))
        second.state = loadedState(CUSTOM_PAYMENT_SELECTION)
        val secondOption = requireNotNull(second.paymentOption.value)
        val secondCallbacksReference = PaymentElementCallbackReferences["second"]
        assertThat(PaymentElementCallbackReferences["first"]?.createIntentCallback)
            .isSameInstanceAs(firstCallbacks.createIntentCallback)
        assertThat(secondCallbacksReference?.createIntentCallback)
            .isSameInstanceAs(secondCallbacks.createIntentCallback)

        firstIntegration.destroy()

        assertThat(second.state?.confirmationState?.selection).isEqualTo(CUSTOM_PAYMENT_SELECTION)
        assertThat(second.paymentOption.value).isSameInstanceAs(secondOption)
        assertThat(PaymentElementCallbackReferences["second"]).isSameInstanceAs(secondCallbacksReference)
        val restoredParent = parentHandle.simulateProcessDeath()
        assertThat(restoredParent.keys()).containsExactly("second", "merchant_state")
        assertThat(restoredParent.get<String>("merchant_state")).isEqualTo("preserved")
    }

    @Test
    fun `repeated destruction of an old integration preserves its same-name replacement`() = runScenario {
        val old = createIntegration(activity, parentHandle, "replacement")
        old.createElement(activity, firstCallbacks.builder).state = loadedState(CUSTOM_PAYMENT_SELECTION)
        old.destroy()
        val replacementIntegration = createIntegration(activity, parentHandle, "replacement")
        val replacement = replacementIntegration.createElement(activity, secondCallbacks.builder)
        replacement.state = loadedState(PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD))
        val replacementOption = requireNotNull(replacement.paymentOption.value)
        val replacementCallbacks = PaymentElementCallbackReferences["replacement"]

        old.destroy()

        assertThat(replacement.paymentOption.value).isSameInstanceAs(replacementOption)
        assertThat(PaymentElementCallbackReferences["replacement"]).isSameInstanceAs(replacementCallbacks)
        assertThat(parentHandle.simulateProcessDeath().keys()).containsExactly("replacement")
    }

    @Test
    fun `permanent destruction unregisters the actual Activity result routes`() = runScenario {
        val originalRoutes = registeredRoutes(activity)
        val integration = createIntegration(activity, parentHandle, "routes")
        integration.createElement(activity, firstCallbacks.builder)
        assertThat(registeredRoutes(activity).filter { it.startsWith("EmbeddedPaymentElement(instance = routes)") })
            .isNotEmpty()

        integration.destroy()

        assertThat(registeredRoutes(activity)).containsExactlyElementsIn(originalRoutes)
        assertThat(PaymentElementCallbackReferences["routes"]).isSameInstanceAs(unrelatedCallbacks)
    }

    @Test
    fun `in-flight confirmation result after Activity recreation reaches the current callback`() = runScenario {
        val integration = createIntegration(activity, parentHandle, "confirmation")
        val original = integration.createElement(activity, firstCallbacks.builder)
        original.state = loadedState(CUSTOM_PAYMENT_SELECTION)
        original.confirm()
        val launch = activity.launches.awaitItem()
        assertThat(launch.intent.component?.className).isEqualTo(CustomPaymentMethodProxyActivity::class.java.name)
        val savedActivityState = saveActivityState(activityController)

        activityController.pause().stop().destroy()
        val replacementActivity = createActivity(savedActivityState).get()
        integration.createElement(replacementActivity, secondCallbacks.builder)
        val delivered = replacementActivity.activityResultRegistry.dispatchResult(
            launch.requestCode,
            Activity.RESULT_OK,
            Intent().putExtras(InternalCustomPaymentMethodResult.Completed.toBundle()),
        )

        assertThat(delivered).isTrue()
        assertThat(secondCallbacks.results.awaitItem())
            .isInstanceOf(EmbeddedPaymentElement.Result.Completed::class.java)
        firstCallbacks.results.expectNoEvents()
    }

    @Test
    fun `pending confirmation result before reattachment reaches the current callback`() = runScenario {
        val integration = createIntegration(activity, parentHandle, "pending_confirmation")
        val original = integration.createElement(activity, firstCallbacks.builder)
        original.state = loadedState(CUSTOM_PAYMENT_SELECTION)
        original.confirm()
        val launch = activity.launches.awaitItem()
        assertThat(launch.intent.component?.className).isEqualTo(CustomPaymentMethodProxyActivity::class.java.name)
        val savedActivityState = saveActivityState(activityController)

        activityController.pause().stop().destroy()
        val replacementActivity = createActivity(savedActivityState).get()
        val delivered = replacementActivity.activityResultRegistry.dispatchResult(
            launch.requestCode,
            Activity.RESULT_OK,
            Intent().putExtras(InternalCustomPaymentMethodResult.Completed.toBundle()),
        )

        assertThat(delivered).isTrue()
        firstCallbacks.results.expectNoEvents()
        secondCallbacks.results.expectNoEvents()

        integration.createElement(replacementActivity, secondCallbacks.builder)

        assertThat(secondCallbacks.results.awaitItem())
            .isInstanceOf(EmbeddedPaymentElement.Result.Completed::class.java)
        firstCallbacks.results.expectNoEvents()
    }

    @Test
    fun `pending completion callback can destroy integration and clear state and unlaunched routes`() = runScenario {
        val originalRoutes = registeredRoutes(activity)
        val integrationName = "completion_removal"
        val integration = createIntegration(activity, parentHandle, integrationName)
        val original = integration.createElement(activity, firstCallbacks.builder)
        original.state = loadedState(CUSTOM_PAYMENT_SELECTION)
        original.confirm()
        val launch = activity.launches.awaitItem()
        val launchedRoute = registeredRoute(activity, launch.requestCode)
        val savedActivityState = saveActivityState(activityController)

        activityController.pause().stop().destroy()
        val replacementActivity = createActivity(savedActivityState).get()
        val delivered = replacementActivity.activityResultRegistry.dispatchResult(
            launch.requestCode,
            Activity.RESULT_OK,
            Intent().putExtras(InternalCustomPaymentMethodResult.Completed.toBundle()),
        )
        assertThat(delivered).isTrue()
        firstCallbacks.results.expectNoEvents()

        val completionRemovalBuilder = EmbeddedPaymentElement.Builder(
            createIntentCallback = secondCallbacks.createIntentCallback,
            resultCallback = { result ->
                secondCallbacks.results.add(result)
                integration.destroy()
            },
        ).confirmCustomPaymentMethodCallback { _, _ -> error("Unexpected custom payment method callback") }

        integration.createElement(replacementActivity, completionRemovalBuilder)

        assertThat(secondCallbacks.results.awaitItem())
            .isInstanceOf(EmbeddedPaymentElement.Result.Completed::class.java)
        assertThat(parentHandle.simulateProcessDeath().keys()).isEmpty()
        assertThat(registeredRoutes(replacementActivity))
            .containsExactlyElementsIn(originalRoutes + launchedRoute)
        assertThat(PaymentElementCallbackReferences[integrationName]).isSameInstanceAs(unrelatedCallbacks)
        firstCallbacks.results.expectNoEvents()
    }

    private fun runScenario(block: suspend Scenario.() -> Unit) = runTest {
        val scenario = Scenario(createActivity(savedState = null))
        PaymentElementCallbackReferences["unrelated-owner"] = scenario.unrelatedCallbacks
        try {
            scenario.block()
        } finally {
            scenario.firstCallbacks.results.ensureAllEventsConsumed()
            scenario.secondCallbacks.results.ensureAllEventsConsumed()
        }
    }

    private fun createActivity(savedState: Bundle?): ActivityController<IntegrationTestActivity> {
        return activityCleanupRule.track(
            Robolectric.buildActivity(IntegrationTestActivity::class.java).create(savedState).start().resume()
        )
    }

    private fun createIntegration(
        activity: ComponentActivity,
        parentHandle: SavedStateHandle,
        integrationName: String,
    ): EmbeddedPaymentElementIntegration {
        return integrationCleanupRule.track(
            EmbeddedPaymentElementIntegration.create(activity, parentHandle, integrationName)
        )
    }

    private fun parentHandle(activity: ComponentActivity): SavedStateHandle {
        return ViewModelProvider.create(activity, ParentStateViewModel.Factory)[ParentStateViewModel::class].handle
    }

    private fun saveActivityState(controller: ActivityController<IntegrationTestActivity>): Bundle {
        val savedState = Bundle()
        controller.saveInstanceState(savedState)
        val parcel = Parcel.obtain()
        return try {
            parcel.writeBundle(savedState)
            parcel.setDataPosition(0)
            requireNotNull(parcel.readBundle(javaClass.classLoader))
        } finally {
            parcel.recycle()
        }
    }

    private fun registeredRoutes(activity: ComponentActivity): List<String> {
        val registryState = Bundle()
        activity.activityResultRegistry.onSaveInstanceState(registryState)
        return registryState.getStringArrayList("KEY_COMPONENT_ACTIVITY_REGISTERED_KEYS").orEmpty()
    }

    private fun registeredRoute(activity: ComponentActivity, requestCode: Int): String {
        val registryState = Bundle()
        activity.activityResultRegistry.onSaveInstanceState(registryState)
        val keys = requireNotNull(registryState.getStringArrayList("KEY_COMPONENT_ACTIVITY_REGISTERED_KEYS"))
        val requestCodes = requireNotNull(registryState.getIntegerArrayList("KEY_COMPONENT_ACTIVITY_REGISTERED_RCS"))
        return keys[requestCodes.indexOf(requestCode)]
    }

    private fun loadedState(selection: PaymentSelection): EmbeddedPaymentElement.State {
        val savedSelection = selection as? PaymentSelection.Saved
        return EmbeddedPaymentElement.State(
            confirmationState = EmbeddedConfirmationStateHolder.State(
                paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                    hasCustomerConfiguration = savedSelection != null,
                    displayableCustomPaymentMethods = listOf(
                        DisplayableCustomPaymentMethod(
                            id = CUSTOM_PAYMENT_METHOD_ID,
                            displayName = "Custom payment",
                            logoUrl = "https://example.com/custom.png",
                            subtitle = null,
                            doesNotCollectBillingDetails = true,
                        )
                    ),
                ),
                selection = selection,
                configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.")
                    .customPaymentMethods(
                        listOf(
                            PaymentSheet.CustomPaymentMethod(
                                id = CUSTOM_PAYMENT_METHOD_ID,
                                subtitle = "Pay directly".resolvableString,
                                disableBillingDetailCollection = true,
                            )
                        )
                    )
                    .customer(
                        if (savedSelection != null) PaymentSheet.CustomerConfiguration("cus_123", "ek_123") else null
                    )
                    .build(),
                statusBarColor = null,
            ),
            customer = savedSelection?.let { createCustomerState(paymentMethods = listOf(it.paymentMethod)) },
            previousNewSelections = Bundle(),
        )
    }

    private inner class Scenario(
        val activityController: ActivityController<IntegrationTestActivity>,
    ) {
        val activity: IntegrationTestActivity = activityController.get()
        val parentHandle = parentHandle(activity)
        val firstCallbacks = CallbackRecorder()
        val secondCallbacks = CallbackRecorder()
        val unrelatedCallbacks = PaymentElementCallbacks.Builder().build()
    }

    private class CallbackRecorder {
        val results = Turbine<EmbeddedPaymentElement.Result>()
        val createIntentCallback = CreateIntentCallback { _, _ -> error("Unexpected create-intent callback") }
        val builder = EmbeddedPaymentElement.Builder(
            createIntentCallback = createIntentCallback,
            resultCallback = { results.add(it) },
        ).confirmCustomPaymentMethodCallback { _, _ ->
            error("The proxy Activity is represented by its Activity result in this test")
        }
    }

    private class ParentStateViewModel(val handle: SavedStateHandle) : ViewModel() {
        companion object {
            val Factory = viewModelFactory {
                initializer { ParentStateViewModel(createSavedStateHandle()) }
            }
        }
    }

    class IntegrationTestActivity : ComponentActivity() {
        val launches = Turbine<Launch>()

        @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
        override fun startActivityForResult(intent: Intent, requestCode: Int, options: Bundle?) {
            super.startActivityForResult(intent, requestCode, options)
            launches.add(Launch(intent, requestCode))
        }

        data class Launch(val intent: Intent, val requestCode: Int)
    }

    private companion object {
        const val CUSTOM_PAYMENT_METHOD_ID = "cpmt_123"
        val CUSTOM_PAYMENT_SELECTION = PaymentSelection.CustomPaymentMethod(
            id = CUSTOM_PAYMENT_METHOD_ID,
            billingDetails = null,
            label = "Custom payment".resolvableString,
            lightThemeIconUrl = null,
            darkThemeIconUrl = null,
        )
    }
}
