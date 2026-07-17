package com.stripe.android.checkout

import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.testing.TestLifecycleOwner
import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.confirmation.asCallbackFor
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedRowSelectionImmediateActionHandler
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
import com.stripe.android.paymentelement.embedded.EmbeddedActivityResult
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.content.EmbeddedConfirmationStateFixtures
import com.stripe.android.paymentelement.embedded.content.SheetStateHolder
import com.stripe.android.paymentelement.embedded.sheet.EmbeddedSheetContract
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.DefaultCustomerStateHolder
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.createCustomerState
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.testing.DummyActivityResultCaller
import com.stripe.android.testing.DummyActivityResultCaller.RegisterCall
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class CheckoutSheetLauncherTest {

    @Test
    fun `launchPaymentOptions launches activity with correct parameters and opens the sheet`() = testScenario {
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create()
        val state = EmbeddedConfirmationStateFixtures.defaultState()
        val selection = PaymentSelection.GooglePay
        val customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE
        val expectedArgs = EmbeddedActivityArgs(
            paymentMethodMetadata = paymentMethodMetadata,
            configuration = state.configuration,
            paymentElementCallbackIdentifier = CALLBACK_IDENTIFIER,
            statusBarColor = null,
            selection = selection,
            customerState = customerState,
            promotion = null,
            launchMode = EmbeddedLaunchMode.PaymentOptions,
        )

        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
        sheetLauncher.launchPaymentOptions(
            paymentMethodMetadata = paymentMethodMetadata,
            customerState = customerState,
            selection = selection,
            embeddedConfirmationState = state,
        )

        assertThat(dummyActivityResultCallerScenario.awaitLaunchCall()).isEqualTo(expectedArgs)
        assertThat(sheetStateHolder.sheetIsOpen).isTrue()
    }

    @Test
    fun `launchForm launches activity with correct parameters and sets temporary selection`() = testScenario {
        val code = "test_code"
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create()
        val state = EmbeddedConfirmationStateFixtures.defaultState()

        sheetLauncher.launchForm(
            code = code,
            paymentMethodMetadata = paymentMethodMetadata,
            embeddedConfirmationState = state,
            customerState = createCustomerState(),
            promotion = null,
        )

        val launchCall = dummyActivityResultCallerScenario.awaitLaunchCall() as EmbeddedActivityArgs
        assertThat(launchCall.launchMode).isEqualTo(EmbeddedLaunchMode.Form(selectedPaymentMethodCode = code))
        assertThat(sheetStateHolder.sheetIsOpen).isTrue()
        assertThat(selectionHolder.temporarySelection.value).isEqualTo(code)
    }

    @Test
    fun `launch logs error and does not open the sheet when confirmation state is null`() = testScenario {
        sheetLauncher.launchPaymentOptions(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            customerState = null,
            selection = null,
            embeddedConfirmationState = null,
        )

        val loggedErrors = errorReporter.getLoggedErrors()
        assertThat(loggedErrors.size).isEqualTo(1)
        assertThat(loggedErrors.first())
            .isEqualTo("unexpected_error.embedded.embedded_sheet_launcher.embedded_state_is_null")
        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
    }

    @Test
    fun `paymentOptions complete result writes selection and customer state back and closes the sheet`() =
        testScenario {
            sheetStateHolder.sheetIsOpen = true
            val customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE
            val selection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
            val result = EmbeddedActivityResult.Complete(
                customerState = customerState,
                selection = selection,
                hasBeenConfirmed = false,
                shouldInvokeSelectionCallback = false,
                launchMode = EmbeddedLaunchMode.PaymentOptions,
            )

            registerCall.callback.asCallbackFor<EmbeddedActivityResult>().onActivityResult(result)

            assertThat(customerStateHolder.customer.value).isEqualTo(customerState)
            assertThat(selectionHolder.selection.value).isEqualTo(selection)
            assertThat(sheetStateHolder.sheetIsOpen).isFalse()
        }

    @Test
    fun `paymentOptions cancelled result clears a stale saved selection`() = testScenario {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        selectionHolder.setSelection(PaymentSelection.Saved(paymentMethod))
        customerStateHolder.setCustomerState(createCustomerState(paymentMethods = listOf(paymentMethod)))

        sheetStateHolder.sheetIsOpen = true
        val result = EmbeddedActivityResult.Cancelled(
            customerState = createCustomerState(paymentMethods = emptyList()),
            launchMode = EmbeddedLaunchMode.PaymentOptions,
        )
        registerCall.callback.asCallbackFor<EmbeddedActivityResult>().onActivityResult(result)

        assertThat(selectionHolder.selection.value).isNull()
        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
    }

    @Test
    fun `onDestroy unregisters the launcher`() = testScenario {
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        val unregisteredLauncher = dummyActivityResultCallerScenario.awaitNextUnregisteredLauncher()

        assertThat(unregisteredLauncher).isEqualTo(launcher)
    }

    private fun testScenario(
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val lifecycleOwner = TestLifecycleOwner()
        val savedStateHandle = SavedStateHandle()
        val selectionHolder = DefaultEmbeddedSelectionHolder(savedStateHandle)
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create()
        val customerStateHolder = DefaultCustomerStateHolder(
            savedStateHandle = savedStateHandle,
            selection = selectionHolder.selection,
            customerMetadata = stateFlowOf(paymentMethodMetadata.customerMetadata),
            paymentMethodMetadataFlow = stateFlowOf(null),
        )
        val sheetStateHolder = SheetStateHolder(savedStateHandle)
        val errorReporter = FakeErrorReporter()
        val immediateActionHandler = DefaultEmbeddedRowSelectionImmediateActionHandler(
            coroutineScope = CoroutineScope(UnconfinedTestDispatcher()),
            internalRowSelectionCallback = { null },
        )

        DummyActivityResultCaller.test {
            val sheetLauncher = CheckoutSheetLauncher(
                activityResultCaller = activityResultCaller,
                lifecycleOwner = lifecycleOwner,
                selectionHolder = selectionHolder,
                rowSelectionImmediateActionHandler = immediateActionHandler,
                customerStateHolder = customerStateHolder,
                sheetStateHolder = sheetStateHolder,
                errorReporter = errorReporter,
                statusBarColor = null,
                paymentElementCallbackIdentifier = CALLBACK_IDENTIFIER,
            )
            val registerCall = awaitRegisterCall()
            val launcher = awaitNextRegisteredLauncher()

            assertThat(registerCall.contract).isInstanceOf<EmbeddedSheetContract>()

            Scenario(
                selectionHolder = selectionHolder,
                lifecycleOwner = lifecycleOwner,
                customerStateHolder = customerStateHolder,
                dummyActivityResultCallerScenario = this,
                registerCall = registerCall,
                launcher = launcher,
                sheetLauncher = sheetLauncher,
                sheetStateHolder = sheetStateHolder,
                errorReporter = errorReporter,
            ).block()
        }
    }

    private class Scenario(
        val selectionHolder: EmbeddedSelectionHolder,
        val lifecycleOwner: TestLifecycleOwner,
        val customerStateHolder: CustomerStateHolder,
        val dummyActivityResultCallerScenario: DummyActivityResultCaller.Scenario,
        val registerCall: RegisterCall<*, *>,
        val launcher: ActivityResultLauncher<*>,
        val sheetLauncher: CheckoutSheetLauncher,
        val sheetStateHolder: SheetStateHolder,
        val errorReporter: FakeErrorReporter,
    )

    private companion object {
        const val CALLBACK_IDENTIFIER = "CheckoutSheetLauncherTestIdentifier"
    }
}
