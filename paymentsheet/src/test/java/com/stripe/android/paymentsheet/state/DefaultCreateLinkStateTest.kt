package com.stripe.android.paymentsheet.state

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.CardFundingFilter
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.link.account.LinkStore
import com.stripe.android.link.gate.FakeLinkGate
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardFundingFilter
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardFundingFilterFactory
import com.stripe.android.model.ClientAttributionMetadata
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentIntentCreationFlow
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodSelectionFlow
import com.stripe.android.paymentsheet.CardFundingFilteringPrivatePreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.utils.FakeCustomerRepository
import com.stripe.android.utils.FakeElementsSessionRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class DefaultCreateLinkStateTest {

    @Test
    fun `cardFundingFilterFactory invoked with custom types when enableCardFundFiltering is true`() = runTest {
        testCardFundingFilterFactory(
            cardFundingTypes = listOf(PaymentSheet.CardFundingType.Credit),
            enableCardFundFiltering = true,
            expectedFundingTypes = listOf(PaymentSheet.CardFundingType.Credit)
        )
    }

    @Test
    fun `cardFundingFilterFactory invoked with default types when enableCardFundFiltering is false`() = runTest {
        testCardFundingFilterFactory(
            cardFundingTypes = listOf(PaymentSheet.CardFundingType.Credit),
            enableCardFundFiltering = false,
            expectedFundingTypes = PaymentSheet.CardFundingType.entries
        )
    }

    @OptIn(CardFundingFilteringPrivatePreview::class)
    private suspend fun testCardFundingFilterFactory(
        cardFundingTypes: List<PaymentSheet.CardFundingType>,
        enableCardFundFiltering: Boolean,
        expectedFundingTypes: List<PaymentSheet.CardFundingType>
    ) {
        val cardFundingFilterFactory = FakeCardFundingFilterFactory()
        val createLinkState = createLinkStateFactory(cardFundingFilterFactory)

        val configuration = PaymentSheetFixtures.CONFIG_MINIMUM
            .newBuilder()
            .allowedCardFundingTypes(cardFundingTypes)
            .build()
            .asCommonConfiguration()

        val elementsSession = createElementsSession(
            flags = mapOf(
                ElementsSession.Flag.ELEMENTS_MOBILE_CARD_FUND_FILTERING to enableCardFundFiltering
            )
        )

        createLinkState(
            elementsSession = elementsSession,
            configuration = configuration,
            customer = null,
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value
            ),
            clientAttributionMetadata = ClientAttributionMetadata(
                elementsSessionConfigId = FakeElementsSessionRepository.DEFAULT_ELEMENTS_SESSION_CONFIG_ID,
                paymentIntentCreationFlow = PaymentIntentCreationFlow.Standard,
                paymentMethodSelectionFlow = PaymentMethodSelectionFlow.MerchantSpecified
            )
        )

        assertThat(cardFundingFilterFactory.invokedWith).isEqualTo(expectedFundingTypes)
    }

    private fun createLinkStateFactory(
        cardFundingFilterFactory: PaymentSheetCardFundingFilterFactory,
    ): DefaultCreateLinkState {
        return DefaultCreateLinkState(
            accountStatusProvider = { AccountStatus.SignedOut },
            retrieveCustomerEmail = DefaultRetrieveCustomerEmail(FakeCustomerRepository()),
            linkStore = LinkStore(ApplicationProvider.getApplicationContext()),
            linkGateFactory = FakeLinkGate.Factory(FakeLinkGate()),
            cardFundingFilterFactory = cardFundingFilterFactory
        )
    }

    private fun createElementsSession(
        flags: Map<ElementsSession.Flag, Boolean>
    ): ElementsSession {
        return ElementsSession(
            linkSettings = null,
            paymentMethodSpecs = null,
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            merchantCountry = "US",
            isGooglePayEnabled = false,
            sessionsError = null,
            externalPaymentMethodData = null,
            customer = null,
            cardBrandChoice = null,
            customPaymentMethods = emptyList(),
            elementsSessionId = FakeElementsSessionRepository.DEFAULT_ELEMENTS_SESSION_ID,
            flags = flags,
            orderedPaymentMethodTypesAndWallets = emptyList(),
            experimentsData = null,
            passiveCaptcha = null,
            merchantLogoUrl = null,
            elementsSessionConfigId = FakeElementsSessionRepository.DEFAULT_ELEMENTS_SESSION_CONFIG_ID,
            accountId = "acct_1SGP1sPvdtoA7EjP",
            merchantId = "acct_1SGP1sPvdtoA7EjP",
        )
    }

    private class FakeCardFundingFilterFactory : CardFundingFilter.Factory<List<PaymentSheet.CardFundingType>> {
        var invokedWith: List<PaymentSheet.CardFundingType>? = null

        override fun invoke(params: List<PaymentSheet.CardFundingType>): CardFundingFilter {
            invokedWith = params
            return PaymentSheetCardFundingFilter(params)
        }
    }
}
