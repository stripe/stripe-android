@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.checkout

import android.os.Parcel
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
    controller: CheckoutController,
    private var savedStateHandle: SavedStateHandle,
    private val createController: (SavedStateHandle) -> CheckoutController,
    private val countDownLatch: CountDownLatch,
    private val scenario: ActivityScenario<MainActivity>,
    private val renderPaymentElementContent: Boolean,
) {
    var controller: CheckoutController = controller
        private set

    fun presentPaymentOptions() {
        scenario.onActivity {
            presenter.paymentElement().present()
        }
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

    fun recreateAfterProcessDeath() {
        scenario.moveToState(Lifecycle.State.CREATED)
        scenario.onActivity {
            savedStateHandle = savedStateHandle.restoreFromParcel()
            controller.destroy()
        }
        scenario.recreate()
        scenario.onActivity {
            controller = createController(savedStateHandle)
        }
        presenter = scenario.createCheckoutPresenter(
            controller = controller,
            renderPaymentElementContent = renderPaymentElementContent,
        )
        scenario.moveToState(Lifecycle.State.RESUMED)
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
        val createController = { savedStateHandle: SavedStateHandle ->
            CheckoutController.Builder(
                application = ApplicationProvider.getApplicationContext(),
                savedStateHandle = savedStateHandle,
            ).rowSelectionBehavior(rowSelectionBehavior).resultCallback { result ->
                resultCallback.onResult(result)
                countDownLatch.countDown()
            }.build()
        }
        val savedStateHandle = SavedStateHandle()
        lateinit var controller: CheckoutController
        scenario.onActivity {
            controller = createController(savedStateHandle)
        }

        runBlocking {
            setup(controller)
        }

        lateinit var presenter: CheckoutPresenter
        presenter = scenario.createCheckoutPresenter(
            controller = controller,
            renderPaymentElementContent = renderPaymentElementContent,
        )

        scenario.moveToState(Lifecycle.State.RESUMED)

        val checkoutScenario = CheckoutPaymentElementScenario(
            presenter = presenter,
            controller = controller,
            savedStateHandle = savedStateHandle,
            createController = createController,
            countDownLatch = countDownLatch,
            scenario = scenario,
            renderPaymentElementContent = renderPaymentElementContent,
        )
        try {
            runBlocking {
                block(checkoutScenario)
            }

            val didCompleteSuccessfully = countDownLatch.await(successTimeoutSeconds, TimeUnit.SECONDS)
            networkRule.validate()
            assertThat(didCompleteSuccessfully).isTrue()
        } finally {
            scenario.onActivity {
                checkoutScenario.controller.destroy()
            }
        }
    }
}

@Suppress("RestrictedApi")
private fun SavedStateHandle.restoreFromParcel(): SavedStateHandle {
    val parcel = Parcel.obtain()
    return try {
        parcel.writeBundle(savedStateProvider().saveState())
        parcel.setDataPosition(0)
        SavedStateHandle.createHandle(parcel.readBundle(CheckoutController::class.java.classLoader), null)
    } finally {
        parcel.recycle()
    }
}

private fun ActivityScenario<MainActivity>.createCheckoutPresenter(
    controller: CheckoutController,
    renderPaymentElementContent: Boolean,
): CheckoutPresenter {
    lateinit var presenter: CheckoutPresenter
    onActivity { activity ->
        presenter = controller.createPresenter(activity)
        if (renderPaymentElementContent) {
            val paymentElement = presenter.paymentElement()
            activity.setContent {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    paymentElement.Content()
                }
            }
        }
    }
    return presenter
}
