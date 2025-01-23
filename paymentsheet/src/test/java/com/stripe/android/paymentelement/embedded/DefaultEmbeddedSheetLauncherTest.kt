package com.stripe.android.paymentelement.embedded

import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.testing.TestLifecycleOwner
import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.confirmation.asCallbackFor
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
        val expectedArgs = FormContract.Args(code, paymentMethodMetadata)

        it.sheetLauncher.launchForm(code, paymentMethodMetadata)
        val launchCall = awaitLaunchCall()
        assertThat(launchCall).isEqualTo(expectedArgs)
    }

    @Test
    fun `formActivityLauncher callback updates selection holder on complete result`() = testScenario {
        val selection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION
        val result = FormResult.Complete(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
        val callback = it.formRegisterCall.callback.asCallbackFor<FormResult>()

        callback.onActivityResult(result)
        assertThat(it.selectionHolder.selection.value).isEqualTo(selection)
    }

    @Test
    fun `formActivityLauncher callback does not update selection holder on non-complete result`() = testScenario {
        val result = FormResult.Cancelled
        val callback = it.formRegisterCall.callback.asCallbackFor<FormResult>()

        callback.onActivityResult(result)
        assertThat(it.selectionHolder.selection.value).isEqualTo(null)
    }

    @Test
    fun `launchManage launches activity with correct parameters`() = testScenario {
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create()
        val customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE
        val expectedArgs = ManageContract.Args(paymentMethodMetadata, customerState, PaymentSelection.GooglePay)

        it.sheetLauncher.launchManage(paymentMethodMetadata, customerState, PaymentSelection.GooglePay)
        val launchCall = awaitLaunchCall()

        assertThat(launchCall).isEqualTo(expectedArgs)
    }

    @Test
    fun `manageActivityLauncher callback updates state on complete result`() = testScenario {
        val customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE
        val selection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        val result = ManageResult.Complete(
            customerState,
            selection,
        )

        val callback = it.manageRegisterCall.callback.asCallbackFor<ManageResult>()
        callback.onActivityResult(result)

        assertThat(it.customerStateHolder.customer.value).isEqualTo(customerState)
        assertThat(it.selectionHolder.selection.value).isEqualTo(selection)
    }

    @Test
    fun `manageActivityLauncher callback does not update state on non-complete result`() = testScenario {
        it.customerStateHolder.setCustomerState(PaymentSheetFixtures.EMPTY_CUSTOMER_STATE)
        val result = ManageResult.Cancelled(customerState = null)
        val callback = it.manageRegisterCall.callback.asCallbackFor<ManageResult>()

        callback.onActivityResult(result)

        assertThat(it.customerStateHolder.customer.value).isEqualTo(PaymentSheetFixtures.EMPTY_CUSTOMER_STATE)
        assertThat(it.selectionHolder.selection.value).isEqualTo(null)
    }

    @Test
    fun `manageActivityLauncher callback updates state on non-complete result`() = testScenario {
        val result = ManageResult.Cancelled(customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE)
        val callback = it.manageRegisterCall.callback.asCallbackFor<ManageResult>()
        callback.onActivityResult(result)

        assertThat(it.customerStateHolder.customer.value).isEqualTo(PaymentSheetFixtures.EMPTY_CUSTOMER_STATE)
    }

    @Test
    fun `onDestroy unregisters launchers`() = testScenario {
        it.lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        val formUnregisterCall = awaitUnregisterCall()
        val manageUnregisterCall = awaitUnregisterCall()

        assertThat(formUnregisterCall).isNotNull()
        assertThat(manageUnregisterCall).isNotNull()
    }

    private fun testScenario(
        activityResultCallerBlock: suspend DummyActivityResultCaller.Scenario.(launcherScenario: Scenario) -> Unit
    ) = runTest {
        val lifecycleOwner = TestLifecycleOwner()
        val savedStateHandle = SavedStateHandle()
        val selectionHolder = EmbeddedSelectionHolder(savedStateHandle)
        val customerStateHolder = CustomerStateHolder(savedStateHandle, selectionHolder.selection)

        DummyActivityResultCaller.test {
            val sheetLauncher = DefaultEmbeddedSheetLauncher(
                activityResultCaller = activityResultCaller,
                lifecycleOwner = lifecycleOwner,
                selectionHolder = selectionHolder,
                customerStateHolder = customerStateHolder
            )
            val formRegisterCall = awaitRegisterCall()
            val manageRegisterCall = awaitRegisterCall()
            val formLauncher = awaitNextRegisteredLauncher()
            val manageLauncher = awaitNextRegisteredLauncher()

            assertThat(formRegisterCall).isNotNull()
            assertThat(manageRegisterCall).isNotNull()

            assertThat(formRegisterCall.contract).isInstanceOf<FormContract>()
            assertThat(formRegisterCall.callback).isInstanceOf<ActivityResultCallback<*>>()

            assertThat(manageRegisterCall.contract).isInstanceOf<ManageContract>()
            assertThat(manageRegisterCall.callback).isInstanceOf<ActivityResultCallback<*>>()

            assertThat(formLauncher).isInstanceOf<ActivityResultLauncher<FormContract.Args>>()
            assertThat(manageLauncher).isInstanceOf<ActivityResultLauncher<ManageContract.Args>>()

            activityResultCallerBlock.invoke(
                this,
                Scenario(
                    selectionHolder = selectionHolder,
                    lifecycleOwner = lifecycleOwner,
                    customerStateHolder = customerStateHolder,
                    formRegisterCall = formRegisterCall,
                    manageRegisterCall = manageRegisterCall,
                    sheetLauncher = sheetLauncher
                )
            )
        }
    }

    private class Scenario(
        val selectionHolder: EmbeddedSelectionHolder,
        val lifecycleOwner: TestLifecycleOwner,
        val customerStateHolder: CustomerStateHolder,
        val formRegisterCall: RegisterCall<*, *>,
        val manageRegisterCall: RegisterCall<*, *>,
        val sheetLauncher: EmbeddedSheetLauncher,
    )
}
