package com.stripe.android.paymentelement.embedded

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.utils.FakeLinkConfigurationCoordinator
import com.stripe.android.utils.NullCardAccountRangeRepositoryFactory
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class FormActivityViewModelTest {

    @Test
    fun `initializeFormInteractor sets up form interactor correctly`() = testScenario {
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_SUCCEEDED,
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(),
            allowsDelayedPaymentMethods = true,
            allowsPaymentMethodsRequiringShippingAddress = true,
            isGooglePayReady = true,
            cbcEligibility = CardBrandChoiceEligibility.Ineligible,
        )

        val selectedPaymentMethodCode = "card"

        viewModel.initializeFormInteractor(
            paymentMethodMetadata,
            selectedPaymentMethodCode
        )

        assert(viewModel.formInteractor.state.value.formArguments.paymentMethodCode == "card")
    }

    private fun testScenario(
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val savedStateHandle = SavedStateHandle()
        val selectionHolder = EmbeddedSelectionHolder(savedStateHandle)

        val viewModel = FormActivityViewModel(
            cardAccountRangeRepositoryFactory = NullCardAccountRangeRepositoryFactory,
            selectionHolder = EmbeddedSelectionHolder(savedStateHandle),
            linkConfigurationCoordinator = FakeLinkConfigurationCoordinator()
        )

        Scenario(
            viewModel = viewModel,
            selectionHolder = selectionHolder,
        ).block()
    }

    private class Scenario(
        val viewModel: FormActivityViewModel,
        val selectionHolder: EmbeddedSelectionHolder,
    )
}
