package com.stripe.android.paymentelement.embedded.form

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.utils.FakeLinkConfigurationCoordinator
import com.stripe.android.utils.NullCardAccountRangeRepositoryFactory
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class FormActivityViewModelTest {

    @Test
    fun `viewmodel initializes interactor correctly`() = runTest {
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create()

        val selectedPaymentMethodCode = "klarna"

        val viewModel = createViewModel(paymentMethodMetadata, selectedPaymentMethodCode)

        assertThat(viewModel.formInteractor.state.value.formArguments.paymentMethodCode).isEqualTo("klarna")
    }

    private fun createViewModel(
        paymentMethodMetadata: PaymentMethodMetadata,
        paymentMethodCode: PaymentMethodCode
    ): FormActivityViewModel {
        val savedStateHandle = SavedStateHandle()
        val selectionHolder = EmbeddedSelectionHolder(savedStateHandle)

        return FormActivityViewModel(
            paymentMethodMetadata = paymentMethodMetadata,
            selectedPaymentMethodCode = paymentMethodCode,
            cardAccountRangeRepositoryFactory = NullCardAccountRangeRepositoryFactory,
            selectionHolder = selectionHolder,
            linkConfigurationCoordinator = FakeLinkConfigurationCoordinator(),
            eventReporter = FakeEventReporter()
        )
    }
}
