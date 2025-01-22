package com.stripe.android.paymentelement.embedded

import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.testing.TestLifecycleOwner
import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethodFixtures
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
    fun `formLauncher launches activity with correct parameters`() = testScenario {
        val code = "test_code"
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create()
        val expectedArgs = FormContract.Args(code, paymentMethodMetadata)
        launcher.launchForm(code, paymentMethodMetadata)
        assertThat(formActivityLauncher.didLaunch).isTrue()
        assertThat(formActivityLauncher.launchArgs).isEqualTo(expectedArgs)
    }

    @Test
    fun `formActivityLauncher callback updates selection holder on complete result`() = testScenario {
        val selection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION
        val result = FormResult.Complete(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
        contractCallbackCaptor.value.onActivityResult(result)
        assertThat(selectionHolder.selection.value).isEqualTo(selection)
    }

    @Test
    fun `formActivityLauncher callback does not update selection holder on non-complete result`() = testScenario {
        val result = FormResult.Cancelled
        contractCallbackCaptor.value.onActivityResult(result)
        assertThat(selectionHolder.selection.value).isNull()
    }

    @Test
    fun `formActivityLauncher unregisters onDestroy`() = testScenario {
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        assertThat(formActivityLauncher.didUnregister).isTrue()
    }

    private fun testScenario(
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        MockitoAnnotations.openMocks(this)
        val activityResultCaller = mock<ActivityResultCaller>()
        val lifecycleOwner = TestLifecycleOwner()
        val formActivityLauncher = FakeFormActivityLauncher()
        val selectionHolder = EmbeddedSelectionHolder(SavedStateHandle())

        @Suppress("UNCHECKED_CAST")
        val contractCallbackCaptor: ArgumentCaptor<ActivityResultCallback<FormResult>> = ArgumentCaptor
            .forClass(ActivityResultCallback::class.java) as ArgumentCaptor<ActivityResultCallback<FormResult>>

        whenever(
            activityResultCaller.registerForActivityResult(
                any<FormContract>(),
                capture(contractCallbackCaptor)
            )
        ).thenReturn(formActivityLauncher)

        val embeddedActivityLauncher = DefaultEmbeddedSheetLauncher(
            activityResultCaller = activityResultCaller,
            lifecycleOwner = lifecycleOwner,
            selectionHolder = selectionHolder
        )

        Scenario(
            contractCallbackCaptor = contractCallbackCaptor,
            selectionHolder = selectionHolder,
            lifecycleOwner = lifecycleOwner,
            formActivityLauncher = formActivityLauncher,
            launcher = embeddedActivityLauncher
        ).block()
    }

    private class Scenario(
        val contractCallbackCaptor: ArgumentCaptor<ActivityResultCallback<FormResult>>,
        val selectionHolder: EmbeddedSelectionHolder,
        val lifecycleOwner: TestLifecycleOwner,
        val formActivityLauncher: FakeFormActivityLauncher,
        val launcher: EmbeddedSheetLauncher,
    )
}
