@file:OptIn(PaymentMethodMessagingElementPreview::class)

package com.stripe.android.paymentmethodmessaging.element

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import com.stripe.android.PaymentConfiguration
import com.stripe.android.networktesting.NetworkRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

internal class PaymentMethodMessagingElementTestRunnerContext(
    val paymentMethodMessagingElement: PaymentMethodMessagingElement
) {
    suspend fun configure(
        configuration: PaymentMethodMessagingElement.Configuration
    ) {
        paymentMethodMessagingElement.configure(configuration)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
internal fun runPaymentMethodMessagingElementTest(
    networkRule: NetworkRule,
    block: suspend (PaymentMethodMessagingElementTestRunnerContext) -> Unit
) {
    val factory: (ComponentActivity) -> PaymentMethodMessagingElement = {
        lateinit var element: PaymentMethodMessagingElement
        it.setContent {
            element = PaymentMethodMessagingElement.create(it.application)
            Column {
                element.Content()
            }
        }
        element
    }

    ActivityScenario.launch(MainActivity::class.java).use { scenario ->
        scenario.moveToState(Lifecycle.State.CREATED)

        lateinit var paymentMethodMessagingElement: PaymentMethodMessagingElement
        scenario.onActivity {
            PaymentConfiguration.init(it, "pk_test_123")
            paymentMethodMessagingElement = factory(it)
        }

        scenario.moveToState(Lifecycle.State.RESUMED)

        val testContext = PaymentMethodMessagingElementTestRunnerContext(
            paymentMethodMessagingElement = paymentMethodMessagingElement
        )

        runTest {
            block(testContext)
        }

        networkRule.validate()
    }
}
