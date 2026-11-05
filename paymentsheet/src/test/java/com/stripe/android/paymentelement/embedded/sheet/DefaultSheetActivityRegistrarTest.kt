package com.stripe.android.paymentelement.embedded.sheet

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.testing.TestLifecycleOwner
import app.cash.turbine.ReceiveTurbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.taptoadd.FakeTapToAddHelper
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentsheet.addresselement.TestAutocompleteLauncher
import com.stripe.android.testing.CoroutineTestRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock

internal class DefaultSheetActivityRegistrarTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `registerAndBootstrap registers and bootstraps handlers`() = testScenario {
        registrar.registerAndBootstrap(
            activityResultCaller = activityResultCaller,
            lifecycleOwner = lifecycleOwner,
        )

        with(confirmationHandler.registerTurbine.awaitItem()) {
            assertThat(activityResultCaller).isEqualTo(activityResultCaller)
            assertThat(lifecycleOwner).isEqualTo(lifecycleOwner)
        }
        with(tapToAddHelper.registerCalls.awaitItem()) {
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
        )
        registrar.registerAndBootstrap(
            activityResultCaller = activityResultCaller,
            lifecycleOwner = lifecycleOwner,
        )

        assertThat(confirmationHandler.registerTurbine.awaitItem()).isNotNull()
        assertThat(confirmationHandler.registerTurbine.awaitItem()).isNotNull()
        assertThat(tapToAddHelper.registerCalls.awaitItem()).isNotNull()
        assertThat(tapToAddHelper.registerCalls.awaitItem()).isNotNull()

        confirmationHandler.bootstrapTurbine.awaitItem()
    }

    @Test
    fun `FlowController registers Link and autocomplete launchers without bootstrapping`() = testScenario(
        launchMode = EmbeddedLaunchMode.PaymentOptions(
            flowController = EmbeddedLaunchMode.FlowControllerOptions(
                visibleWallets = emptyList(),
                googlePlacesApiKey = null,
            ),
        ),
    ) {
        registrar.registerAndBootstrap(
            activityResultCaller = activityResultCaller,
            lifecycleOwner = lifecycleOwner,
        )

        assertThat(confirmationHandler.registerTurbine.awaitItem()).isNotNull()
        assertThat(tapToAddHelper.registerCalls.awaitItem()).isNotNull()
        assertThat(linkLauncher.registerCalls.awaitItem()).isEqualTo(
            FakeSheetLinkPaymentMethodLauncher.RegisterCall(activityResultCaller, lifecycleOwner)
        )
        with(autocompleteRegisterCalls.awaitItem()) {
            assertThat(activityResultCaller).isEqualTo(this@testScenario.activityResultCaller)
            assertThat(lifecycleOwner).isEqualTo(this@testScenario.lifecycleOwner)
        }
        // FlowController's own confirmation handler is already bootstrapped.
        confirmationHandler.bootstrapTurbine.expectNoEvents()
    }

    private fun testScenario(
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        launchMode: EmbeddedLaunchMode = EmbeddedLaunchMode.Form(selectedPaymentMethodCode = "card"),
        block: suspend Scenario.() -> Unit
    ) = runTest {
        TestAutocompleteLauncher.test {
            val confirmationHandler = FakeConfirmationHandler()
            val tapToAddHelper = FakeTapToAddHelper()
            val linkLauncher = FakeSheetLinkPaymentMethodLauncher()
            val activityResultCaller = mock<ActivityResultCaller>()
            val lifecycleOwner = TestLifecycleOwner(coroutineDispatcher = Dispatchers.Unconfined)

            val registrar = DefaultSheetActivityRegistrar(
                confirmationHandler = confirmationHandler,
                tapToAddHelper = tapToAddHelper,
                paymentMethodMetadata = paymentMethodMetadata,
                launchMode = launchMode,
                linkLauncher = { linkLauncher },
                autocompleteLauncher = { launcher },
            )

            val scenario = Scenario(
                registrar = registrar,
                confirmationHandler = confirmationHandler,
                tapToAddHelper = tapToAddHelper,
                linkLauncher = linkLauncher,
                autocompleteRegisterCalls = registerCalls,
                activityResultCaller = activityResultCaller,
                lifecycleOwner = lifecycleOwner,
                paymentMethodMetadata = paymentMethodMetadata
            )

            block(scenario)
            confirmationHandler.validate()
            tapToAddHelper.validate()
            linkLauncher.validate()
        }
    }

    private class Scenario(
        val registrar: DefaultSheetActivityRegistrar,
        val confirmationHandler: FakeConfirmationHandler,
        val tapToAddHelper: FakeTapToAddHelper,
        val linkLauncher: FakeSheetLinkPaymentMethodLauncher,
        val autocompleteRegisterCalls: ReceiveTurbine<TestAutocompleteLauncher.RegisterCall>,
        val activityResultCaller: ActivityResultCaller,
        val lifecycleOwner: LifecycleOwner,
        val paymentMethodMetadata: PaymentMethodMetadata,
    )
}
