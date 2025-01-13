package com.stripe.android.paymentelement.confirmation

import android.app.Application
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherContractV2
import com.stripe.android.isInstanceOf
import com.stripe.android.link.LinkActivityContract
import com.stripe.android.payments.paymentlauncher.PaymentLauncherContract
import com.stripe.android.paymentsheet.ExternalPaymentMethodContract
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationContract
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcRecollectionContract
import com.stripe.android.utils.DummyActivityResultCaller
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExtendedPaymentElementConfirmationOrderTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun `on register, should register contracts in expected order`() = runTest {
        DummyActivityResultCaller.test {
            val viewModel = DaggerExtendedPaymentElementConfirmationTestComponent.builder()
                .application(application)
                .allowsManualConfirmation(allowsManualConfirmation = false)
                .statusBarColor(statusBarColor = null)
                .savedStateHandle(SavedStateHandle())
                .build().viewModel

            viewModel.confirmationHandler.register(
                activityResultCaller = activityResultCaller,
                lifecycleOwner = TestLifecycleOwner()
            )

            assertThat(awaitRegisterCall().contract).isInstanceOf<BacsMandateConfirmationContract>()
            assertThat(awaitNextRegisteredLauncher()).isInstanceOf<ActivityResultLauncher<*>>()

            assertThat(awaitRegisterCall().contract).isInstanceOf<CvcRecollectionContract>()
            assertThat(awaitNextRegisteredLauncher()).isInstanceOf<ActivityResultLauncher<*>>()

            assertThat(awaitRegisterCall().contract).isInstanceOf<ExternalPaymentMethodContract>()
            assertThat(awaitNextRegisteredLauncher()).isInstanceOf<ActivityResultLauncher<*>>()

            assertThat(awaitRegisterCall().contract).isInstanceOf<GooglePayPaymentMethodLauncherContractV2>()
            assertThat(awaitNextRegisteredLauncher()).isInstanceOf<ActivityResultLauncher<*>>()

            assertThat(awaitRegisterCall().contract).isInstanceOf<PaymentLauncherContract>()
            assertThat(awaitNextRegisteredLauncher()).isInstanceOf<ActivityResultLauncher<*>>()

            assertThat(awaitRegisterCall().contract).isInstanceOf<LinkActivityContract>()
            assertThat(awaitNextRegisteredLauncher()).isInstanceOf<ActivityResultLauncher<*>>()
        }
    }
}
