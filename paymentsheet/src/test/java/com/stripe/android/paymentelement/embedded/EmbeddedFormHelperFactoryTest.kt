package com.stripe.android.paymentelement.embedded

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.FormHelper
import com.stripe.android.paymentsheet.PaymentSheet
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
    fun `restores previous new selection for code when live selection is for a different code`() {
        val selectionHolder = DefaultEmbeddedSelectionHolder(SavedStateHandle())

        // Enter and stash a Klarna selection (with a billing email), then switch the live
        // selection to a different payment method code (card).
        selectionHolder.setSelection(
            PaymentSelection.New.GenericPaymentMethod(
                label = "Klarna".resolvableString,
                iconResource = 0,
                iconResourceNight = null,
                lightThemeIconUrl = null,
                darkThemeIconUrl = null,
                paymentMethodCreateParams = PaymentMethodCreateParams.createKlarna(
                    billingDetails = PaymentMethod.BillingDetails(email = "example@email.com"),
                ),
                customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest,
                paymentMethodOptionsParams = null,
                paymentMethodExtraParams = null,
            )
        )
        selectionHolder.setSelection(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)

        val formHelper = createFormHelper(
            selectionHolder = selectionHolder,
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf("card", "klarna"),
                ),
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                ),
            ),
        )

        // The live selection is for "card", so the "klarna" form falls back to the stashed
        // Klarna selection and restores its previously entered email.
        val emailField = formHelper.formElementsForCode("klarna")
            .flatMap { it.getFormFieldValueFlow().value }
            .first { it.first.v1 == "billing_details[email]" }
        assertThat(emailField.second.value).isEqualTo("example@email.com")
    }

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

    private fun createFormHelper(
        selectionHolder: EmbeddedSelectionHolder,
        paymentMethodMetadata: PaymentMethodMetadata,
    ): FormHelper {
        val factory = EmbeddedFormHelperFactory(
            linkConfigurationCoordinator = FakeLinkConfigurationCoordinator(),
            embeddedSelectionHolder = selectionHolder,
            cardAccountRangeRepositoryFactory = NullCardAccountRangeRepositoryFactory,
            savedStateHandle = SavedStateHandle(),
            isNfcScanningAvailable = FakeIsNfcScanningAvailable(result = false),
        )
        return factory.create(
            coroutineScope = TestScope(UnconfinedTestDispatcher()),
            setAsDefaultMatchesSaveForFutureUse = false,
            paymentMethodMetadata = paymentMethodMetadata,
            eventReporter = FakeEventReporter(),
            automaticallyLaunchedCardScanFormDataHelper = null,
            tapToAddHelper = null,
            paymentMethodMessagePromotionsHelper = null,
            selectionUpdater = {},
        )
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
