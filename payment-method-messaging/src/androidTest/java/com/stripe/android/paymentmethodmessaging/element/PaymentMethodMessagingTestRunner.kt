@file:OptIn(PaymentMethodMessagingElementPreview::class)

package com.stripe.android.paymentmethodmessaging.element

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.PaymentMethod
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

internal class PaymentMethodMessagingElementTestRunnerContext(
    val paymentMethodMessagingElement: PaymentMethodMessagingElement
) {
    suspend fun configure(): PaymentMethodMessagingElement.ConfigureResult {
        return paymentMethodMessagingElement.configure(
            PaymentMethodMessagingElement.Configuration()
                .amount(5000)
                .currency("usd")
                .locale("en")
                .countryCode("US")
                .paymentMethodTypes(
                    listOf(PaymentMethod.Type.Klarna, PaymentMethod.Type.Affirm, PaymentMethod.Type.AfterpayClearpay)
                )
        )
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
internal fun runPaymentMethodMessagingElementTest(
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
    }
}
