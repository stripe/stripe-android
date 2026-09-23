package com.stripe.android.paymentsheet

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.utils.PaymentElementCallbackTestRule
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestParameterInjector
import org.robolectric.android.controller.ActivityController

@RunWith(RobolectricTestParameterInjector::class)
internal class PaymentElementCallbackLifecycleTest {
    @get:Rule
    val activityRule = createTestActivityRule<TestActivity>()

    @get:Rule
    val callbackRule = PaymentElementCallbackTestRule()

    @get:Rule
    val coroutineRule = CoroutineTestRule(UnconfinedTestDispatcher())

    @Test
    fun `Destroying an earlier host preserves the latest host's callbacks`(
        @TestParameter creation: Creation,
        @TestParameter ownerType: OwnerType,
    ) = runScenario(creation, ownerType) {
        createElement(firstHost, createCallback("first"))
        val latestCallback = createCallback("latest")
        createElement(secondHost, latestCallback)

        firstHost.destroy()

        assertThat(PaymentElementCallbackReferences[creation.identifier]?.createIntentCallback)
            .isSameInstanceAs(latestCallback)
    }

    @Test
    fun `Destroying the registering host removes its callbacks`(
        @TestParameter creation: Creation,
        @TestParameter ownerType: OwnerType,
    ) = runScenario(creation, ownerType) {
        val callback = createCallback("registered")
        createElement(firstHost, callback)
        assertThat(PaymentElementCallbackReferences[creation.identifier]?.createIntentCallback)
            .isSameInstanceAs(callback)

        firstHost.destroy()

        assertThat(PaymentElementCallbackReferences[creation.identifier]).isNull()
    }

    private fun runScenario(creation: Creation, ownerType: OwnerType, block: Scenario.() -> Unit) {
        val firstHost = createHost(ownerType)
        val secondHost = createHost(ownerType)
        try {
            Scenario(creation, firstHost, secondHost).block()
        } finally {
            firstHost.close()
            secondHost.close()
        }
    }

    private fun createHost(ownerType: OwnerType): Host {
        val controller = Robolectric.buildActivity(TestActivity::class.java).create()
        val activity = controller.get()
        PaymentConfiguration.init(activity, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        val fragment = if (ownerType == OwnerType.Fragment) {
            Fragment().also {
                activity.supportFragmentManager.beginTransaction().add(it, "host").commitNow()
            }
        } else {
            null
        }
        return Host(controller, fragment)
    }

    private fun createCallback(name: String) = CreateIntentCallback { _, _ -> error(name) }

    private class Scenario(
        val creation: Creation,
        val firstHost: Host,
        val secondHost: Host,
    ) {
        @Suppress("DEPRECATION")
        fun createElement(host: Host, callback: CreateIntentCallback) {
            val fragment = host.fragment
            val resultCallback = PaymentSheetResultCallback { error("Should not be called!") }
            val optionCallback = PaymentOptionCallback { error("Should not be called!") }
            when (creation) {
                Creation.PaymentSheetBuilder -> {
                    val builder = PaymentSheet.Builder(resultCallback).createIntentCallback(callback)
                    if (fragment == null) builder.build(host.activity) else builder.build(fragment)
                }
                Creation.PaymentSheetConstructor -> {
                    if (fragment == null) {
                        PaymentSheet(host.activity, callback, resultCallback)
                    } else {
                        PaymentSheet(fragment, callback, resultCallback)
                    }
                }
                Creation.FlowControllerBuilder -> {
                    val builder = PaymentSheet.FlowController.Builder(resultCallback, optionCallback)
                        .createIntentCallback(callback)
                    if (fragment == null) builder.build(host.activity) else builder.build(fragment)
                }
                Creation.FlowControllerFactory -> {
                    if (fragment == null) {
                        PaymentSheet.FlowController.create(host.activity, optionCallback, callback, resultCallback)
                    } else {
                        PaymentSheet.FlowController.create(fragment, optionCallback, callback, resultCallback)
                    }
                }
            }
        }
    }

    private class Host(
        private val controller: ActivityController<TestActivity>,
        val fragment: Fragment?,
    ) {
        val activity: TestActivity
            get() = controller.get()

        fun destroy() {
            if (fragment == null) {
                controller.destroy()
            } else {
                activity.supportFragmentManager.beginTransaction().remove(fragment).commitNow()
                assertThat(activity.lifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
            }
        }

        fun close() {
            if (activity.lifecycle.currentState != Lifecycle.State.DESTROYED) {
                controller.destroy()
            }
        }
    }

    enum class Creation(val identifier: String) {
        PaymentSheetBuilder(PAYMENT_SHEET_DEFAULT_CALLBACK_IDENTIFIER),
        PaymentSheetConstructor(PAYMENT_SHEET_DEFAULT_CALLBACK_IDENTIFIER),
        FlowControllerBuilder(FLOW_CONTROLLER_DEFAULT_CALLBACK_IDENTIFIER),
        FlowControllerFactory(FLOW_CONTROLLER_DEFAULT_CALLBACK_IDENTIFIER),
    }

    enum class OwnerType {
        Activity,
        Fragment,
    }

    class TestActivity : AppCompatActivity()
}
