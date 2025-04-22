@file:OptIn(ExperimentalEmbeddedPaymentElementApi::class)

package com.stripe.android.paymentelement

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.link.account.LinkStore
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.MainActivity
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.coroutines.test.runTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

internal class EmbeddedPaymentElementTestRunnerContext(
    val embeddedPaymentElement: EmbeddedPaymentElement,
    private val countDownLatch: CountDownLatch,
) {
    suspend fun configure(
        configurationMutator: EmbeddedPaymentElement.Configuration.Builder.() -> EmbeddedPaymentElement.Configuration.Builder = { this },
    ) {
        embeddedPaymentElement.configure(
            intentConfiguration = PaymentSheet.IntentConfiguration(
                mode = PaymentSheet.IntentConfiguration.Mode.Payment(amount = 5000, currency = "USD")
            ),
            configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.")
                .configurationMutator()
                .build()
        )
    }

    fun confirm() {
        embeddedPaymentElement.confirm()
    }

    /**
     * Normally we know a test succeeds when it calls [EmbeddedPaymentElement.ResultCallback], but some tests
     * succeed based on other criteria. In these cases, call this method to manually mark a test as
     * succeeded.
     */
    fun markTestSucceeded() {
        countDownLatch.countDown()
    }
}

@OptIn(ExperimentalAnalyticEventCallbackApi::class)
internal fun runEmbeddedPaymentElementTest(
    networkRule: NetworkRule,
    createIntentCallback: CreateIntentCallback,
    resultCallback: EmbeddedPaymentElement.ResultCallback,
    analyticEventCallback: AnalyticEventCallback? = null,
    successTimeoutSeconds: Long = 5L,
    block: suspend (EmbeddedPaymentElementTestRunnerContext) -> Unit,
) {
    val countDownLatch = CountDownLatch(1)

    val factory: (ComponentActivity) -> EmbeddedPaymentElement = {
        lateinit var embeddedPaymentElement: EmbeddedPaymentElement
        val builder = EmbeddedPaymentElement.Builder(
            createIntentCallback = createIntentCallback,
            resultCallback = { result ->
                resultCallback.onResult(result)
                countDownLatch.countDown()
            },
        ).analyticEventCallback { event ->
            analyticEventCallback?.onEvent(event)
        }
        it.setContent {
            embeddedPaymentElement = rememberEmbeddedPaymentElement(builder)
            val scrollState = rememberScrollState()
            Box(modifier = Modifier.verticalScroll(scrollState)) {
                embeddedPaymentElement.Content()
            }
        }
        embeddedPaymentElement
    }

    runEmbeddedPaymentElementTestInternal(
        networkRule = networkRule,
        countDownLatch = countDownLatch,
        countDownLatchTimeoutSeconds = successTimeoutSeconds,
        makeEmbeddedPaymentElement = factory,
        block = block,
    )
}

private fun runEmbeddedPaymentElementTestInternal(
    networkRule: NetworkRule,
    countDownLatch: CountDownLatch,
    countDownLatchTimeoutSeconds: Long,
    makeEmbeddedPaymentElement: (ComponentActivity) -> EmbeddedPaymentElement,
    block: suspend (EmbeddedPaymentElementTestRunnerContext) -> Unit,
) {
    ActivityScenario.launch(MainActivity::class.java).use { scenario ->
        scenario.moveToState(Lifecycle.State.CREATED)
        scenario.onActivity {
            PaymentConfiguration.init(it, "pk_test_123")
            LinkStore(it.applicationContext).clear()
        }

        lateinit var embeddedPaymentElement: EmbeddedPaymentElement
        scenario.onActivity {
            embeddedPaymentElement = makeEmbeddedPaymentElement(it)
        }

        scenario.moveToState(Lifecycle.State.RESUMED)

        val testContext = EmbeddedPaymentElementTestRunnerContext(
            embeddedPaymentElement = embeddedPaymentElement,
            countDownLatch = countDownLatch,
        )
        runTest {
            block(testContext)
        }

        val didCompleteSuccessfully = countDownLatch.await(countDownLatchTimeoutSeconds, TimeUnit.SECONDS)
        networkRule.validate()
        assertThat(didCompleteSuccessfully).isTrue()
    }
}
