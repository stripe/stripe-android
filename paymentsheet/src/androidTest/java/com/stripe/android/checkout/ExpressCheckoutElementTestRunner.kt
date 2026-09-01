@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.checkout

import androidx.activity.compose.setContent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.checkouttesting.checkoutInit
import com.stripe.android.elements.ExpressCheckoutElement
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.paymentsheet.MainActivity
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

internal fun runExpressCheckoutElementTest(
    networkRule: NetworkRule,
    resultCallback: CheckoutController.ResultCallback = CheckoutController.ResultCallback {
        error("Override + validate if expected.")
    },
    successTimeoutSeconds: Long = 5L,
    assertions: (CheckoutController) -> Unit,
    block: () -> Unit,
) {
    val countDownLatch = CountDownLatch(1)

    networkRule.checkoutInit(responseFactory = CheckoutInitResponseFactory::create)

    ActivityScenario.launch(MainActivity::class.java).use { scenario ->
        scenario.moveToState(Lifecycle.State.CREATED)

        PaymentConfiguration.init(ApplicationProvider.getApplicationContext(), "pk_test_123")
        val controller: CheckoutController = CheckoutController.Builder(
            application = ApplicationProvider.getApplicationContext(),
            savedStateHandle = SavedStateHandle(),
        ).resultCallback { result ->
            resultCallback.onResult(result)
            countDownLatch.countDown()
        }.build()

        runBlocking {
            controller.configure(
                DEFAULT_CLIENT_SECRET,
                configuration = CheckoutController.Configuration()
                    .expressCheckoutElement(ExpressCheckoutElement.Configuration())
            ).getOrThrow()
        }
        assertions(controller)

        scenario.onActivity { activity ->
            val expressCheckoutElement = controller.createPresenter(activity).expressCheckoutElement()
            activity.setContent {
                expressCheckoutElement.Content()
            }
        }

        scenario.moveToState(Lifecycle.State.RESUMED)

        block()

        val didCompleteSuccessfully = countDownLatch.await(successTimeoutSeconds, TimeUnit.SECONDS)
        networkRule.validate()
        assertThat(didCompleteSuccessfully).isTrue()
        scenario.onActivity {
            controller.destroy()
        }
    }
}

private const val DEFAULT_CLIENT_SECRET = "${DEFAULT_CHECKOUT_SESSION_ID}_secret_example"
