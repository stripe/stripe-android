@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.checkout

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.checkouttesting.checkoutInit
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.MainActivity
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

internal class CheckoutPaymentElementTestRunnerContext(
    private val presenter: CheckoutPresenter,
    private val countDownLatch: CountDownLatch,
) {
    fun confirm() {
        presenter.confirm()
    }

    /**
     * Normally a test succeeds when [CheckoutController.ResultCallback] is invoked. Tests that
     * intentionally do not confirm should call this after making their assertions.
     */
    fun markTestSucceeded() {
        countDownLatch.countDown()
    }
}

internal fun runCheckoutPaymentElementTest(
    networkRule: NetworkRule,
    resultCallback: CheckoutController.ResultCallback = CheckoutController.ResultCallback {
        error("Override + validate if expected.")
    },
    checkoutInitResponse: (MockResponse) -> Unit = { response ->
        response.testBodyFromFile("checkout-session-init.json") { json ->
            json.put("customer_email", "checkout@example.com")
            json.getJSONObject("elements_session").remove("link_settings")
        }
    },
    successTimeoutSeconds: Long = 5L,
    setup: suspend (CheckoutController) -> Unit,
    block: (CheckoutPaymentElementTestRunnerContext) -> Unit,
) {
    val countDownLatch = CountDownLatch(1)

    networkRule.checkoutInit(responseFactory = checkoutInitResponse)

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
            setup(controller)
        }

        lateinit var presenter: CheckoutPresenter
        scenario.onActivity { activity ->
            presenter = controller.createPresenter(activity)
            val paymentElement = presenter.paymentElement()
            activity.setContent {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    paymentElement.Content()
                }
            }
        }

        scenario.moveToState(Lifecycle.State.RESUMED)

        block(
            CheckoutPaymentElementTestRunnerContext(
                presenter = presenter,
                countDownLatch = countDownLatch,
            )
        )

        val didCompleteSuccessfully = countDownLatch.await(successTimeoutSeconds, TimeUnit.SECONDS)
        networkRule.validate()
        assertThat(didCompleteSuccessfully).isTrue()
        controller.destroy()
    }
}
