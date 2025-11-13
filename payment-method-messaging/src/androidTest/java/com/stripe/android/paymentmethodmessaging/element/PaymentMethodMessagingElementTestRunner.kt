@file:OptIn(PaymentMethodMessagingElementPreview::class)

package com.stripe.android.paymentmethodmessaging.element

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
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
    val application = ApplicationProvider.getApplicationContext<Application>()
    PaymentConfiguration.init(application, "pk_test_123")
    val element = PaymentMethodMessagingElement.create(application)
    val factory: (ComponentActivity) -> PaymentMethodMessagingElement = {
        it.setContent {
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
