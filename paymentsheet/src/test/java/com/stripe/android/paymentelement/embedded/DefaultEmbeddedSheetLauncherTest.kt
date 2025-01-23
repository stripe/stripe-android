package com.stripe.android.paymentelement.embedded

import androidx.activity.result.ActivityResultCallback
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
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class DefaultEmbeddedSheetLauncherTest {

    @Test
    fun `launchForm launches activity with correct parameters`() = testScenario {
        val code = "test_code"
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create()
        val expectedArgs = FormContract.Args(code, paymentMethodMetadata)
        DummyActivityResultCaller.test {
            val launcher = DefaultEmbeddedSheetLauncher(
                activityResultCaller = activityResultCaller,
                lifecycleOwner = lifecycleOwner,
                selectionHolder = selectionHolder,
                customerStateHolder = customerStateHolder
            )

            val registerCall = awaitRegisterCall()
            // Because DefaultEmbeddedPaymentSheetLauncher has two launchers we have to
            // duplicate all turbine calls
            awaitRegisterCall()

            assertThat(registerCall).isNotNull()

            launcher.launchForm(code, paymentMethodMetadata)

            val launchCall = awaitLaunchCall()

            assertThat(launchCall).isEqualTo(expectedArgs)

            // oof
            awaitNextRegisteredLauncher()
            awaitNextRegisteredLauncher()
        }
    }

    @Test
    fun `formActivityLauncher callback updates selection holder on complete result`() = testScenario {
        val selection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION
        val result = FormResult.Complete(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
        DummyActivityResultCaller.test {
            // Need to initialize launcher for, even though it's not used, for DummyActivityResultCaller
            // to work
            DefaultEmbeddedSheetLauncher(
                activityResultCaller = activityResultCaller,
                lifecycleOwner = lifecycleOwner,
                selectionHolder = selectionHolder,
                customerStateHolder = customerStateHolder
            )

            val registerCall = awaitRegisterCall()
            // rip
            awaitRegisterCall()

            assertThat(awaitNextRegisteredLauncher()).isNotNull()
            // oof
            awaitNextRegisteredLauncher()

            assertThat(registerCall.contract).isInstanceOf<FormContract>()
            assertThat(registerCall.callback).isInstanceOf<ActivityResultCallback<*>>()

            val callback = registerCall.callback.asCallbackFor<FormResult>()

            callback.onActivityResult(result)

            assertThat(selectionHolder.selection.value).isEqualTo(selection)
        }
    }

    @Test
    fun `formActivityLauncher callback does not update selection holder on non-complete result`() = testScenario {
        val result = FormResult.Cancelled
        DummyActivityResultCaller.test {
            DefaultEmbeddedSheetLauncher(
                activityResultCaller = activityResultCaller,
                lifecycleOwner = lifecycleOwner,
                selectionHolder = selectionHolder,
                customerStateHolder = customerStateHolder
            )

            val registerCall = awaitRegisterCall()
            // rip
            awaitRegisterCall()

            assertThat(awaitNextRegisteredLauncher()).isNotNull()
            // oof
            awaitNextRegisteredLauncher()

            assertThat(registerCall.contract).isInstanceOf<FormContract>()
            assertThat(registerCall.callback).isInstanceOf<ActivityResultCallback<*>>()

            val callback = registerCall.callback.asCallbackFor<FormResult>()

            callback.onActivityResult(result)

            assertThat(selectionHolder.selection.value).isEqualTo(null)
        }
    }

    @Test
    fun `launchManage launches activity with correct parameters`() = testScenario {
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create()
        val customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE
        val expectedArgs = ManageContract.Args(paymentMethodMetadata, customerState, PaymentSelection.GooglePay)
        DummyActivityResultCaller.test {
            val launcher = DefaultEmbeddedSheetLauncher(
                activityResultCaller = activityResultCaller,
                lifecycleOwner = lifecycleOwner,
                selectionHolder = selectionHolder,
                customerStateHolder = customerStateHolder
            )

            // my eyes
            awaitRegisterCall()
            val registerCall = awaitRegisterCall()
            assertThat(registerCall).isNotNull()

            launcher.launchManage(paymentMethodMetadata, customerState, PaymentSelection.GooglePay)
            val launchCall = awaitLaunchCall()

            assertThat(launchCall).isEqualTo(expectedArgs)

            // no thx
            awaitNextRegisteredLauncher()
            awaitNextRegisteredLauncher()
        }
    }

    @Test
    fun `manageActivityLauncher callback updates state on complete result`() = testScenario {
        val customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE
        val selection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        val result = ManageResult.Complete(
            customerState,
            selection,
        )
        DummyActivityResultCaller.test {
            DefaultEmbeddedSheetLauncher(
                activityResultCaller = activityResultCaller,
                lifecycleOwner = lifecycleOwner,
                selectionHolder = selectionHolder,
                customerStateHolder = customerStateHolder
            )

            // rip
            awaitRegisterCall()
            val registerCall = awaitRegisterCall()

            // oof
            awaitNextRegisteredLauncher()
            assertThat(awaitNextRegisteredLauncher()).isNotNull()

            assertThat(registerCall.contract).isInstanceOf<ManageContract>()
            assertThat(registerCall.callback).isInstanceOf<ActivityResultCallback<*>>()

            val callback = registerCall.callback.asCallbackFor<ManageResult>()

            callback.onActivityResult(result)

            assertThat(customerStateHolder.customer.value).isEqualTo(customerState)
            assertThat(selectionHolder.selection.value).isEqualTo(selection)
        }
    }

    @Test
    fun `manageActivityLauncher callback does not update state on non-complete result`() = testScenario {
        customerStateHolder.setCustomerState(PaymentSheetFixtures.EMPTY_CUSTOMER_STATE)
        val result = ManageResult.Cancelled(customerState = null)
        DummyActivityResultCaller.test {
            DefaultEmbeddedSheetLauncher(
                activityResultCaller = activityResultCaller,
                lifecycleOwner = lifecycleOwner,
                selectionHolder = selectionHolder,
                customerStateHolder = customerStateHolder
            )

            // rip
            awaitRegisterCall()
            val registerCall = awaitRegisterCall()

            // oof
            awaitNextRegisteredLauncher()
            assertThat(awaitNextRegisteredLauncher()).isNotNull()

            assertThat(registerCall.contract).isInstanceOf<ManageContract>()
            assertThat(registerCall.callback).isInstanceOf<ActivityResultCallback<*>>()

            val callback = registerCall.callback.asCallbackFor<ManageResult>()

            callback.onActivityResult(result)

            assertThat(customerStateHolder.customer.value).isEqualTo(PaymentSheetFixtures.EMPTY_CUSTOMER_STATE)
            assertThat(selectionHolder.selection.value).isEqualTo(null)
        }
    }

    @Test
    fun `manageActivityLauncher callback updates state on non-complete result`() = testScenario {
        customerStateHolder.setCustomerState(PaymentSheetFixtures.EMPTY_CUSTOMER_STATE)
        val result = ManageResult.Cancelled(customerState = null)
        DummyActivityResultCaller.test {
            DefaultEmbeddedSheetLauncher(
                activityResultCaller = activityResultCaller,
                lifecycleOwner = lifecycleOwner,
                selectionHolder = selectionHolder,
                customerStateHolder = customerStateHolder
            )

            // rip
            awaitRegisterCall()
            val registerCall = awaitRegisterCall()

            // oof
            awaitNextRegisteredLauncher()
            assertThat(awaitNextRegisteredLauncher()).isNotNull()

            assertThat(registerCall.contract).isInstanceOf<ManageContract>()
            assertThat(registerCall.callback).isInstanceOf<ActivityResultCallback<*>>()

            val callback = registerCall.callback.asCallbackFor<ManageResult>()

            callback.onActivityResult(result)

            assertThat(customerStateHolder.customer.value).isEqualTo(PaymentSheetFixtures.EMPTY_CUSTOMER_STATE)
            assertThat(selectionHolder.selection.value).isEqualTo(null)
        }
    }

    @Test
    fun `onDestroy unregisters launchers`() = testScenario {
        DummyActivityResultCaller.test {
            DefaultEmbeddedSheetLauncher(
                activityResultCaller = activityResultCaller,
                lifecycleOwner = lifecycleOwner,
                selectionHolder = selectionHolder,
                customerStateHolder = customerStateHolder
            )

            awaitRegisterCall()
            awaitRegisterCall()
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

            val formUnregisterCall = awaitUnregisterCall()
            val manageUnregisterCall = awaitUnregisterCall()

            assertThat(formUnregisterCall).isNotNull()
            assertThat(manageUnregisterCall).isNotNull()

            awaitNextRegisteredLauncher()
            awaitNextRegisteredLauncher()
        }
    }

    private fun testScenario(
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        MockitoAnnotations.openMocks(this)
        val lifecycleOwner = TestLifecycleOwner()
        val savedStateHandle = SavedStateHandle()
        val selectionHolder = EmbeddedSelectionHolder(savedStateHandle)
        val customerStateHolder = CustomerStateHolder(savedStateHandle, selectionHolder.selection)

        Scenario(
            selectionHolder = selectionHolder,
            lifecycleOwner = lifecycleOwner,
            customerStateHolder = customerStateHolder,
        ).block()
    }

    private class Scenario(
        val selectionHolder: EmbeddedSelectionHolder,
        val lifecycleOwner: TestLifecycleOwner,
        val customerStateHolder: CustomerStateHolder,
    )
}
