package com.stripe.android.paymentelement.embedded

import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.testing.TestLifecycleOwner
import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.confirmation.asCallbackFor
import com.stripe.android.paymentelement.embedded.form.FormContract
import com.stripe.android.paymentelement.embedded.form.FormResult
import com.stripe.android.paymentelement.embedded.manage.ManageContract
import com.stripe.android.paymentelement.embedded.manage.ManageResult
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.utils.DummyActivityResultCaller
import com.stripe.android.utils.DummyActivityResultCaller.RegisterCall
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class DefaultEmbeddedSheetLauncherTest {

    @Test
    fun `launchForm launches activity with correct parameters`() = testScenario {
        val code = "test_code"
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create()
        val expectedArgs = FormContract.Args(code, paymentMethodMetadata, false)

        sheetLauncher.launchForm(code, paymentMethodMetadata, false)
        val launchCall = dummyActivityResultCallerScenario.awaitLaunchCall()
        assertThat(launchCall).isEqualTo(expectedArgs)
        assertThat(sheetStateHolder.sheetIsOpen).isTrue()
    }

    @Test
    fun `formActivityLauncher callback updates selection holder on complete result`() = testScenario {
        sheetStateHolder.sheetIsOpen = true
        val selection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION
        val result = FormResult.Complete(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
        val callback = formRegisterCall.callback.asCallbackFor<FormResult>()

        callback.onActivityResult(result)
        assertThat(selectionHolder.selection.value).isEqualTo(selection)
        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
    }

    @Test
    fun `formActivityLauncher callback does not update selection holder on non-complete result`() = testScenario {
        sheetStateHolder.sheetIsOpen = true
        val result = FormResult.Cancelled
        val callback = formRegisterCall.callback.asCallbackFor<FormResult>()

        callback.onActivityResult(result)
        assertThat(selectionHolder.selection.value).isEqualTo(null)
        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
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
        val result = ManageResult.Cancelled(customerState = null)
        val callback = manageRegisterCall.callback.asCallbackFor<ManageResult>()

        callback.onActivityResult(result)

        assertThat(customerStateHolder.customer.value).isEqualTo(PaymentSheetFixtures.EMPTY_CUSTOMER_STATE)
        assertThat(selectionHolder.selection.value).isEqualTo(null)
        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
    }

    @Test
    fun `manageActivityLauncher callback updates state on non-complete result`() = testScenario {
        sheetStateHolder.sheetIsOpen = true
        val result = ManageResult.Cancelled(customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE)
        val callback = manageRegisterCall.callback.asCallbackFor<ManageResult>()
        callback.onActivityResult(result)

        assertThat(customerStateHolder.customer.value).isEqualTo(PaymentSheetFixtures.EMPTY_CUSTOMER_STATE)
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

        DummyActivityResultCaller.test {
            val sheetLauncher = DefaultEmbeddedSheetLauncher(
                activityResultCaller = activityResultCaller,
                lifecycleOwner = lifecycleOwner,
                selectionHolder = selectionHolder,
                customerStateHolder = customerStateHolder,
                sheetStateHolder = sheetStateHolder,
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
            ).block()
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
    )
}
