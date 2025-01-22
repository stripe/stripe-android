package com.stripe.android.paymentelement.embedded

import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.testing.TestLifecycleOwner
import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.embedded.manage.ManageContract
import com.stripe.android.paymentelement.embedded.manage.ManageResult
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.capture
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class DefaultEmbeddedSheetLauncherTest {

    @Test
    fun `launchForm launches activity with correct parameters`() = testScenario {
        val code = "test_code"
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create()
        val expectedArgs = FormContract.Args(code, paymentMethodMetadata)
        launcher.launchForm(code, paymentMethodMetadata)
        val launchState = formActivityLauncher.launchTurbine.awaitItem()
        assertThat(launchState.didLaunch).isTrue()
        assertThat(launchState.launchArgs).isEqualTo(expectedArgs)
    }

    @Test
    fun `formActivityLauncher callback updates selection holder on complete result`() = testScenario {
        val selection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION
        val result = FormResult.Complete(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
        formContractCallbackCaptor.value.onActivityResult(result)
        assertThat(selectionHolder.selection.value).isEqualTo(selection)
    }

    @Test
    fun `formActivityLauncher callback does not update selection holder on non-complete result`() = testScenario {
        val result = FormResult.Cancelled
        formContractCallbackCaptor.value.onActivityResult(result)
        assertThat(selectionHolder.selection.value).isNull()
    }

    @Test
    fun `launchManage launches activity with correct parameters`() = testScenario {
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create()
        val customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE
        val expectedArgs = ManageContract.Args(paymentMethodMetadata, customerState, PaymentSelection.GooglePay)
        launcher.launchManage(paymentMethodMetadata, customerState, PaymentSelection.GooglePay)
        val launchState = manageActivityLauncher.launchTurbine.awaitItem()
        assertThat(launchState.didLaunch).isTrue()
        assertThat(launchState.launchArgs).isEqualTo(expectedArgs)
    }

    @Test
    fun `manageActivityLauncher callback updates state on complete result`() = testScenario {
        val customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE
        val selection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        val result = ManageResult.Complete(
            customerState,
            selection,
        )
        manageContractCallbackCaptor.value.onActivityResult(result)
        assertThat(customerStateHolder.customer.value).isEqualTo(customerState)
        assertThat(selectionHolder.selection.value).isEqualTo(selection)
    }

    @Test
    fun `manageActivityLauncher callback does not update state on non-complete result`() = testScenario {
        customerStateHolder.setCustomerState(PaymentSheetFixtures.EMPTY_CUSTOMER_STATE)
        val result = ManageResult.Cancelled(customerState = null)
        manageContractCallbackCaptor.value.onActivityResult(result)
        assertThat(customerStateHolder.customer.value).isEqualTo(PaymentSheetFixtures.EMPTY_CUSTOMER_STATE)
    }

    @Test
    fun `manageActivityLauncher callback updates state on non-complete result`() = testScenario {
        val result = ManageResult.Cancelled(customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE)
        manageContractCallbackCaptor.value.onActivityResult(result)
        assertThat(customerStateHolder.customer.value).isEqualTo(PaymentSheetFixtures.EMPTY_CUSTOMER_STATE)
    }

    @Test
    fun `onDestroy unregisters launchers`() = testScenario {
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        assertThat(formActivityLauncher.unregisterTurbine.awaitItem()).isTrue()
        assertThat(manageActivityLauncher.unregisterTurbine.awaitItem()).isTrue()
    }

    private fun testScenario(
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        MockitoAnnotations.openMocks(this)
        val activityResultCaller = mock<ActivityResultCaller>()
        val lifecycleOwner = TestLifecycleOwner()
        val formActivityLauncher = FakeEmbeddedActivityLauncher(FormContract)
        val manageActivityLauncher = FakeEmbeddedActivityLauncher(ManageContract)
        val savedStateHandle = SavedStateHandle()
        val selectionHolder = EmbeddedSelectionHolder(savedStateHandle)
        val customerStateHolder = CustomerStateHolder(savedStateHandle, selectionHolder.selection)

        @Suppress("UNCHECKED_CAST")
        val formContractCallbackCaptor: ArgumentCaptor<ActivityResultCallback<FormResult>> = ArgumentCaptor
            .forClass(ActivityResultCallback::class.java) as ArgumentCaptor<ActivityResultCallback<FormResult>>

        whenever(
            activityResultCaller.registerForActivityResult(
                any<FormContract>(),
                capture(formContractCallbackCaptor)
            )
        ).thenReturn(formActivityLauncher)

        @Suppress("UNCHECKED_CAST")
        val manageContractCallbackCaptor: ArgumentCaptor<ActivityResultCallback<ManageResult>> = ArgumentCaptor
            .forClass(ActivityResultCallback::class.java) as ArgumentCaptor<ActivityResultCallback<ManageResult>>

        whenever(
            activityResultCaller.registerForActivityResult(
                any<ManageContract>(),
                capture(manageContractCallbackCaptor)
            )
        ).thenReturn(manageActivityLauncher)

        val embeddedActivityLauncher = DefaultEmbeddedSheetLauncher(
            activityResultCaller = activityResultCaller,
            lifecycleOwner = lifecycleOwner,
            selectionHolder = selectionHolder,
            customerStateHolder = customerStateHolder,
        )

        Scenario(
            formContractCallbackCaptor = formContractCallbackCaptor,
            manageContractCallbackCaptor = manageContractCallbackCaptor,
            selectionHolder = selectionHolder,
            lifecycleOwner = lifecycleOwner,
            formActivityLauncher = formActivityLauncher,
            manageActivityLauncher = manageActivityLauncher,
            launcher = embeddedActivityLauncher,
            customerStateHolder = customerStateHolder,
        ).block()

        formActivityLauncher.validate()
        manageActivityLauncher.validate()
    }

    private class Scenario(
        val formContractCallbackCaptor: ArgumentCaptor<ActivityResultCallback<FormResult>>,
        val manageContractCallbackCaptor: ArgumentCaptor<ActivityResultCallback<ManageResult>>,
        val selectionHolder: EmbeddedSelectionHolder,
        val lifecycleOwner: TestLifecycleOwner,
        val formActivityLauncher: FakeEmbeddedActivityLauncher<FormContract.Args>,
        val manageActivityLauncher: FakeEmbeddedActivityLauncher<ManageContract.Args>,
        val launcher: EmbeddedSheetLauncher,
        val customerStateHolder: CustomerStateHolder,
    )
}
