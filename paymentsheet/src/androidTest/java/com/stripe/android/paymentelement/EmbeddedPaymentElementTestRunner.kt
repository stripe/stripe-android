package com.stripe.android.paymentelement

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.elements.payment.IntentConfiguration
import com.stripe.android.link.account.LinkStore
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.MainActivity
import kotlinx.coroutines.test.runTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

internal class EmbeddedPaymentElementTestRunnerContext(
    val embeddedPaymentElement: EmbeddedPaymentElement,
    val rowSelectionCalls: ReceiveTurbine<RowSelectionCall>,
    private val countDownLatch: CountDownLatch,
) {
    suspend fun configure(
        configurationMutator: EmbeddedPaymentElement.Configuration.Builder.() -> EmbeddedPaymentElement.Configuration.Builder = { this },
    ) {
        embeddedPaymentElement.configure(
            intentConfiguration = IntentConfiguration(
                mode = IntentConfiguration.Mode.Payment(amount = 5000, currency = "USD")
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

internal fun runEmbeddedPaymentElementTest(
    networkRule: NetworkRule,
    createIntentCallback: CreateIntentCallback,
    resultCallback: EmbeddedPaymentElement.ResultCallback,
    builder: EmbeddedPaymentElement.Builder.() -> Unit = {},
    successTimeoutSeconds: Long = 5L,
    showWalletButtons: Boolean = false,
    rowSelectionCalls: ReceiveTurbine<RowSelectionCall> = Turbine(),
    block: suspend (EmbeddedPaymentElementTestRunnerContext) -> Unit,
) {
    runEmbeddedPaymentElementTest(
        networkRule = networkRule,
        builderInstance = EmbeddedPaymentElement.Builder(
            createIntentCallback = createIntentCallback,
            resultCallback = resultCallback,
        ),
        builder = builder,
        successTimeoutSeconds = successTimeoutSeconds,
        showWalletButtons = showWalletButtons,
        rowSelectionCalls = rowSelectionCalls,
        block = block,
    )
}

@OptIn(WalletButtonsPreview::class, SharedPaymentTokenSessionPreview::class)
internal fun runEmbeddedPaymentElementTest(
    networkRule: NetworkRule,
    builderInstance: EmbeddedPaymentElement.Builder,
    builder: EmbeddedPaymentElement.Builder.() -> Unit = {},
    successTimeoutSeconds: Long = 5L,
    showWalletButtons: Boolean = false,
    rowSelectionCalls: ReceiveTurbine<RowSelectionCall> = Turbine(),
    block: suspend (EmbeddedPaymentElementTestRunnerContext) -> Unit,
) {
    val countDownLatch = CountDownLatch(1)

    val factory: (ComponentActivity) -> EmbeddedPaymentElement = {
        lateinit var embeddedPaymentElement: EmbeddedPaymentElement
        val embeddedPaymentElementBuilderInstance = when (builderInstance.deferredHandler) {
            is EmbeddedPaymentElement.Builder.DeferredHandler.Intent -> {
                EmbeddedPaymentElement.Builder(
                    resultCallback = { result ->
                        builderInstance.resultCallback.onResult(result)
                        countDownLatch.countDown()
                    },
                    createIntentCallback = builderInstance.deferredHandler.createIntentCallback,
                )
            }
            is EmbeddedPaymentElement.Builder.DeferredHandler.SharedPaymentToken -> {
                EmbeddedPaymentElement.Builder(
                    resultCallback = { result ->
                        builderInstance.resultCallback.onResult(result)
                        countDownLatch.countDown()
                    },
                    preparePaymentMethodHandler = builderInstance.deferredHandler.preparePaymentMethodHandler,
                )
            }
        }

        val embeddedPaymentElementBuilder = embeddedPaymentElementBuilderInstance.apply {
            builder()
        }
        it.setContent {
            embeddedPaymentElement = rememberEmbeddedPaymentElement(embeddedPaymentElementBuilder)
            val scrollState = rememberScrollState()
            Column(modifier = Modifier.verticalScroll(scrollState)) {
                if (showWalletButtons) {
                    embeddedPaymentElement.WalletButtons()
                }

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
        rowSelectionCalls = rowSelectionCalls,
        block = block,
    )
}

private fun runEmbeddedPaymentElementTestInternal(
    networkRule: NetworkRule,
    countDownLatch: CountDownLatch,
    countDownLatchTimeoutSeconds: Long,
    makeEmbeddedPaymentElement: (ComponentActivity) -> EmbeddedPaymentElement,
    rowSelectionCalls: ReceiveTurbine<RowSelectionCall>,
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
            rowSelectionCalls = rowSelectionCalls,
            countDownLatch = countDownLatch,
        )
        runTest {
            block(testContext)
        }

        testContext.rowSelectionCalls.ensureAllEventsConsumed()
        val didCompleteSuccessfully = countDownLatch.await(countDownLatchTimeoutSeconds, TimeUnit.SECONDS)
        networkRule.validate()
        assertThat(didCompleteSuccessfully).isTrue()
    }
}

data class RowSelectionCall(
    val paymentMethodType: String?,
    val paymentOptionLabel: String?,
)
