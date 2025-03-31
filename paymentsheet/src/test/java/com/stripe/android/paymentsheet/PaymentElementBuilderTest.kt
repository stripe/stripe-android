package com.stripe.android.paymentsheet

import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.FragmentScenario
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test

@RunWith(AndroidJUnit4::class)
internal class PaymentElementBuilderTest {
    @get:Rule
    val testActivityRule = createTestActivityRule<PaymentElementBuilderTestActivity>()

    @Test
    fun `On activity constructor with create intent callback, should properly register for Payment Sheet`() =
        testWithActivity { activity ->
            val createIntentCallback = newCreateIntentCallback()

            val paymentSheet = PaymentSheet(activity, createIntentCallback, newPaymentSheetResultCallback())

            assertThat(paymentSheet).isNotNull()

            val paymentSheetCallbacks = PaymentElementCallbackReferences[PAYMENT_SHEET_DEFAULT_CALLBACK_IDENTIFIER]

            assertThat(paymentSheetCallbacks?.createIntentCallback).isEqualTo(createIntentCallback)
        }

    @Test
    fun `On fragment constructor with create intent callback, should properly register for Payment Sheet`() =
        testWithFragment { fragment ->
            val createIntentCallback = newCreateIntentCallback()

            val paymentSheet = PaymentSheet(fragment, createIntentCallback, newPaymentSheetResultCallback())

            assertThat(paymentSheet).isNotNull()

            val paymentSheetCallbacks = PaymentElementCallbackReferences[PAYMENT_SHEET_DEFAULT_CALLBACK_IDENTIFIER]

            assertThat(paymentSheetCallbacks?.createIntentCallback).isEqualTo(createIntentCallback)
        }

    @Test
    fun `On activity constructor with EPM handler, should properly register for Payment Sheet`() =
        testWithActivity { activity ->
            val externalPaymentMethodConfirmHandler = newExternalPaymentMethodConfirmHandler()

            val paymentSheet = PaymentSheet(
                activity = activity,
                externalPaymentMethodConfirmHandler = externalPaymentMethodConfirmHandler,
                callback = newPaymentSheetResultCallback(),
            )

            assertThat(paymentSheet).isNotNull()

            val paymentSheetCallbacks = PaymentElementCallbackReferences[PAYMENT_SHEET_DEFAULT_CALLBACK_IDENTIFIER]

            assertThat(paymentSheetCallbacks?.externalPaymentMethodConfirmHandler)
                .isEqualTo(externalPaymentMethodConfirmHandler)
        }

    @Test
    fun `On fragment constructor with EPM handler, should properly register for Payment Sheet`() =
        testWithFragment { fragment ->
            val externalPaymentMethodConfirmHandler = newExternalPaymentMethodConfirmHandler()

            val paymentSheet = PaymentSheet(
                fragment = fragment,
                externalPaymentMethodConfirmHandler = externalPaymentMethodConfirmHandler,
                callback = newPaymentSheetResultCallback(),
            )

            assertThat(paymentSheet).isNotNull()

            val paymentSheetCallbacks = PaymentElementCallbackReferences[PAYMENT_SHEET_DEFAULT_CALLBACK_IDENTIFIER]

            assertThat(paymentSheetCallbacks?.externalPaymentMethodConfirmHandler)
                .isEqualTo(externalPaymentMethodConfirmHandler)
        }

    @Test
    fun `On activity constructor with callbacks, should properly register for Payment Sheet`() =
        testWithActivity { activity ->
            val createIntentCallback = newCreateIntentCallback()
            val externalPaymentMethodConfirmHandler = newExternalPaymentMethodConfirmHandler()

            val paymentSheet = PaymentSheet(
                activity = activity,
                createIntentCallback = createIntentCallback,
                externalPaymentMethodConfirmHandler = externalPaymentMethodConfirmHandler,
                paymentResultCallback = newPaymentSheetResultCallback(),
            )

            assertThat(paymentSheet).isNotNull()

            val paymentSheetCallbacks = PaymentElementCallbackReferences[PAYMENT_SHEET_DEFAULT_CALLBACK_IDENTIFIER]

            assertThat(paymentSheetCallbacks?.createIntentCallback).isEqualTo(createIntentCallback)
            assertThat(paymentSheetCallbacks?.externalPaymentMethodConfirmHandler)
                .isEqualTo(externalPaymentMethodConfirmHandler)
        }

    @Test
    fun `On fragment constructor with callbacks, should properly register for Payment Sheet`() =
        testWithFragment { fragment ->
            val createIntentCallback = newCreateIntentCallback()
            val externalPaymentMethodConfirmHandler = newExternalPaymentMethodConfirmHandler()

            val paymentSheet = PaymentSheet(
                fragment = fragment,
                createIntentCallback = createIntentCallback,
                externalPaymentMethodConfirmHandler = externalPaymentMethodConfirmHandler,
                paymentResultCallback = newPaymentSheetResultCallback(),
            )

            assertThat(paymentSheet).isNotNull()

            val paymentSheetCallbacks = PaymentElementCallbackReferences[PAYMENT_SHEET_DEFAULT_CALLBACK_IDENTIFIER]

            assertThat(paymentSheetCallbacks?.externalPaymentMethodConfirmHandler)
                .isEqualTo(externalPaymentMethodConfirmHandler)
        }

    @Test
    fun `On activity create with create intent callback, should properly register for Flow Controller`() =
        testWithActivity { activity ->
            val createIntentCallback = newCreateIntentCallback()

            val paymentSheet = PaymentSheet.FlowController.create(
                activity = activity,
                createIntentCallback = createIntentCallback,
                paymentOptionCallback = newPaymentOptionCallback(),
                paymentResultCallback = newPaymentSheetResultCallback(),
            )

            assertThat(paymentSheet).isNotNull()

            val paymentSheetCallbacks = PaymentElementCallbackReferences[PAYMENT_SHEET_DEFAULT_CALLBACK_IDENTIFIER]

            assertThat(paymentSheetCallbacks?.createIntentCallback).isEqualTo(createIntentCallback)
        }

    @Test
    fun `On fragment create with create intent callback, should properly register for Flow Controller`() =
        testWithFragment { fragment ->
            val createIntentCallback = newCreateIntentCallback()

            val paymentSheet = PaymentSheet.FlowController.create(
                fragment = fragment,
                createIntentCallback = createIntentCallback,
                paymentOptionCallback = newPaymentOptionCallback(),
                paymentResultCallback = newPaymentSheetResultCallback(),
            )

            assertThat(paymentSheet).isNotNull()

            val paymentSheetCallbacks = PaymentElementCallbackReferences[PAYMENT_SHEET_DEFAULT_CALLBACK_IDENTIFIER]

            assertThat(paymentSheetCallbacks?.createIntentCallback).isEqualTo(createIntentCallback)
        }

    @Test
    fun `On activity create with EPM handler, should properly register for Flow Controller`() =
        testWithActivity { activity ->
            val externalPaymentMethodConfirmHandler = newExternalPaymentMethodConfirmHandler()

            val paymentSheet = PaymentSheet.FlowController.create(
                activity = activity,
                externalPaymentMethodConfirmHandler = externalPaymentMethodConfirmHandler,
                paymentOptionCallback = newPaymentOptionCallback(),
                paymentResultCallback = newPaymentSheetResultCallback(),
            )

            assertThat(paymentSheet).isNotNull()

            val paymentSheetCallbacks = PaymentElementCallbackReferences[PAYMENT_SHEET_DEFAULT_CALLBACK_IDENTIFIER]

            assertThat(paymentSheetCallbacks?.externalPaymentMethodConfirmHandler)
                .isEqualTo(externalPaymentMethodConfirmHandler)
        }

    @Test
    fun `On fragment create with EPM handler, should properly register for Flow Controller`() =
        testWithFragment { fragment ->
            val externalPaymentMethodConfirmHandler = newExternalPaymentMethodConfirmHandler()

            val paymentSheet = PaymentSheet.FlowController.create(
                fragment = fragment,
                externalPaymentMethodConfirmHandler = externalPaymentMethodConfirmHandler,
                paymentOptionCallback = newPaymentOptionCallback(),
                paymentResultCallback = newPaymentSheetResultCallback(),
            )

            assertThat(paymentSheet).isNotNull()

            val paymentSheetCallbacks = PaymentElementCallbackReferences[PAYMENT_SHEET_DEFAULT_CALLBACK_IDENTIFIER]

            assertThat(paymentSheetCallbacks?.externalPaymentMethodConfirmHandler)
                .isEqualTo(externalPaymentMethodConfirmHandler)
        }

    @Test
    fun `On activity create with callbacks, should properly register for Flow Controller`() =
        testWithActivity { activity ->
            val createIntentCallback = newCreateIntentCallback()
            val externalPaymentMethodConfirmHandler = newExternalPaymentMethodConfirmHandler()

            val paymentSheet = PaymentSheet.FlowController.create(
                activity = activity,
                createIntentCallback = createIntentCallback,
                externalPaymentMethodConfirmHandler = externalPaymentMethodConfirmHandler,
                paymentOptionCallback = newPaymentOptionCallback(),
                paymentResultCallback = newPaymentSheetResultCallback(),
            )

            assertThat(paymentSheet).isNotNull()

            val paymentSheetCallbacks = PaymentElementCallbackReferences[PAYMENT_SHEET_DEFAULT_CALLBACK_IDENTIFIER]

            assertThat(paymentSheetCallbacks?.createIntentCallback).isEqualTo(createIntentCallback)
            assertThat(paymentSheetCallbacks?.externalPaymentMethodConfirmHandler)
                .isEqualTo(externalPaymentMethodConfirmHandler)
        }

    @Test
    fun `On fragment create with callbacks, should properly register for Flow Controller`() =
        testWithFragment { fragment ->
            val createIntentCallback = newCreateIntentCallback()
            val externalPaymentMethodConfirmHandler = newExternalPaymentMethodConfirmHandler()

            val paymentSheet = PaymentSheet.FlowController.create(
                fragment = fragment,
                createIntentCallback = createIntentCallback,
                externalPaymentMethodConfirmHandler = externalPaymentMethodConfirmHandler,
                paymentOptionCallback = newPaymentOptionCallback(),
                paymentResultCallback = newPaymentSheetResultCallback(),
            )

            assertThat(paymentSheet).isNotNull()

            val paymentSheetCallbacks = PaymentElementCallbackReferences[PAYMENT_SHEET_DEFAULT_CALLBACK_IDENTIFIER]

            assertThat(paymentSheetCallbacks?.externalPaymentMethodConfirmHandler)
                .isEqualTo(externalPaymentMethodConfirmHandler)
        }

    @Test
    fun `On activity build, should properly register callbacks for Payment Sheet`() = testWithActivity { activity ->
        val createIntentCallback = newCreateIntentCallback()
        val externalPaymentMethodConfirmHandler = newExternalPaymentMethodConfirmHandler()

        val paymentSheet = PaymentSheet.Builder(newPaymentSheetResultCallback())
            .createIntentCallback(createIntentCallback)
            .externalPaymentMethodConfirmHandler(externalPaymentMethodConfirmHandler)
            .build(activity)

        assertThat(paymentSheet).isNotNull()

        val paymentSheetCallbacks = PaymentElementCallbackReferences[PAYMENT_SHEET_DEFAULT_CALLBACK_IDENTIFIER]

        assertThat(paymentSheetCallbacks?.createIntentCallback).isEqualTo(createIntentCallback)
        assertThat(paymentSheetCallbacks?.externalPaymentMethodConfirmHandler)
            .isEqualTo(externalPaymentMethodConfirmHandler)
    }

    @Test
    fun `On activity build, should properly register callbacks for Flow Controller`() = testWithActivity { activity ->
        val createIntentCallback = newCreateIntentCallback()
        val externalPaymentMethodConfirmHandler = newExternalPaymentMethodConfirmHandler()

        val flowController = PaymentSheet.FlowController.Builder(
            resultCallback = newPaymentSheetResultCallback(),
            paymentOptionCallback = newPaymentOptionCallback(),
        )
            .createIntentCallback(createIntentCallback)
            .externalPaymentMethodConfirmHandler(externalPaymentMethodConfirmHandler)
            .build(activity)

        assertThat(flowController).isNotNull()

        val flowControllerCallbacks = PaymentElementCallbackReferences[FLOW_CONTROLLER_DEFAULT_CALLBACK_IDENTIFIER]

        assertThat(flowControllerCallbacks?.createIntentCallback).isEqualTo(createIntentCallback)
        assertThat(flowControllerCallbacks?.externalPaymentMethodConfirmHandler)
            .isEqualTo(externalPaymentMethodConfirmHandler)
    }

    @Test
    fun `On fragment build, should properly register callbacks for Payment Sheet`() = testWithFragment { fragment ->
        val createIntentCallback = newCreateIntentCallback()
        val externalPaymentMethodConfirmHandler = newExternalPaymentMethodConfirmHandler()

        val paymentSheet = PaymentSheet.Builder(newPaymentSheetResultCallback())
            .createIntentCallback(createIntentCallback)
            .externalPaymentMethodConfirmHandler(externalPaymentMethodConfirmHandler)
            .build(fragment)

        assertThat(paymentSheet).isNotNull()

        val paymentSheetCallbacks = PaymentElementCallbackReferences[PAYMENT_SHEET_DEFAULT_CALLBACK_IDENTIFIER]

        assertThat(paymentSheetCallbacks?.createIntentCallback).isEqualTo(createIntentCallback)
        assertThat(paymentSheetCallbacks?.externalPaymentMethodConfirmHandler)
            .isEqualTo(externalPaymentMethodConfirmHandler)
    }

    @Test
    fun `On fragment build, should properly register callbacks for Flow Controller`() = testWithFragment { fragment ->
        val createIntentCallback = newCreateIntentCallback()
        val externalPaymentMethodConfirmHandler = newExternalPaymentMethodConfirmHandler()

        val flowController = PaymentSheet.FlowController.Builder(
            resultCallback = newPaymentSheetResultCallback(),
            paymentOptionCallback = newPaymentOptionCallback(),
        )
            .createIntentCallback(createIntentCallback)
            .externalPaymentMethodConfirmHandler(externalPaymentMethodConfirmHandler)
            .build(fragment)

        assertThat(flowController).isNotNull()

        val flowControllerCallbacks = PaymentElementCallbackReferences[FLOW_CONTROLLER_DEFAULT_CALLBACK_IDENTIFIER]

        assertThat(flowControllerCallbacks?.createIntentCallback).isEqualTo(createIntentCallback)
        assertThat(flowControllerCallbacks?.externalPaymentMethodConfirmHandler)
            .isEqualTo(externalPaymentMethodConfirmHandler)
    }

    private fun newPaymentSheetResultCallback(): PaymentSheetResultCallback {
        return PaymentSheetResultCallback { _ ->
            error("Should not be called!")
        }
    }

    private fun newPaymentOptionCallback(): PaymentOptionCallback {
        return PaymentOptionCallback { _ ->
            error("Should not be called!")
        }
    }

    private fun newCreateIntentCallback(): CreateIntentCallback {
        return CreateIntentCallback { _, _ ->
            error("Should not be called!")
        }
    }

    private fun newExternalPaymentMethodConfirmHandler(): ExternalPaymentMethodConfirmHandler {
        return ExternalPaymentMethodConfirmHandler { _, _ ->
            error("Should not be called!")
        }
    }

    private fun testWithActivity(test: (ComponentActivity) -> Unit) = runTest {
        val testCompleted = CountDownLatch(1)

        ActivityScenario.launch(PaymentElementBuilderTestActivity::class.java).use { scenario ->
            scenario.moveToState(Lifecycle.State.CREATED)

            scenario.onActivity { activity ->
                test(activity)
                testCompleted.countDown()
            }
        }

        testCompleted.await(5, TimeUnit.SECONDS)
    }

    private fun testWithFragment(test: (Fragment) -> Unit) = runTest {
        val testCompleted = CountDownLatch(1)

        FragmentScenario.launch(PaymentElementBuilderTestFragment::class.java).use { scenario ->
            scenario.moveToState(Lifecycle.State.CREATED)

            scenario.onFragment { fragment ->
                test(fragment)
                testCompleted.countDown()
            }
        }

        testCompleted.await(5, TimeUnit.SECONDS)
    }

    class PaymentElementBuilderTestActivity : AppCompatActivity()

    class PaymentElementBuilderTestFragment : Fragment()
}
