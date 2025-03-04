package com.stripe.android.paymentelement.embedded.content

import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.testing.TestLifecycleOwner
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.confirmation.asCallbackFor
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.form.FormContract
import com.stripe.android.paymentelement.embedded.form.FormResult
import com.stripe.android.paymentelement.embedded.manage.ManageContract
import com.stripe.android.paymentelement.embedded.manage.ManageResult
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.utils.DummyActivityResultCaller
import com.stripe.android.utils.DummyActivityResultCaller.RegisterCall
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
@RunWith(RobolectricTestRunner::class)
internal class DefaultEmbeddedSheetLauncherTest {

    @Test
    fun `launchForm launches activity with correct parameters`() = testScenario {
        val code = "test_code"
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create()
        val state = EmbeddedConfirmationStateFixtures.defaultState()
        val expectedArgs = FormContract.Args(
            selectedPaymentMethodCode = code,
            paymentMethodMetadata = paymentMethodMetadata,
            hasSavedPaymentMethods = false,
            configuration = state.configuration,
            initializationMode = state.initializationMode,
            paymentElementCallbackIdentifier = "EmbeddedFormTestIdentifier",
            statusBarColor = null
        )

        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
        assertThat(selectionHolder.temporarySelection.value).isNull()
        sheetLauncher.launchForm(code, paymentMethodMetadata, false, state)
        val launchCall = dummyActivityResultCallerScenario.awaitLaunchCall()
        assertThat(launchCall).isEqualTo(expectedArgs)
        assertThat(sheetStateHolder.sheetIsOpen).isTrue()
        assertThat(selectionHolder.temporarySelection.value).isEqualTo(code)
    }

    @Test
    fun `launchForm logs error and returns if confirmation state is null`() = testScenario {
        val code = "test_code"
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create()

        sheetLauncher.launchForm(code, paymentMethodMetadata, false, null)
        val loggedErrors = errorReporter.getLoggedErrors()
        assertThat(loggedErrors.size).isEqualTo(1)
        assertThat(loggedErrors.first())
            .isEqualTo("unexpected_error.embedded.embedded_sheet_launcher.embedded_state_is_null")
        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
        assertThat(selectionHolder.temporarySelection.value).isNull()
    }

    @Test
    fun `launchForm is not launched again when the sheet is already open`() = testScenario {
        val code = "test_code"
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create()
        val state = EmbeddedConfirmationStateFixtures.defaultState()
        sheetStateHolder.sheetIsOpen = true
        sheetLauncher.launchForm(code, paymentMethodMetadata, false, state)
    }

    @Test
    fun `formActivityLauncher clears selection holder and invokes callback on complete result`() = testScenario {
        sheetStateHolder.sheetIsOpen = true
        selectionHolder.setTemporary("test_code")
        val selection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION
        selectionHolder.set(selection)

        val result = FormResult.Complete(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
        val callback = formRegisterCall.callback.asCallbackFor<FormResult>()

        callback.onActivityResult(result)
        assertThat(selectionHolder.selection.value).isNull()
        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
        assertThat(selectionHolder.temporarySelection.value).isNull()
        assertThat(resultCallbackTurbine.awaitItem()).isInstanceOf<EmbeddedPaymentElement.Result.Completed>()
    }

    @Test
    fun `formActivityLauncher callback does not update selection holder on non-complete result`() = testScenario {
        sheetStateHolder.sheetIsOpen = true
        selectionHolder.setTemporary("test_code")
        val result = FormResult.Cancelled
        val callback = formRegisterCall.callback.asCallbackFor<FormResult>()

        callback.onActivityResult(result)
        assertThat(selectionHolder.selection.value).isEqualTo(null)
        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
        assertThat(selectionHolder.temporarySelection.value).isNull()
        resultCallbackTurbine.expectNoEvents()
    }

    @Test
    fun `launchManage launches activity with correct parameters`() = testScenario {
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create()
        val customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE
        val expectedArgs = ManageContract.Args(paymentMethodMetadata, customerState, PaymentSelection.GooglePay)

        sheetLauncher.launchManage(paymentMethodMetadata, customerState, PaymentSelection.GooglePay)
        val launchCall = dummyActivityResultCallerScenario.awaitLaunchCall()

        assertThat(launchCall).isEqualTo(expectedArgs)
        assertThat(sheetStateHolder.sheetIsOpen).isTrue()
    }

    @Test
    fun `launchManage is not launched again when the sheet is already open`() = testScenario {
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create()
        val customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE
        sheetStateHolder.sheetIsOpen = true
        sheetLauncher.launchManage(paymentMethodMetadata, customerState, PaymentSelection.GooglePay)
    }

    @Test
    fun `manageActivityLauncher callback updates state on complete result`() = testScenario {
        sheetStateHolder.sheetIsOpen = true
        val customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE
        val selection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        val result = ManageResult.Complete(
            customerState,
            selection,
        )

        val callback = manageRegisterCall.callback.asCallbackFor<ManageResult>()
        callback.onActivityResult(result)

        assertThat(customerStateHolder.customer.value).isEqualTo(customerState)
        assertThat(selectionHolder.selection.value).isEqualTo(selection)
        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
    }

    @Test
    fun `manageActivityLauncher callback does not update state on non-complete result`() = testScenario {
        sheetStateHolder.sheetIsOpen = true
        customerStateHolder.setCustomerState(PaymentSheetFixtures.EMPTY_CUSTOMER_STATE)
        val result = ManageResult.Error
        val callback = manageRegisterCall.callback.asCallbackFor<ManageResult>()

        callback.onActivityResult(result)

        assertThat(customerStateHolder.customer.value).isEqualTo(PaymentSheetFixtures.EMPTY_CUSTOMER_STATE)
        assertThat(selectionHolder.selection.value).isEqualTo(null)
        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
    }

    @Test
    fun `onDestroy unregisters launchers`() = testScenario {
        sheetStateHolder.sheetIsOpen = true
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        val formUnregisteredLauncher = dummyActivityResultCallerScenario.awaitNextUnregisteredLauncher()
        val manageUnregisteredLauncher = dummyActivityResultCallerScenario.awaitNextUnregisteredLauncher()

        assertThat(formUnregisteredLauncher).isEqualTo(formLauncher)
        assertThat(manageUnregisteredLauncher).isEqualTo(manageLauncher)
        assertThat(sheetStateHolder.sheetIsOpen).isTrue()
    }

    private fun testScenario(
        block: suspend Scenario.() -> Unit
    ) = runTest {
        val lifecycleOwner = TestLifecycleOwner()
        val savedStateHandle = SavedStateHandle()
        val selectionHolder = EmbeddedSelectionHolder(savedStateHandle)
        val customerStateHolder = CustomerStateHolder(savedStateHandle, selectionHolder.selection)
        val sheetStateHolder = SheetStateHolder(savedStateHandle)
        val errorReporter = FakeErrorReporter()
        val resultCallbackTurbine = Turbine<EmbeddedPaymentElement.Result>()

        DummyActivityResultCaller.test {
            val sheetLauncher = DefaultEmbeddedSheetLauncher(
                activityResultCaller = activityResultCaller,
                lifecycleOwner = lifecycleOwner,
                selectionHolder = selectionHolder,
                customerStateHolder = customerStateHolder,
                sheetStateHolder = sheetStateHolder,
                errorReporter = errorReporter,
                statusBarColor = null,
                paymentElementCallbackIdentifier = "EmbeddedFormTestIdentifier",
                resultCallback = {
                    resultCallbackTurbine.add(it)
                }
            )
            val formRegisterCall = awaitRegisterCall()
            val manageRegisterCall = awaitRegisterCall()

            val formLauncher = awaitNextRegisteredLauncher()
            val manageLauncher = awaitNextRegisteredLauncher()

            assertThat(formRegisterCall).isNotNull()
            assertThat(manageRegisterCall).isNotNull()

            assertThat(formRegisterCall.contract).isInstanceOf<FormContract>()
            assertThat(manageRegisterCall.contract).isInstanceOf<ManageContract>()

            Scenario(
                selectionHolder = selectionHolder,
                lifecycleOwner = lifecycleOwner,
                customerStateHolder = customerStateHolder,
                dummyActivityResultCallerScenario = this,
                formRegisterCall = formRegisterCall,
                manageRegisterCall = manageRegisterCall,
                formLauncher = formLauncher,
                manageLauncher = manageLauncher,
                sheetLauncher = sheetLauncher,
                sheetStateHolder = sheetStateHolder,
                errorReporter = errorReporter,
                resultCallbackTurbine = resultCallbackTurbine
            ).block()

            resultCallbackTurbine.ensureAllEventsConsumed()
        }
    }

    private class Scenario(
        val selectionHolder: EmbeddedSelectionHolder,
        val lifecycleOwner: TestLifecycleOwner,
        val customerStateHolder: CustomerStateHolder,
        val dummyActivityResultCallerScenario: DummyActivityResultCaller.Scenario,
        val formRegisterCall: RegisterCall<*, *>,
        val manageRegisterCall: RegisterCall<*, *>,
        val formLauncher: ActivityResultLauncher<*>,
        val manageLauncher: ActivityResultLauncher<*>,
        val sheetLauncher: EmbeddedSheetLauncher,
        val sheetStateHolder: SheetStateHolder,
        val errorReporter: FakeErrorReporter,
        val resultCallbackTurbine: Turbine<EmbeddedPaymentElement.Result>
    )
}
