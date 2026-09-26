package com.stripe.android.paymentsheet

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.stripe.android.core.reactnative.ReactNativeSdkInternal
import com.stripe.android.core.reactnative.UnregisterSignal
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbacks
import com.stripe.android.utils.PaymentElementCallbackTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestParameterInjector
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController

@RunWith(RobolectricTestParameterInjector::class)
internal class PaymentSheetTest {
    @get:Rule
    val callbackRule = PaymentElementCallbackTestRule()

    @Test
    fun `launch uses the owner's stored identifier`(
        @TestParameter construction: Construction,
    ) = runScenario(construction, savedState = null) {
        paymentSheet.presentWithPaymentIntent("pi_secret")

        val args = requireNotNull(PaymentSheetContract.Args.fromIntent(shadowOf(activity).nextStartedActivity))
        assertThat(args.paymentElementCallbackIdentifier).isEqualTo(callbackIdentifier)
        assertThat(args.initializedViaCompose).isFalse()
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
            "ReactNativeBuilder",
        ) construction: Construction,
    ) = runScenario(construction, savedState = null) {
        val first = this
        val firstCallbacks = PaymentElementCallbackReferences[first.callbackIdentifier]
        assertThat(firstCallbacks).isNotNull()

        runScenario(construction, savedState = null) {
            val secondCallbacks = PaymentElementCallbackReferences[callbackIdentifier]
            assertThat(callbackIdentifier).isNotEqualTo(first.callbackIdentifier)
            assertThat(secondCallbacks).isNotNull()
            assertThat(secondCallbacks).isNotSameInstanceAs(firstCallbacks)
            assertThat(PaymentElementCallbackReferences[first.callbackIdentifier]).isSameInstanceAs(firstCallbacks)

            first.controller.pause().stop().destroy()

            assertThat(PaymentElementCallbackReferences[callbackIdentifier]).isSameInstanceAs(secondCallbacks)
            // A removed key falls back to the remaining owner's callbacks.
            assertThat(PaymentElementCallbackReferences[first.callbackIdentifier]).isSameInstanceAs(secondCallbacks)
        }

        assertThat(PaymentElementCallbackReferences[first.callbackIdentifier]).isNull()
    }

    @Test
    fun `restored owner reuses its identifier for launch and callback registration`(
        @TestParameter("ActivityBuilder", "FragmentBuilder", "ReactNativeBuilder") construction: Construction,
    ) {
        val savedState = Bundle()
        val originalIdentifier = runScenario(construction, savedState = null) {
            controller.saveInstanceState(savedState)
            callbackIdentifier
        }
        val unrelatedCallbacks = PaymentElementCallbacks.Builder().build()
        PaymentElementCallbackReferences["unrelated-owner"] = unrelatedCallbacks

        runScenario(construction, savedState) {
            assertThat(callbackIdentifier).isEqualTo(originalIdentifier)
            val callbacks = PaymentElementCallbackReferences[callbackIdentifier]
            assertThat(callbacks).isNotSameInstanceAs(unrelatedCallbacks)
            assertThat(callbacks?.createIntentCallback).isSameInstanceAs(createIntentCallback)

            paymentSheet.presentWithPaymentIntent("pi_secret")

            val args = requireNotNull(PaymentSheetContract.Args.fromIntent(shadowOf(activity).nextStartedActivity))
            assertThat(args.paymentElementCallbackIdentifier).isEqualTo(originalIdentifier)
        }
    }

    private fun <T> runScenario(
        construction: Construction,
        savedState: Bundle?,
        block: Scenario.() -> T,
    ): T {
        val controller = Robolectric.buildActivity(FragmentActivity::class.java).create(savedState)
        val activity = controller.get()
        val fragment = activity.supportFragmentManager.findFragmentByTag("payment-sheet") ?: Fragment().also {
            activity.supportFragmentManager.beginTransaction().add(it, "payment-sheet").commitNow()
        }
        val createIntentCallback = CreateIntentCallback { _, _ -> error("Should not be called") }
        val paymentSheet = construction.create(activity, fragment, createIntentCallback)
        val owner: ViewModelStoreOwner = if (construction.isFragment) fragment else activity
        val callbackIdentifier = ViewModelProvider.create(
            owner = owner,
            factory = PaymentSheet.StoreViewModel.Factory,
        )[PaymentSheet.StoreViewModel::class].paymentElementCallbackIdentifier
        controller.start().resume()

        return try {
            Scenario(controller, paymentSheet, callbackIdentifier, createIntentCallback).block()
        } finally {
            if (activity.lifecycle.currentState != Lifecycle.State.DESTROYED) {
                controller.pause().stop().destroy()
            }
        }
    }

    private class Scenario(
        val controller: ActivityController<FragmentActivity>,
        val paymentSheet: PaymentSheet,
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
        FragmentBuilder(isFragment = true),
        ReactNativeBuilder(isFragment = false);

        @Suppress("DEPRECATION")
        @OptIn(ReactNativeSdkInternal::class)
        fun create(
            activity: FragmentActivity,
            fragment: Fragment,
            createIntentCallback: CreateIntentCallback,
        ): PaymentSheet {
            val resultCallback = PaymentSheetResultCallback { error("Should not be called") }
            val externalPaymentMethodConfirmHandler = ExternalPaymentMethodConfirmHandler { _, _ ->
                error("Should not be called")
            }
            val builder = PaymentSheet.Builder(resultCallback)
                .createIntentCallback(createIntentCallback)
                .externalPaymentMethodConfirmHandler(externalPaymentMethodConfirmHandler)

            return when (this) {
                Activity -> PaymentSheet(activity, resultCallback)
                ActivityWithCreateIntent -> PaymentSheet(activity, createIntentCallback, resultCallback)
                ActivityWithExternalPaymentMethod -> PaymentSheet(
                    activity,
                    externalPaymentMethodConfirmHandler,
                    resultCallback,
                )
                ActivityWithBothCallbacks -> PaymentSheet(
                    activity,
                    createIntentCallback,
                    externalPaymentMethodConfirmHandler,
                    resultCallback,
                )
                Fragment -> PaymentSheet(fragment, resultCallback)
                FragmentWithCreateIntent -> PaymentSheet(fragment, createIntentCallback, resultCallback)
                FragmentWithExternalPaymentMethod -> PaymentSheet(
                    fragment,
                    externalPaymentMethodConfirmHandler,
                    resultCallback,
                )
                FragmentWithBothCallbacks -> PaymentSheet(
                    fragment,
                    createIntentCallback,
                    externalPaymentMethodConfirmHandler,
                    resultCallback,
                )
                ActivityBuilder -> builder.build(activity)
                FragmentBuilder -> builder.build(fragment)
                ReactNativeBuilder -> builder.build(activity, UnregisterSignal())
            }
        }
    }
}
