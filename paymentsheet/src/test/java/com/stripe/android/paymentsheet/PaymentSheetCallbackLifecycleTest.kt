package com.stripe.android.paymentsheet

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.reactnative.ReactNativeSdkInternal
import com.stripe.android.core.reactnative.UnregisterSignal
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.utils.PaymentElementCallbackTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController

@RunWith(RobolectricTestRunner::class)
internal class PaymentSheetCallbackLifecycleTest {
    @get:Rule
    val callbackRule = PaymentElementCallbackTestRule()

    @Test
    fun `destroying the previous activity after building the next sheet preserves its deferred callback`() {
        val firstIdentifier = runScenario(savedState = null) {
            val first = this
            val firstArgs = presentDeferredIntent()
            assertDeferredCallback(firstArgs)

            runScenario(savedState = null) {
                assertThat(createIntentCallback).isNotSameInstanceAs(first.createIntentCallback)

                // B is already resumed and its sheet is initialized before A's lifecycle cleanup runs.
                first.controller.pause().stop().destroy()

                val secondArgs = presentDeferredIntent()
                // Exercise the loader's real check first, so the legacy shared key produces
                // "No callback for deferred intent." instead of failing only on identifier equality.
                assertDeferredCallback(secondArgs)
                assertThat(secondArgs.paymentElementCallbackIdentifier)
                    .isNotEqualTo(firstArgs.paymentElementCallbackIdentifier)
            }

            firstArgs.paymentElementCallbackIdentifier
        }

        // Lookup falls back to another owner's callbacks, so check removal after both owners are gone.
        assertThat(PaymentElementCallbackReferences[firstIdentifier]).isNull()
    }

    @Test
    fun `destroying the previous activity before building the next sheet preserves its deferred callback`() {
        runScenario(savedState = null) {
            assertDeferredCallback(presentDeferredIntent())
        }

        runScenario(savedState = null) {
            assertDeferredCallback(presentDeferredIntent())
        }
    }

    @Test
    fun `the surviving activity reuses its callback identifier when restored from saved state`() {
        val savedState = Bundle()
        val survivingIdentifier = runScenario(savedState = null) {
            val first = this

            runScenario(savedState = null) {
                first.controller.pause().stop().destroy()
                val args = presentDeferredIntent()
                assertDeferredCallback(args)
                controller.saveInstanceState(savedState)
                args.paymentElementCallbackIdentifier
            }
        }

        runScenario(savedState = savedState) {
            val args = presentDeferredIntent()
            assertThat(args.paymentElementCallbackIdentifier).isEqualTo(survivingIdentifier)
            assertDeferredCallback(args)
        }
    }

    @OptIn(ReactNativeSdkInternal::class)
    private fun <T> runScenario(savedState: Bundle?, block: Scenario.() -> T): T {
        val controller = Robolectric.buildActivity(FragmentActivity::class.java)
            .create(savedState).start().resume()
        val activity = controller.get()
        val signal = UnregisterSignal()

        return try {
            assertThat(activity.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
            val createIntentCallback = CreateIntentCallback { _, _ -> error("Unexpected callback for $activity") }
            // This native SDK overload supports construction after resume without a React Native runtime.
            val paymentSheet = PaymentSheet.Builder { error("Should not be called") }
                .createIntentCallback(createIntentCallback)
                .build(activity, signal)

            Scenario(controller, paymentSheet, createIntentCallback).block()
        } finally {
            if (activity.lifecycle.currentState != Lifecycle.State.DESTROYED) {
                controller.pause().stop().destroy()
            }
            signal.unregister()
        }
    }

    private class Scenario(
        val controller: ActivityController<FragmentActivity>,
        val paymentSheet: PaymentSheet,
        val createIntentCallback: CreateIntentCallback,
    ) {
        fun presentDeferredIntent(): PaymentSheetContract.Args {
            paymentSheet.presentWithIntentConfiguration(
                PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(amount = 1099, currency = "usd"),
                )
            )
            return requireNotNull(PaymentSheetContract.Args.fromIntent(shadowOf(controller.get()).nextStartedActivity))
        }

        fun assertDeferredCallback(args: PaymentSheetContract.Args) {
            val callbacks = PaymentElementCallbackReferences[args.paymentElementCallbackIdentifier]
            val initializationMode = args.initializationMode as PaymentElementLoader.InitializationMode.DeferredIntent
            assertThat(initializationMode.integrationMetadata(callbacks))
                .isEqualTo(IntegrationMetadata.DeferredIntent.WithPaymentMethod(initializationMode.intentConfiguration))
            assertThat(callbacks?.createIntentCallback).isSameInstanceAs(createIntentCallback)
        }
    }
}
