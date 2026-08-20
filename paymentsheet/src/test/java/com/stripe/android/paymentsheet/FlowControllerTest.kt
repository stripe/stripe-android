package com.stripe.android.paymentsheet

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbacks
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
import com.stripe.android.paymentsheet.flowcontroller.DefaultFlowController
import com.stripe.android.paymentsheet.flowcontroller.FlowControllerViewModel
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.utils.PaymentElementCallbackTestRule
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestParameterInjector
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController

@RunWith(RobolectricTestParameterInjector::class)
internal class FlowControllerTest {
    @get:Rule
    val callbackRule = PaymentElementCallbackTestRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(StandardTestDispatcher())

    private val unrelatedCallbacks = PaymentElementCallbacks.Builder().build()

    @Test
    fun `launch uses the owner's stored identifier`(
        @TestParameter construction: Construction,
    ) = runScenario(construction, savedState = null) {
        flowController.presentPaymentOptions()

        val args = requireNotNull(EmbeddedActivityArgs.fromIntent(shadowOf(activity).nextStartedActivity))
        assertThat(args.paymentElementCallbackIdentifier).isEqualTo(callbackIdentifier)
    }

    @Test
    fun `callbacks belong to their owner and are removed when that owner is destroyed`(
        @TestParameter(
            "ActivityWithCreateIntent",
            "ActivityWithExternalPaymentMethod",
            "ActivityWithBothCallbacks",
            "FragmentWithCreateIntent",
            "FragmentWithExternalPaymentMethod",
            "FragmentWithBothCallbacks",
            "ActivityBuilder",
            "FragmentBuilder",
        ) construction: Construction,
    ) = runScenario(construction, savedState = null) {
        val first = this
        val firstCallbacks = PaymentElementCallbackReferences[first.callbackIdentifier]
        assertThat(firstCallbacks).isNotNull()
        assertThat(firstCallbacks).isNotSameInstanceAs(unrelatedCallbacks)

        runScenario(construction, savedState = null) {
            val secondCallbacks = PaymentElementCallbackReferences[callbackIdentifier]
            assertThat(callbackIdentifier).isNotEqualTo(first.callbackIdentifier)
            assertThat(secondCallbacks).isNotNull()
            assertThat(secondCallbacks).isNotSameInstanceAs(firstCallbacks)
            assertThat(PaymentElementCallbackReferences[first.callbackIdentifier]).isSameInstanceAs(firstCallbacks)

            first.controller.pause().stop().destroy()

            assertThat(PaymentElementCallbackReferences[callbackIdentifier]).isSameInstanceAs(secondCallbacks)
            // A removed key falls back to the unrelated callbacks seeded before either owner.
            assertThat(PaymentElementCallbackReferences[first.callbackIdentifier]).isSameInstanceAs(unrelatedCallbacks)
        }

        assertThat(PaymentElementCallbackReferences[first.callbackIdentifier]).isSameInstanceAs(unrelatedCallbacks)
    }

    @Test
    fun `restored owner reuses its identifier for launch and callback registration`(
        @TestParameter("ActivityBuilder", "FragmentBuilder") construction: Construction,
    ) {
        val savedState = Bundle()
        val originalIdentifier = runScenario(construction, savedState = null) {
            controller.saveInstanceState(savedState)
            callbackIdentifier
        }
        runScenario(construction, savedState) {
            assertThat(callbackIdentifier).isEqualTo(originalIdentifier)
            val callbacks = PaymentElementCallbackReferences[callbackIdentifier]
            assertThat(callbacks).isNotSameInstanceAs(unrelatedCallbacks)
            assertThat(callbacks?.createIntentCallback).isSameInstanceAs(createIntentCallback)

            flowController.presentPaymentOptions()

            val args = requireNotNull(EmbeddedActivityArgs.fromIntent(shadowOf(activity).nextStartedActivity))
            assertThat(args.paymentElementCallbackIdentifier).isEqualTo(originalIdentifier)
        }
    }

    @Test
    fun `PaymentSheet and FlowController use separate stores with distinct callback identifiers`() {
        val controller = Robolectric.buildActivity(FragmentActivity::class.java).create()
        val activity = controller.get()
        PaymentConfiguration.init(activity, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        val sheetCallback = CreateIntentCallback { _, _ -> error("Should not be called") }
        val flowControllerCallback = CreateIntentCallback { _, _ -> error("Should not be called") }
        try {
            val paymentSheet = PaymentSheet.Builder(PaymentSheetResultCallback { error("Should not be called") })
                .createIntentCallback(sheetCallback)
                .build(activity)
            val fragment = Fragment()
            activity.supportFragmentManager.beginTransaction().add(fragment, "flow-controller").commitNow()
            Construction.ActivityBuilder.create(activity, fragment, flowControllerCallback)
            val storeProvider = ViewModelProvider.create(
                owner = activity,
                factory = PaymentSheet.StoreViewModel.Factory,
            )
            val sheetStore = storeProvider[PaymentSheet.StoreViewModel::class]
            val flowControllerStore = storeProvider[FLOW_CONTROLLER_STORE_KEY, PaymentSheet.StoreViewModel::class]
            val sheetIdentifier = sheetStore.paymentElementCallbackIdentifier
            val flowControllerIdentifier = flowControllerStore.paymentElementCallbackIdentifier

            assertThat(flowControllerStore).isNotSameInstanceAs(sheetStore)
            assertThat(flowControllerIdentifier).isNotEqualTo(sheetIdentifier)
            assertThat(PaymentElementCallbackReferences[flowControllerIdentifier]?.createIntentCallback)
                .isSameInstanceAs(flowControllerCallback)
            assertThat(PaymentElementCallbackReferences[sheetIdentifier]?.createIntentCallback)
                .isSameInstanceAs(sheetCallback)
            controller.start().resume()
            paymentSheet.presentWithPaymentIntent("pi_secret")
            val args = requireNotNull(EmbeddedActivityArgs.fromIntent(shadowOf(activity).nextStartedActivity))
            assertThat(args.paymentElementCallbackIdentifier).isEqualTo(sheetIdentifier)
        } finally {
            controller.pause().stop().destroy()
        }
    }

    @Test
    fun `Activity and Fragment sharing a result registry have separate callbacks`() =
        runScenario(Construction.ActivityBuilder, savedState = null) {
            val activityCallbacks = PaymentElementCallbackReferences[callbackIdentifier]
            val fragmentCallback = CreateIntentCallback { _, _ -> error("Should not be called") }
            Construction.FragmentBuilder.create(activity, fragment, fragmentCallback)
            val fragmentIdentifier = ViewModelProvider.create(
                owner = fragment,
                factory = PaymentSheet.StoreViewModel.Factory,
            )[FLOW_CONTROLLER_STORE_KEY, PaymentSheet.StoreViewModel::class].paymentElementCallbackIdentifier

            assertThat(fragmentIdentifier).isNotEqualTo(callbackIdentifier)
            assertThat(PaymentElementCallbackReferences[fragmentIdentifier]?.createIntentCallback)
                .isSameInstanceAs(fragmentCallback)
            assertThat(PaymentElementCallbackReferences[callbackIdentifier]).isSameInstanceAs(activityCallbacks)

            activity.supportFragmentManager.beginTransaction().remove(fragment).commitNow()

            assertThat(PaymentElementCallbackReferences[fragmentIdentifier]).isSameInstanceAs(unrelatedCallbacks)
            assertThat(PaymentElementCallbackReferences[callbackIdentifier]).isSameInstanceAs(activityCallbacks)
            flowController.presentPaymentOptions()
            val args = requireNotNull(EmbeddedActivityArgs.fromIntent(shadowOf(activity).nextStartedActivity))
            assertThat(args.paymentElementCallbackIdentifier).isEqualTo(callbackIdentifier)
        }

    private fun <T> runScenario(
        construction: Construction,
        savedState: Bundle?,
        block: Scenario.() -> T,
    ): T {
        PaymentElementCallbackReferences["unrelated-owner"] = unrelatedCallbacks
        val controller = Robolectric.buildActivity(FragmentActivity::class.java).create(savedState)
        val activity = controller.get()
        PaymentConfiguration.init(activity, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        val fragment = activity.supportFragmentManager.findFragmentByTag("flow-controller") ?: Fragment().also {
            activity.supportFragmentManager.beginTransaction().add(it, "flow-controller").commitNow()
        }
        val createIntentCallback = CreateIntentCallback { _, _ -> error("Should not be called") }
        val flowController = construction.create(activity, fragment, createIntentCallback)
        val owner: ViewModelStoreOwner = if (construction.isFragment) fragment else activity
        val callbackIdentifier = ViewModelProvider.create(
            owner = owner,
            factory = PaymentSheet.StoreViewModel.Factory,
        )[FLOW_CONTROLLER_STORE_KEY, PaymentSheet.StoreViewModel::class].paymentElementCallbackIdentifier
        val viewModel = ViewModelProvider.create(owner = owner)[
            "FlowControllerViewModel(instance = $callbackIdentifier)",
            FlowControllerViewModel::class,
        ]
        viewModel.state = DefaultFlowController.State(
            paymentSheetState = PaymentSheetFixtures.PAYMENT_OPTIONS_CONTRACT_ARGS.state,
            config = PaymentSheetFixtures.CONFIG_GOOGLEPAY,
        )
        controller.start().resume()

        return try {
            Scenario(controller, fragment, flowController, callbackIdentifier, createIntentCallback).block()
        } finally {
            if (activity.lifecycle.currentState != Lifecycle.State.DESTROYED) {
                controller.pause().stop().destroy()
            }
        }
    }

    private class Scenario(
        val controller: ActivityController<FragmentActivity>,
        val fragment: Fragment,
        val flowController: PaymentSheet.FlowController,
        val callbackIdentifier: String,
        val createIntentCallback: CreateIntentCallback,
    ) {
        val activity: FragmentActivity
            get() = controller.get()
    }

    enum class Construction(val isFragment: Boolean) {
        Activity(isFragment = false),
        ActivityWithCreateIntent(isFragment = false),
        ActivityWithExternalPaymentMethod(isFragment = false),
        ActivityWithBothCallbacks(isFragment = false),
        Fragment(isFragment = true),
        FragmentWithCreateIntent(isFragment = true),
        FragmentWithExternalPaymentMethod(isFragment = true),
        FragmentWithBothCallbacks(isFragment = true),
        ActivityBuilder(isFragment = false),
        FragmentBuilder(isFragment = true);

        @Suppress("DEPRECATION")
        fun create(
            activity: FragmentActivity,
            fragment: Fragment,
            createIntentCallback: CreateIntentCallback,
        ): PaymentSheet.FlowController {
            val optionCallback = PaymentOptionCallback { error("Should not be called") }
            val resultCallback = PaymentSheetResultCallback { error("Should not be called") }
            val externalPaymentMethodConfirmHandler = ExternalPaymentMethodConfirmHandler { _, _ ->
                error("Should not be called")
            }
            val builder = PaymentSheet.FlowController.Builder(resultCallback, optionCallback)
                .createIntentCallback(createIntentCallback)
                .externalPaymentMethodConfirmHandler(externalPaymentMethodConfirmHandler)

            return when (this) {
                Activity -> PaymentSheet.FlowController.create(activity, optionCallback, resultCallback)
                ActivityWithCreateIntent -> PaymentSheet.FlowController.create(
                    activity, optionCallback, createIntentCallback, resultCallback,
                )
                ActivityWithExternalPaymentMethod -> PaymentSheet.FlowController.create(
                    activity,
                    externalPaymentMethodConfirmHandler,
                    optionCallback,
                    resultCallback,
                )
                ActivityWithBothCallbacks -> PaymentSheet.FlowController.create(
                    activity = activity,
                    paymentOptionCallback = optionCallback,
                    createIntentCallback = createIntentCallback,
                    externalPaymentMethodConfirmHandler = externalPaymentMethodConfirmHandler,
                    paymentResultCallback = resultCallback,
                )
                Fragment -> PaymentSheet.FlowController.create(fragment, optionCallback, resultCallback)
                FragmentWithCreateIntent -> PaymentSheet.FlowController.create(
                    fragment, optionCallback, createIntentCallback, resultCallback,
                )
                FragmentWithExternalPaymentMethod -> PaymentSheet.FlowController.create(
                    fragment,
                    externalPaymentMethodConfirmHandler,
                    optionCallback,
                    resultCallback,
                )
                FragmentWithBothCallbacks -> PaymentSheet.FlowController.create(
                    fragment = fragment,
                    paymentOptionCallback = optionCallback,
                    createIntentCallback = createIntentCallback,
                    externalPaymentMethodConfirmHandler = externalPaymentMethodConfirmHandler,
                    paymentResultCallback = resultCallback,
                )
                ActivityBuilder -> builder.build(activity)
                FragmentBuilder -> builder.build(fragment)
            }
        }
    }
}
