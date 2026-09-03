package com.stripe.android.paymentelement.embedded.content

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.EmbeddedFormHelperFactory
import com.stripe.android.paymentsheet.DefaultCustomerStateHolder
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.uicore.utils.stateFlowOf
import com.stripe.android.utils.FakeIsNfcScanningAvailable
import com.stripe.android.utils.FakeLinkConfigurationCoordinator
import com.stripe.android.utils.FakePaymentMethodMessagePromotionsHelper
import com.stripe.android.utils.NullCardAccountRangeRepositoryFactory
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test

internal class EmbeddedPreferFormInteractorFactoryTest {
    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `selected payment method requiring a form is displayed inline`() = runTest {
        val testScope = TestScope(UnconfinedTestDispatcher())
        val savedStateHandle = SavedStateHandle()
        val selectionHolder = DefaultEmbeddedSelectionHolder(savedStateHandle).apply {
            setTemporarySelection("us_bank_account")
        }
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "us_bank_account"),
            ),
        )
        val formHelperFactory = EmbeddedFormHelperFactory(
            linkConfigurationCoordinator = FakeLinkConfigurationCoordinator(),
            embeddedSelectionHolder = selectionHolder,
            cardAccountRangeRepositoryFactory = NullCardAccountRangeRepositoryFactory,
            savedStateHandle = savedStateHandle,
            isNfcScanningAvailable = FakeIsNfcScanningAvailable(result = false),
        )
        val customerStateHolder = DefaultCustomerStateHolder(
            customerMetadata = stateFlowOf(null),
            paymentMethodMetadataFlow = stateFlowOf(metadata),
            savedStateHandle = savedStateHandle,
            selection = selectionHolder.selection,
        )
        val factory = DefaultEmbeddedPreferFormInteractorFactory(
            selectionHolder = selectionHolder,
            formHelperFactory = formHelperFactory,
            customerStateHolder = customerStateHolder,
            confirmationHandler = FakeConfirmationHandler(),
            eventReporter = FakeEventReporter(),
            promotionsHelper = FakePaymentMethodMessagePromotionsHelper(),
            validationStateHolder = EmbeddedContentValidationStateHolder(),
            viewModelScope = testScope,
        )

        val interactor = factory.create(
            paymentMethodMetadata = metadata,
            configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.")
                .preferForm(true)
                .build(),
            walletsState = stateFlowOf(null),
            preferFormDisabled = false,
        )

        assertThat(interactor).isNotNull()
        assertThat(interactor?.state?.value?.selectedPaymentMethodCode).isEqualTo("us_bank_account")

        interactor?.close()
        testScope.cancel()
    }
}
