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
import com.stripe.android.paymentsheet.MainActivity
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonObject
import kotlin.time.Duration.Companion.seconds

internal suspend fun runCheckoutIntegrationTest(
    backend: CheckoutPlaygroundBackend,
    sessionParams: JsonObject,
    configuration: CheckoutElementsController.Configuration,
    setup: suspend (CheckoutElementsController) -> Unit,
    handleNextAction: suspend () -> Unit,
): CheckoutElementsController.Result {
    val publishableKey = backend.fetchPublishableKey().getOrThrow()
    val clientSecret = backend.createCheckoutSession(sessionParams).getOrThrow()
    val checkoutResult = CompletableDeferred<CheckoutElementsController.Result>()

    return ActivityScenario.launch(MainActivity::class.java).use { scenario ->
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
            val configureResult = controller.configure(clientSecret, configuration)
            assertThat(configureResult).isInstanceOf(CheckoutElementsController.OperationResult.Succeeded::class.java)

            lateinit var presenter: CheckoutElementsPresenter
            scenario.onActivity { activity ->
                presenter = controller.createPresenter(activity)
                val currencySelectorElement = presenter.currencySelectorElement()
                    .takeIf { configuration.build().currencySelectorElementConfiguration != null }
                val paymentElement = presenter.paymentElement()
                activity.setContent {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        currencySelectorElement?.Content()
                        paymentElement.Content()
                    }
                }
            }
            scenario.moveToState(Lifecycle.State.RESUMED)

            setup(controller)

            presenter.confirm()
            handleNextAction()
            withTimeout(OPERATION_TIMEOUT) { checkoutResult.await() }
        } finally {
            scenario.onActivity { controller.destroy() }
        }
    }
}

internal suspend fun CheckoutElementsController.awaitSession(
    description: String,
    predicate: (CheckoutElementsController.Session) -> Boolean,
): CheckoutElementsController.Session {
    return withTimeoutOrNull(OPERATION_TIMEOUT) {
        session.filterNotNull().first(predicate)
    } ?: error("Timed out waiting for $description. Last session: ${session.value}")
}

private val OPERATION_TIMEOUT = 30.seconds
