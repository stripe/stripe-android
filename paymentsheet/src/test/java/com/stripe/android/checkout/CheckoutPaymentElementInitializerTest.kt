package com.stripe.android.checkout

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import kotlinx.coroutines.test.runTest
import org.mockito.kotlin.mock
import kotlin.test.Test

internal class CheckoutPaymentElementInitializerTest {

    @Test
    fun `initialize registers the confirmation handler with the caller and lifecycle owner`() = runScenario {
        initializer.initialize()

        val registerCall = confirmationHandler.registerTurbine.awaitItem()
        assertThat(registerCall.activityResultCaller).isSameInstanceAs(activityResultCaller)
        assertThat(registerCall.lifecycleOwner).isSameInstanceAs(lifecycleOwner)
    }

    private fun runScenario(
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val confirmationHandler = FakeConfirmationHandler()
        val activityResultCaller = mock<ActivityResultCaller>()
        val lifecycleOwner = mock<LifecycleOwner>()
        val initializer = CheckoutPaymentElementInitializer(
            confirmationHandler = confirmationHandler,
            activityResultCaller = activityResultCaller,
            lifecycleOwner = lifecycleOwner,
        )

        Scenario(
            initializer = initializer,
            confirmationHandler = confirmationHandler,
            activityResultCaller = activityResultCaller,
            lifecycleOwner = lifecycleOwner,
        ).block()

        confirmationHandler.validate()
    }

    private class Scenario(
        val initializer: CheckoutPaymentElementInitializer,
        val confirmationHandler: FakeConfirmationHandler,
        val activityResultCaller: ActivityResultCaller,
        val lifecycleOwner: LifecycleOwner,
    )
}
