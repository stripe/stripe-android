package com.stripe.android.paymentelement.embedded.form

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.testing.TestLifecycleOwner
import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.testing.CoroutineTestRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock

internal class DefaultFormActivityConfirmationHandlerRegistrarTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `registerAndBootstrap registers and bootstraps confirmation handler`() = testScenario {
        registrar.registerAndBootstrap(
            activityResultCaller = activityResultCaller,
            lifecycleOwner = lifecycleOwner,
            paymentMethodMetadata = paymentMethodMetadata
        )

        with(confirmationHandler.registerTurbine.awaitItem()) {
            assertThat(activityResultCaller).isEqualTo(activityResultCaller)
            assertThat(lifecycleOwner).isEqualTo(lifecycleOwner)
        }
        assertThat(confirmationHandler.bootstrapTurbine.awaitItem().paymentMethodMetadata)
            .isEqualTo(paymentMethodMetadata)
    }

    @Test
    fun `registerAndBootstrap only bootstraps once when called multiple times`() = testScenario {
        registrar.registerAndBootstrap(
            activityResultCaller = activityResultCaller,
            lifecycleOwner = lifecycleOwner,
            paymentMethodMetadata = paymentMethodMetadata
        )
        registrar.registerAndBootstrap(
            activityResultCaller = activityResultCaller,
            lifecycleOwner = lifecycleOwner,
            paymentMethodMetadata = paymentMethodMetadata
        )

        confirmationHandler.registerTurbine.awaitItem()
        confirmationHandler.registerTurbine.awaitItem()

        confirmationHandler.bootstrapTurbine.awaitItem()
    }

    private fun testScenario(
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        block: suspend Scenario.() -> Unit
    ) = runTest {
        val confirmationHandler = FakeConfirmationHandler()
        val activityResultCaller = mock<ActivityResultCaller>()
        val lifecycleOwner = TestLifecycleOwner(coroutineDispatcher = Dispatchers.Unconfined)

        val registrar = DefaultFormActivityConfirmationHandlerRegistrar(
            confirmationHandler = confirmationHandler
        )

        val scenario = Scenario(
            registrar = registrar,
            confirmationHandler = confirmationHandler,
            activityResultCaller = activityResultCaller,
            lifecycleOwner = lifecycleOwner,
            paymentMethodMetadata = paymentMethodMetadata
        )

        block(scenario)
        confirmationHandler.validate()
    }

    private class Scenario(
        val registrar: DefaultFormActivityConfirmationHandlerRegistrar,
        val confirmationHandler: FakeConfirmationHandler,
        val activityResultCaller: ActivityResultCaller,
        val lifecycleOwner: LifecycleOwner,
        val paymentMethodMetadata: PaymentMethodMetadata,
    )
}
