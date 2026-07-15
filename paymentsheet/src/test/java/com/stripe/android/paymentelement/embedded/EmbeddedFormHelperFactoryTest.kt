package com.stripe.android.paymentelement.embedded

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.ui.core.elements.AutomaticallyLaunchedCardScanFormDataHelper
import com.stripe.android.ui.core.elements.CardDetailsAction
import com.stripe.android.ui.core.elements.CardDetailsSectionController
import com.stripe.android.utils.FakeIsNfcScanningAvailable
import com.stripe.android.utils.FakeLinkConfigurationCoordinator
import com.stripe.android.utils.NullCardAccountRangeRepositoryFactory
import com.stripe.android.utils.shouldAutomaticallyLaunchCardScan
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import kotlin.test.Test

internal class EmbeddedFormHelperFactoryTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `shouldLaunchCardScanAutomatically is true for an empty card form when configured`() {
        val helper = createCardScanHelper(
            selectedPaymentMethodCode = PaymentMethod.Type.Card.code,
            selection = null,
            openCardScanAutomatically = true,
        )

        assertThat(helper.shouldLaunchCardScanAutomatically).isTrue()
    }

    @Test
    fun `shouldLaunchCardScanAutomatically is false when the card form is being reopened with entered details`() {
        val helper = createCardScanHelper(
            selectedPaymentMethodCode = PaymentMethod.Type.Card.code,
            selection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION,
            openCardScanAutomatically = true,
        )

        assertThat(helper.shouldLaunchCardScanAutomatically).isFalse()
    }

    @Test
    fun `shouldLaunchCardScanAutomatically is false for a non-card form`() {
        val helper = createCardScanHelper(
            selectedPaymentMethodCode = PaymentMethod.Type.CashAppPay.code,
            selection = null,
            openCardScanAutomatically = true,
        )

        assertThat(helper.shouldLaunchCardScanAutomatically).isFalse()
    }

    @Test
    fun `shouldLaunchCardScanAutomatically is false when openCardScanAutomatically is disabled`() {
        val helper = createCardScanHelper(
            selectedPaymentMethodCode = PaymentMethod.Type.Card.code,
            selection = null,
            openCardScanAutomatically = false,
        )

        assertThat(helper.shouldLaunchCardScanAutomatically).isFalse()
    }

    @Test
    fun `create wires the injected card scan helper into the card form`() {
        val cardScanHelper = createCardScanHelper(
            selectedPaymentMethodCode = PaymentMethod.Type.Card.code,
            selection = null,
            openCardScanAutomatically = true,
        )

        val cardDetailsAction = cardDetailsActionForCardForm(
            automaticallyLaunchedCardScanFormDataHelper = cardScanHelper,
        )

        assertThat(cardDetailsAction?.shouldAutomaticallyLaunchCardScan).isTrue()
    }

    @Test
    fun `create omits card scan auto-launch when no helper is injected`() {
        val cardDetailsAction = cardDetailsActionForCardForm(
            automaticallyLaunchedCardScanFormDataHelper = null,
        )

        assertThat(cardDetailsAction?.shouldAutomaticallyLaunchCardScan).isNull()
    }

    private fun createCardScanHelper(
        selectedPaymentMethodCode: PaymentMethodCode,
        selection: PaymentSelection?,
        openCardScanAutomatically: Boolean,
    ): AutomaticallyLaunchedCardScanFormDataHelper {
        val selectionHolder = DefaultEmbeddedSelectionHolder(SavedStateHandle())
        selectionHolder.setSelection(selection)
        val factory = EmbeddedFormHelperFactory(
            linkConfigurationCoordinator = FakeLinkConfigurationCoordinator(),
            embeddedSelectionHolder = selectionHolder,
            cardAccountRangeRepositoryFactory = NullCardAccountRangeRepositoryFactory,
            savedStateHandle = SavedStateHandle(),
            isNfcScanningAvailable = FakeIsNfcScanningAvailable(result = false),
        )
        return factory.createAutomaticallyLaunchedCardScanFormDataHelper(
            selectedPaymentMethodCode = selectedPaymentMethodCode,
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                openCardScanAutomatically = openCardScanAutomatically,
            ),
        )
    }

    private fun cardDetailsActionForCardForm(
        automaticallyLaunchedCardScanFormDataHelper: AutomaticallyLaunchedCardScanFormDataHelper?,
    ): CardDetailsAction? {
        val factory = EmbeddedFormHelperFactory(
            linkConfigurationCoordinator = FakeLinkConfigurationCoordinator(),
            embeddedSelectionHolder = DefaultEmbeddedSelectionHolder(SavedStateHandle()),
            cardAccountRangeRepositoryFactory = NullCardAccountRangeRepositoryFactory,
            savedStateHandle = SavedStateHandle(),
            isNfcScanningAvailable = FakeIsNfcScanningAvailable(result = false),
        )
        val formHelper = factory.create(
            coroutineScope = TestScope(UnconfinedTestDispatcher()),
            setAsDefaultMatchesSaveForFutureUse = false,
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(openCardScanAutomatically = true),
            eventReporter = FakeEventReporter(),
            automaticallyLaunchedCardScanFormDataHelper = automaticallyLaunchedCardScanFormDataHelper,
            tapToAddHelper = null,
            paymentMethodMessagePromotionsHelper = null,
            selectionUpdater = {},
        )
        return formHelper.formElementsForCode(PaymentMethod.Type.Card.code)
            .firstNotNullOf { it.controller as? CardDetailsSectionController }
            .cardDetailsAction
    }
}
