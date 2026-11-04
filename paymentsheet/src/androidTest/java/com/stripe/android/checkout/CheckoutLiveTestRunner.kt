@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.checkout

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentelement.EmbeddedContentPage
import com.stripe.android.paymentelement.EmbeddedFormPage
import com.stripe.android.paymentsheet.MainActivity
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonObject
import kotlin.time.Duration.Companion.seconds

internal fun runCheckoutLiveTest(
    composeRule: ComposeTestRule,
    sessionParams: JsonObject,
    configuration: CheckoutElementsController.Configuration,
    block: CheckoutLiveTestContext.() -> Unit,
) {
    val backend = CheckoutLiveBackend(
        baseUrl = InstrumentationRegistry.getArguments().getString("checkoutBackendUrl")
            ?: "https://stp-mobile-playground-backend-v7.stripedemos.com/",
    )
    val (publishableKey, clientSecret) = runBlocking {
        withTimeout(OPERATION_TIMEOUT) {
            backend.fetchPublishableKey() to backend.createCheckoutSession(sessionParams)
        }
    }
    val checkoutResult = CompletableDeferred<CheckoutElementsController.Result>()

    ActivityScenario.launch(MainActivity::class.java).use { scenario ->
        scenario.moveToState(Lifecycle.State.CREATED)
        PaymentConfiguration.init(ApplicationProvider.getApplicationContext(), publishableKey)

        lateinit var controller: CheckoutElementsController
        scenario.onActivity {
            controller = CheckoutElementsController.Builder(
                application = ApplicationProvider.getApplicationContext(),
                savedStateHandle = SavedStateHandle(),
            ).resultCallback { checkoutResult.complete(it) }.build()
        }
        try {
            val configureResult = runBlocking {
                withTimeout(OPERATION_TIMEOUT) {
                    controller.configure(clientSecret, configuration)
                }
            }
            assertThat(configureResult).isInstanceOf(CheckoutElementsController.OperationResult.Succeeded::class.java)

            lateinit var presenter: CheckoutElementsPresenter
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

            CheckoutLiveTestContext(
                composeRule = composeRule,
                presenter = presenter,
                checkoutResult = checkoutResult,
            ).block()
        } finally {
            scenario.onActivity { controller.destroy() }
        }
    }
}

internal class CheckoutLiveTestContext(
    composeRule: ComposeTestRule,
    private val presenter: CheckoutElementsPresenter,
    private val checkoutResult: CompletableDeferred<CheckoutElementsController.Result>,
) {
    val contentPage = EmbeddedContentPage(composeRule)
    val formPage = EmbeddedFormPage(composeRule)

    fun confirmAndAwaitResult(): CheckoutElementsController.Result {
        presenter.confirm()
        return runBlocking {
            withTimeout(OPERATION_TIMEOUT) { checkoutResult.await() }
        }
    }
}

private val OPERATION_TIMEOUT = 60.seconds
