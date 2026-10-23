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
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleCallback
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.checkouttesting.checkoutInit
import com.stripe.android.elements.PaymentElement
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.TestApiKeys
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.MainActivity
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

internal class CheckoutPaymentElementScenario(
    private var presenter: CheckoutPresenter,
    val controller: CheckoutController,
    private val countDownLatch: CountDownLatch,
    private val scenario: ActivityScenario<MainActivity>,
    private val renderPaymentElementContent: Boolean,
) {
    fun presentPaymentOptions() {
        presenter.paymentElement().present()
    }

    fun presentShippingAddressElement() {
        scenario.onActivity {
            presenter.shippingAddressElement().present()
        }
    }

    fun confirm() {
        presenter.confirm()
    }

    fun recreateHost() {
        scenario.moveToState(Lifecycle.State.CREATED)
        scenario.recreate()
        presenter = scenario.createCheckoutPresenter(
            controller = controller,
            renderPaymentElementContent = renderPaymentElementContent,
        )
        scenario.moveToState(Lifecycle.State.RESUMED)
    }

    /**
     * Recreates the host while another Activity, such as the shipping address sheet, is on top.
     * [ActivityScenario.recreate] first resumes the host, which never happens while it is covered,
     * so recreate the Activity directly and rebuild the presenter at the end of the new instance's
     * `onCreate`, as an app creating its presenter there would.
     */
    fun recreateHostUnderSheet() {
        lateinit var previousActivity: MainActivity
        scenario.onActivity { previousActivity = it }

        val recreated = CountDownLatch(1)
        val callback = ActivityLifecycleCallback { activity, stage ->
            if (stage == Stage.CREATED && activity is MainActivity && activity !== previousActivity) {
                presenter = activity.createCheckoutPresenter(
                    controller = controller,
                    renderPaymentElementContent = renderPaymentElementContent,
                )
                recreated.countDown()
            }
        }
        val monitor = ActivityLifecycleMonitorRegistry.getInstance()
        monitor.addLifecycleCallback(callback)
        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync { previousActivity.recreate() }
            check(recreated.await(10, TimeUnit.SECONDS)) { "Timed out waiting for the host to be recreated." }
        } finally {
            monitor.removeLifecycleCallback(callback)
        }
    }

    /**
     * Normally a test succeeds when [CheckoutController.ResultCallback] is invoked. Tests that
     * intentionally do not confirm should call this after making their assertions.
     */
    fun markTestSucceeded() {
        countDownLatch.countDown()
    }
}

internal fun runCheckoutPaymentElementScenario(
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
    renderPaymentElementContent: Boolean = true,
    rowSelectionBehavior: PaymentElement.RowSelectionBehavior = PaymentElement.RowSelectionBehavior.default(),
    setup: suspend (CheckoutController) -> Unit,
    block: suspend CheckoutPaymentElementScenario.() -> Unit,
) {
    val countDownLatch = CountDownLatch(1)

    networkRule.checkoutInit(responseFactory = checkoutInitResponse)

    ActivityScenario.launch(MainActivity::class.java).use { scenario ->
        scenario.moveToState(Lifecycle.State.CREATED)

        PaymentConfiguration.init(
            ApplicationProvider.getApplicationContext(),
            TestApiKeys.PUBLISHABLE,
            TestApiKeys.ACCOUNT,
        )
        val controller: CheckoutController = CheckoutController.Builder(
            application = ApplicationProvider.getApplicationContext(),
            savedStateHandle = SavedStateHandle(),
        ).rowSelectionBehavior(rowSelectionBehavior).resultCallback { result ->
            resultCallback.onResult(result)
            countDownLatch.countDown()
        }.build()

        runBlocking {
            setup(controller)
        }

        lateinit var presenter: CheckoutPresenter
        presenter = scenario.createCheckoutPresenter(
            controller = controller,
            renderPaymentElementContent = renderPaymentElementContent,
        )

        scenario.moveToState(Lifecycle.State.RESUMED)

        runBlocking {
            block(
                CheckoutPaymentElementScenario(
                    presenter = presenter,
                    controller = controller,
                    countDownLatch = countDownLatch,
                    scenario = scenario,
                    renderPaymentElementContent = renderPaymentElementContent,
                )
            )
        }

        val didCompleteSuccessfully = countDownLatch.await(successTimeoutSeconds, TimeUnit.SECONDS)
        networkRule.validate()
        assertThat(didCompleteSuccessfully).isTrue()
        scenario.onActivity {
            controller.destroy()
        }
    }
}

private fun ActivityScenario<MainActivity>.createCheckoutPresenter(
    controller: CheckoutController,
    renderPaymentElementContent: Boolean,
): CheckoutPresenter {
    lateinit var presenter: CheckoutPresenter
    onActivity { activity ->
        presenter = activity.createCheckoutPresenter(controller, renderPaymentElementContent)
    }
    return presenter
}

private fun MainActivity.createCheckoutPresenter(
    controller: CheckoutController,
    renderPaymentElementContent: Boolean,
): CheckoutPresenter {
    val presenter = controller.createPresenter(this)
    if (renderPaymentElementContent) {
        val paymentElement = presenter.paymentElement()
        setContent {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                paymentElement.Content()
            }
        }
    }
    return presenter
}
