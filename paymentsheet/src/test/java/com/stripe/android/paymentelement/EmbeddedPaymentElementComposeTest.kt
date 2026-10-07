package com.stripe.android.paymentelement

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Parcel
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.DisplayableCustomPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbacks
import com.stripe.android.paymentelement.confirmation.cpms.CustomPaymentMethodContract
import com.stripe.android.paymentelement.confirmation.cpms.CustomPaymentMethodInput
import com.stripe.android.paymentelement.confirmation.cpms.InternalCustomPaymentMethodResult
import com.stripe.android.paymentelement.embedded.content.EmbeddedConfirmationStateHolder
import com.stripe.android.paymentelement.embedded.content.EmbeddedPaymentElementViewModel
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.createComposeCleanupRule
import com.stripe.android.utils.PaymentElementCallbackTestRule
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
@Suppress("RestrictedApi")
internal class EmbeddedPaymentElementComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    @get:Rule
    val callbackRule = PaymentElementCallbackTestRule()

    private val stateRestorer = StateRestorationTester(composeRule)

    @Test
    fun `repeated default named mounts reuse the element and one saved ViewModel slot`() = runScenario {
        val original = firstElement
        composeRule.runOnIdle { original.state = loadedState() }
        val originalOption = requireNotNull(original.paymentOption.value)

        repeat(20) {
            remount()

            assertThat(firstElement).isSameInstanceAs(original)
            assertThat(firstElement.paymentOption.value).isSameInstanceAs(originalOption)
            assertThat(embeddedViewModelKeys()).containsExactly(viewModelKey(DEFAULT_NAME))
        }

        assertThat(savedViewModelKeys(saveActivityState(controller))).containsExactly(viewModelKey(DEFAULT_NAME))
    }

    @Test
    fun `unnamed native elements sharing an owner retain distinct identities and callbacks`() = runScenario(
        firstName = null,
        showSecond = true,
    ) {
        assertThat(firstElement).isNotSameInstanceAs(secondElement)
        assertThat(embeddedViewModelKeys()).hasSize(2)
        composeRule.runOnIdle {
            firstElement.state = loadedState()
            secondElement.state = loadedState("cmpt_second")
        }

        val firstLaunch = confirm(firstElement)
        val secondLaunch = confirm(secondElement)

        assertThat(firstLaunch.input.paymentElementCallbackIdentifier)
            .isNotEqualTo(secondLaunch.input.paymentElementCallbackIdentifier)
        assertThat(PaymentElementCallbackReferences[firstLaunch.input.paymentElementCallbackIdentifier]
            ?.createIntentCallback).isSameInstanceAs(firstCallbacks.createIntentCallback)
        assertThat(PaymentElementCallbackReferences[secondLaunch.input.paymentElementCallbackIdentifier]
            ?.createIntentCallback).isSameInstanceAs(secondCallbacks.createIntentCallback)
        complete(firstLaunch)
        complete(secondLaunch)
        assertCompleted(firstCallbacks)
        assertCompleted(secondCallbacks)
    }

    @Test
    fun `different explicit names isolate elements state and callbacks under one owner`() = runScenario(
        firstName = "first",
        secondName = "second",
        showSecond = true,
    ) {
        assertThat(firstElement).isNotSameInstanceAs(secondElement)
        assertThat(embeddedViewModelKeys()).containsExactly(viewModelKey("first"), viewModelKey("second"))
        composeRule.runOnIdle {
            firstElement.state = loadedState()
            secondElement.state = loadedState("cmpt_second")
            firstElement.clearPaymentOption()
        }

        assertThat(firstElement.paymentOption.value).isNull()
        assertThat(secondElement.state?.confirmationState?.selection)
            .isEqualTo(customSelection("cmpt_second"))
        assertThat(PaymentElementCallbackReferences["first"]?.createIntentCallback)
            .isSameInstanceAs(firstCallbacks.createIntentCallback)
        assertThat(PaymentElementCallbackReferences["second"]?.createIntentCallback)
            .isSameInstanceAs(secondCallbacks.createIntentCallback)
        val launch = confirm(secondElement)
        assertThat(launch.input.paymentElementCallbackIdentifier).isEqualTo("second")
        complete(launch)
        assertCompleted(secondCallbacks)
        firstCallbacks.results.expectNoEvents()
    }

    @Test
    fun `in-flight confirmation after remount reaches the latest callback exactly once`() = runScenario {
        composeRule.runOnIdle { firstElement.state = loadedState() }
        val launch = confirm(firstElement)

        remount(replacementCallbacks)
        complete(launch)

        assertCompleted(replacementCallbacks)
        replacementCallbacks.results.expectNoEvents()
        firstCallbacks.results.expectNoEvents()
    }

    @Test
    fun `ordinary recomposition updates the result callback without replacing the element`() = runScenario {
        val original = firstElement
        composeRule.runOnIdle {
            firstElement.state = loadedState()
            firstBuilder = replacementCallbacks.builder(firstName)
        }
        composeRule.waitForIdle()

        assertThat(firstElement).isSameInstanceAs(original)
        complete(confirm(firstElement))

        assertCompleted(replacementCallbacks)
        firstCallbacks.results.expectNoEvents()
    }

    @Test
    fun `saved composition restores unnamed identity loaded state and current callbacks`() = runScenario(
        firstName = null,
    ) {
        composeRule.runOnIdle { firstElement.state = loadedState() }
        val original = firstElement
        val originalKey = embeddedViewModelKeys().single()
        composeRule.runOnIdle {
            PaymentElementCallbackReferences.clear()
            seedUnrelatedCallbacks()
        }

        stateRestorer.emulateSavedInstanceStateRestore()
        composeRule.waitForIdle()

        assertThat(firstElement).isSameInstanceAs(original)
        assertThat(embeddedViewModelKeys()).containsExactly(originalKey)
        val launch = confirm(firstElement)
        assertThat(viewModelKey(launch.input.paymentElementCallbackIdentifier)).isEqualTo(originalKey)
        assertThat(PaymentElementCallbackReferences[launch.input.paymentElementCallbackIdentifier]
            ?.createIntentCallback).isSameInstanceAs(firstCallbacks.createIntentCallback)
        complete(launch)
        assertCompleted(firstCallbacks)
    }

    @Test
    fun `Activity saved state restores a named loaded element in a new ViewModel store`() = runScenario {
        composeRule.runOnIdle { firstElement.state = loadedState() }
        val original = firstElement
        val originalOption = requireNotNull(original.paymentOption.value)
        val savedState = saveActivityState(controller)
        composeRule.runOnIdle { mounted = false }
        composeRule.waitForIdle()
        controller.pause().stop().destroy()
        val replacementController = createActivity(savedState)

        composeRule.runOnIdle {
            controller = replacementController
            mounted = true
        }
        composeRule.waitForIdle()

        assertThat(firstElement).isNotSameInstanceAs(original)
        assertThat(embeddedViewModelKeys()).containsExactly(viewModelKey(DEFAULT_NAME))
        assertThat(firstElement.state?.confirmationState?.selection).isEqualTo(customSelection(CUSTOM_METHOD_ID))
        assertThat(firstElement.paymentOption.value?.label).isEqualTo(originalOption.label)
        complete(confirm(firstElement))
        assertCompleted(firstCallbacks)
    }

    @Test
    fun `retained named runtime delivers pending confirmation to the recreated Activity callback`() = runScenario {
        val original = firstElement
        val originalStore = controller.get().viewModelStore
        composeRule.runOnIdle { original.state = loadedState() }
        val launch = confirm(original)
        val originalRegistry = registry
        val registryState = Bundle()
        composeRule.runOnIdle {
            registry.onSaveInstanceState(registryState)
            mounted = false
        }
        composeRule.waitForIdle()
        composeRule.runOnIdle { controller.recreate() }
        assertThat(controller.get().viewModelStore).isSameInstanceAs(originalStore)
        assertThat(PaymentElementCallbackReferences[DEFAULT_NAME]?.createIntentCallback)
            .isSameInstanceAs(firstCallbacks.createIntentCallback)
        assertThat(original.state?.confirmationState?.selection).isEqualTo(customSelection(CUSTOM_METHOD_ID))

        val replacementRegistry = FakeEmbeddedActivityResultRegistry().apply {
            onRestoreInstanceState(registryState)
        }
        composeRule.runOnIdle { registry = replacementRegistry }
        complete(launch)
        firstCallbacks.results.expectNoEvents()
        replacementCallbacks.results.expectNoEvents()
        composeRule.runOnIdle {
            firstBuilder = replacementCallbacks.builder(firstName)
            mounted = true
        }
        composeRule.waitForIdle()

        assertThat(firstElement).isNotSameInstanceAs(original)
        assertThat(embeddedViewModelKeys()).containsExactly(viewModelKey(DEFAULT_NAME))
        assertCompleted(replacementCallbacks)
        assertThat(firstElement.state).isNull()
        assertThat(firstElement.paymentOption.value).isNull()
        replacementCallbacks.results.expectNoEvents()
        firstCallbacks.results.expectNoEvents()
        originalRegistry.launchCalls.ensureAllEventsConsumed()

        composeRule.runOnIdle { mounted = false }
        composeRule.waitForIdle()
        composeRule.runOnIdle { controller.pause().stop().destroy() }
        assertThat(originalStore.keys()).isEmpty()
        assertThat(PaymentElementCallbackReferences[DEFAULT_NAME]).isSameInstanceAs(unrelatedCallbacks)
    }

    private fun runScenario(
        firstName: String? = DEFAULT_NAME,
        secondName: String? = null,
        showSecond: Boolean = false,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        PaymentConfiguration.init(ApplicationProvider.getApplicationContext(), ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        val unrelatedCallbacks = seedUnrelatedCallbacks()
        val scenario = Scenario(firstName, secondName, createActivity(savedState = null), unrelatedCallbacks)
        stateRestorer.setContent {
            val activity = scenario.controller.get()
            CompositionLocalProvider(
                LocalContext provides activity,
                LocalLifecycleOwner provides activity,
                LocalViewModelStoreOwner provides activity,
                LocalActivityResultRegistryOwner provides scenario.registryOwner,
            ) {
                if (scenario.mounted) {
                    key("first") {
                        if (scenario.firstVisible) {
                            val element = rememberEmbeddedPaymentElement(scenario.firstBuilder)
                            SideEffect { scenario.firstElement = element }
                        }
                    }
                    if (showSecond) {
                        key("second") {
                            val element = rememberEmbeddedPaymentElement(scenario.secondBuilder)
                            SideEffect { scenario.secondElement = element }
                        }
                    }
                }
            }
        }
        composeRule.waitForIdle()

        try {
            scenario.block()
        } finally {
            composeRule.runOnIdle { scenario.mounted = false }
            composeRule.waitForIdle()
            if (scenario.controller.get().lifecycle.currentState != Lifecycle.State.DESTROYED) {
                scenario.controller.pause().stop().destroy()
            }
            scenario.registry.launchCalls.ensureAllEventsConsumed()
            scenario.firstCallbacks.results.ensureAllEventsConsumed()
            scenario.secondCallbacks.results.ensureAllEventsConsumed()
            scenario.replacementCallbacks.results.ensureAllEventsConsumed()
        }
    }

    private fun createActivity(savedState: Bundle?): ActivityController<ComponentActivity> {
        return Robolectric.buildActivity(ComponentActivity::class.java).create(savedState).start().resume()
    }

    private fun saveActivityState(controller: ActivityController<ComponentActivity>): Bundle {
        val savedState = Bundle()
        composeRule.runOnIdle { controller.saveInstanceState(savedState) }
        val parcel = Parcel.obtain()
        return try {
            parcel.writeBundle(savedState)
            parcel.setDataPosition(0)
            requireNotNull(parcel.readBundle(javaClass.classLoader))
        } finally {
            parcel.recycle()
        }
    }

    @Suppress("DEPRECATION")
    private fun savedViewModelKeys(state: Bundle): Set<String> {
        return state.keySet().flatMap { key ->
            val nested = state.get(key) as? Bundle
            listOfNotNull(key.takeIf { it.startsWith(VIEW_MODEL_PREFIX) }) +
                nested?.let(::savedViewModelKeys).orEmpty()
        }.toSet()
    }

    private fun seedUnrelatedCallbacks(): PaymentElementCallbacks {
        return PaymentElementCallbacks.Builder().build().also {
            PaymentElementCallbackReferences["unrelated-owner"] = it
        }
    }

    private suspend fun assertCompleted(callbacks: CallbackRecorder) {
        assertThat(callbacks.results.awaitItem()).isInstanceOf(EmbeddedPaymentElement.Result.Completed::class.java)
    }

    private inner class Scenario(
        val firstName: String?,
        secondName: String?,
        controller: ActivityController<ComponentActivity>,
        val unrelatedCallbacks: PaymentElementCallbacks,
    ) {
        var registry by mutableStateOf(FakeEmbeddedActivityResultRegistry())
        val registryOwner = object : ActivityResultRegistryOwner {
            override val activityResultRegistry: ActivityResultRegistry
                get() = registry
        }
        val firstCallbacks = CallbackRecorder()
        val secondCallbacks = CallbackRecorder()
        val replacementCallbacks = CallbackRecorder()
        var controller by mutableStateOf(controller)
        var mounted by mutableStateOf(true)
        var firstVisible by mutableStateOf(true)
        var firstBuilder by mutableStateOf(firstCallbacks.builder(firstName))
        val secondBuilder = secondCallbacks.builder(secondName)
        lateinit var firstElement: EmbeddedPaymentElement
        lateinit var secondElement: EmbeddedPaymentElement

        fun embeddedViewModelKeys(): Set<String> {
            val store: ViewModelStore = controller.get().viewModelStore
            return store.keys().filter { store[it] is EmbeddedPaymentElementViewModel }.toSet()
        }

        fun remount(callbacks: CallbackRecorder = firstCallbacks) {
            composeRule.runOnIdle { firstVisible = false }
            composeRule.waitForIdle()
            composeRule.runOnIdle {
                firstBuilder = callbacks.builder(firstName)
                firstVisible = true
            }
            composeRule.waitForIdle()
        }

        suspend fun confirm(element: EmbeddedPaymentElement): FakeEmbeddedActivityResultRegistry.Launch {
            composeRule.runOnIdle { element.confirm() }
            return registry.launchCalls.awaitItem()
        }

        fun complete(launch: FakeEmbeddedActivityResultRegistry.Launch) {
            composeRule.runOnIdle {
                assertThat(
                    registry.dispatchResult(
                        launch.requestCode,
                        Activity.RESULT_OK,
                        Intent().putExtras(InternalCustomPaymentMethodResult.Completed.toBundle()),
                    )
                ).isTrue()
            }
        }
    }

    private class CallbackRecorder {
        val results = Turbine<EmbeddedPaymentElement.Result>()
        val createIntentCallback = CreateIntentCallback { _, _ -> error("Unexpected create-intent callback") }

        fun builder(name: String?): EmbeddedPaymentElement.Builder {
            return EmbeddedPaymentElement.Builder(createIntentCallback) { results.add(it) }
                .confirmCustomPaymentMethodCallback { _, _ -> error("Unexpected proxy Activity callback") }
                .apply {
                    when (name) {
                        DEFAULT_NAME -> integrationName()
                        null -> Unit
                        else -> integrationName(name)
                    }
                }
        }
    }

    internal class FakeEmbeddedActivityResultRegistry : ActivityResultRegistry() {
        val launchCalls = Turbine<Launch>()

        override fun <I, O> onLaunch(
            requestCode: Int,
            contract: ActivityResultContract<I, O>,
            input: I,
            options: ActivityOptionsCompat?,
        ) {
            assertThat(contract).isInstanceOf(CustomPaymentMethodContract::class.java)
            launchCalls.add(Launch(requestCode, input as CustomPaymentMethodInput))
        }

        data class Launch(val requestCode: Int, val input: CustomPaymentMethodInput)
    }

    private companion object {
        const val DEFAULT_NAME = "stripe_embedded"
        const val CUSTOM_METHOD_ID = "cmpt_123"
        const val VIEW_MODEL_PREFIX = "EmbeddedPaymentElementViewModel(instance = "

        fun viewModelKey(name: String): String = "$VIEW_MODEL_PREFIX$name)"

        fun customSelection(id: String): PaymentSelection.CustomPaymentMethod {
            return PaymentSelection.CustomPaymentMethod(
                id = id,
                billingDetails = null,
                label = "Custom payment".resolvableString,
                lightThemeIconUrl = null,
                darkThemeIconUrl = null,
            )
        }

        fun loadedState(id: String = CUSTOM_METHOD_ID): EmbeddedPaymentElement.State {
            return EmbeddedPaymentElement.State(
                confirmationState = EmbeddedConfirmationStateHolder.State(
                    paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                        displayableCustomPaymentMethods = listOf(
                            DisplayableCustomPaymentMethod(
                                id = id,
                                displayName = "Custom payment",
                                logoUrl = "https://example.com/custom.png",
                                subtitle = null,
                                doesNotCollectBillingDetails = true,
                            )
                        ),
                    ),
                    selection = customSelection(id),
                    configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.")
                        .customPaymentMethods(
                            listOf(
                                PaymentSheet.CustomPaymentMethod(
                                    id = id,
                                    subtitle = "Pay directly".resolvableString,
                                    disableBillingDetailCollection = true,
                                )
                            )
                        ).build(),
                    statusBarColor = null,
                ),
                customer = null,
                previousNewSelections = Bundle(),
            )
        }
    }
}
