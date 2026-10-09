package com.stripe.android.paymentsheet.state

import android.os.Parcel
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.stripe.android.CardFundingFilter
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.common.model.PaymentMethodRemovePermission
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.isInstanceOf
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.gate.FakeLinkGate
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.ui.inline.LinkSignupMode
import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.DEFAULT_API_CONFIG
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardFundingFilter
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardFundingFilterFactory
import com.stripe.android.model.ClientAttributionMetadata
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.LinkDisabledReason
import com.stripe.android.model.PaymentIntentCreationFlow
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodSelectionFlow
import com.stripe.android.paymentsheet.CardFundingFilteringPrivatePreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.utils.FakeCustomerRepository
import com.stripe.android.utils.FakeDurationProvider
import com.stripe.android.utils.FakeElementsSessionRepository
import com.stripe.android.utils.FakeLinkStore
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestParameterInjector

@RunWith(RobolectricTestParameterInjector::class)
internal class DefaultCreateLinkStateTest {

    @Test
    fun `passes customer email from elements session into retrieveCustomerEmail`() = runTest {
        val retrieveCustomerEmail = FakeRetrieveCustomerEmail()
        val createLinkState = createLinkStateFactory(retrieveCustomerEmail = retrieveCustomerEmail)

        createLinkState(
            elementsSession = createElementsSession(customer = customerWithEmail),
            configuration = PaymentSheetFixtures.CONFIG_MINIMUM.asCommonConfiguration(),
            initializationMode = PAYMENT_INTENT_INIT_MODE,
            customerMetadata = null,
            clientAttributionMetadata = DEFAULT_CLIENT_ATTRIBUTION_METADATA,
            apiConfiguration = DEFAULT_API_CONFIG,
        )

        val invocation = retrieveCustomerEmail.calls.awaitItem()
        assertThat(invocation.customerEmail).isEqualTo(customerWithEmail.email)
        assertThat(invocation.apiConfiguration).isEqualTo(DEFAULT_API_CONFIG)
        assertThat(invocation.initializationMode).isEqualTo(PAYMENT_INTENT_INIT_MODE)
        retrieveCustomerEmail.calls.ensureAllEventsConsumed()
    }

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

    @Test
    fun `isLinkInlineSignupWithSavedPaymentMethodsEnabled is false by default`() =
        testLinkInlineSignupWithSavedPaymentMethodsEnabledFlag(
            flags = emptyMap(),
            isLinkInlineSignupAvailableForSavedPaymentMethods = false,
        )

    @Test
    fun `isLinkInlineSignupWithSavedPaymentMethodsEnabled matches elements session flag`() =
        testLinkInlineSignupWithSavedPaymentMethodsEnabledFlag(
            flags = mapOf(
                ElementsSession.Flag.ELEMENTS_MOBILE_LINK_INLINE_SIGNUP_WITH_SAVED_PM_ENABLED to true
            ),
            isLinkInlineSignupAvailableForSavedPaymentMethods = true,
        )

    @Test
    fun `link is disabled when checkout session should disable wallets for automatic tax billing`() = runTest {
        val createLinkState = createLinkStateFactory()
        val elementsSession = createElementsSession()
        val initializationMode = PaymentElementLoader.InitializationMode.CheckoutSession(
            collectedEmail = "tax@example.com",
            instancesKey = "DefaultCreateLinkStateTest",
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(
                elementsSession = elementsSession,
                automaticTaxEnabled = true,
                taxAddressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
            ),
        )

        val result = createLinkState(
            elementsSession = elementsSession,
            configuration = PaymentSheetFixtures.CONFIG_MINIMUM.asCommonConfiguration(),
            initializationMode = initializationMode,
            customerMetadata = null,
            clientAttributionMetadata = DEFAULT_CLIENT_ATTRIBUTION_METADATA,
            apiConfiguration = DEFAULT_API_CONFIG,
        )

        assertThat(result).isInstanceOf<LinkDisabledState>()
        val disabledState = result as LinkDisabledState
        assertThat(disabledState.customerInfo.email).isEqualTo("tax@example.com")
        assertThat(disabledState.linkDisabledReasons)
            .contains(LinkDisabledReason.AutomaticTaxBillingAddress)
    }

    @Test
    fun `link is disabled when web Link checkout session and configuration are missing email`() =
        testLinkEmailRequirement(
            useNativeLink = false,
            useCheckoutSession = true,
            checkoutSessionCustomerEmail = null,
            defaultEmail = null,
            expectedDisabledReason = LinkDisabledReason.CheckoutSessionsRequiresEmail,
        )

    @Test
    fun `link is enabled when native Link checkout session and configuration are missing email`() =
        testLinkEmailRequirement(
            useNativeLink = true,
            useCheckoutSession = true,
            checkoutSessionCustomerEmail = null,
            defaultEmail = null,
            expectedDisabledReason = null,
        )

    @Test
    fun `link is enabled when web Link checkout session has customer email`() =
        testLinkEmailRequirement(
            useNativeLink = false,
            useCheckoutSession = true,
            checkoutSessionCustomerEmail = "customer@example.com",
            defaultEmail = null,
            expectedDisabledReason = null,
        )

    @Test
    fun `web Link Checkout requires resolved email despite billing defaults`() =
        testLinkEmailRequirement(
            useNativeLink = false,
            useCheckoutSession = true,
            checkoutSessionCustomerEmail = null,
            defaultEmail = "merchant@example.com",
            expectedDisabledReason = LinkDisabledReason.CheckoutSessionsRequiresEmail,
        )

    @Test
    fun `link is enabled when web Link is not initialized with checkout session`() =
        testLinkEmailRequirement(
            useNativeLink = false,
            useCheckoutSession = false,
            checkoutSessionCustomerEmail = null,
            defaultEmail = null,
            expectedDisabledReason = null,
        )

    @Test
    fun `uses checkout session save consent to determine Link signup mode`() = runTest {
        val createLinkState = createLinkStateFactory()
        val elementsSession = createElementsSession()
        val customerMetadata = CustomerMetadata.CheckoutSession(
            sessionId = "cs_test_123",
            customerId = "cus_123",
            removePaymentMethod = PaymentMethodRemovePermission.None,
            saveConsent = PaymentMethodSaveConsentBehavior.Enabled,
        )
        val initializationMode = PaymentElementLoader.InitializationMode.CheckoutSession(
            collectedEmail = null,
            instancesKey = "DefaultCreateLinkStateTest",
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(elementsSession = elementsSession),
        )

        val result = createLinkState(
            elementsSession = elementsSession,
            configuration = PaymentSheetFixtures.CONFIG_MINIMUM.asCommonConfiguration(),
            initializationMode = initializationMode,
            customerMetadata = customerMetadata,
            clientAttributionMetadata = DEFAULT_CLIENT_ATTRIBUTION_METADATA,
            apiConfiguration = DEFAULT_API_CONFIG,
        )

        assertThat(result).isInstanceOf<LinkState>()
        assertThat((result as LinkState).signupMode).isEqualTo(LinkSignupMode.AlongsideSaveForFutureUse)
    }

    @Test
    @Suppress("LongMethod")
    fun `resolved customer context is retained and only enabled Link initializes an account`(
        @TestParameter enabled: Boolean,
    ) = runTest {
        val resolver = FakeRetrieveCustomerEmail()
        val accountCalls = Turbine<LinkConfiguration>()
        val createLinkState = DefaultCreateLinkState(
            accountStatusProvider = {
                accountCalls.add(it)
                AccountStatus.SignedOut
            },
            retrieveCustomerEmail = resolver,
            linkStore = FakeLinkStore(),
            linkGateFactory = FakeLinkGate.Factory(FakeLinkGate()),
            cardFundingFilterFactory = FakeCardFundingFilterFactory(),
        )
        val configuration = PaymentSheetFixtures.CONFIG_MINIMUM.newBuilder()
            .defaultBillingDetails(
                PaymentSheet.BillingDetails(
                    name = "Customer",
                    phone = "123",
                    email = "billing@example.com",
                    address = PaymentSheet.Address(country = "CA"),
                )
            )
            .link(
                PaymentSheet.LinkConfiguration(
                    display = if (enabled) {
                        PaymentSheet.LinkConfiguration.Display.Automatic
                    } else {
                        PaymentSheet.LinkConfiguration.Display.Never
                    }
                )
            )
            .build().asCommonConfiguration()
        val result = createLinkState(
            elementsSession = createElementsSession(customer = customerWithEmail),
            configuration = configuration,
            initializationMode = PAYMENT_INTENT_INIT_MODE,
            customerMetadata = null,
            clientAttributionMetadata = DEFAULT_CLIENT_ATTRIBUTION_METADATA,
            apiConfiguration = DEFAULT_API_CONFIG,
        )
        assertThat(result.customerInfo).isEqualTo(
            LinkConfiguration.CustomerInfo(
                name = "Customer",
                email = customerWithEmail.email,
                phone = "123",
                billingCountryCode = "CA",
            )
        )
        resolver.calls.awaitItem()
        resolver.calls.ensureAllEventsConsumed()
        if (enabled) {
            assertThat(accountCalls.awaitItem().customerInfo).isEqualTo(result.customerInfo)
        } else {
            assertThat((result as LinkDisabledState).linkDisabledReasons).contains(LinkDisabledReason.LinkConfiguration)
        }
        accountCalls.ensureAllEventsConsumed()
        val parcel = Parcel.obtain()
        try {
            parcel.writeParcelable(result, 0)
            parcel.setDataPosition(0)
            val restored = parcel.readParcelable<LinkStateResult>(LinkStateResult::class.java.classLoader)
            assertThat(restored?.customerInfo).isEqualTo(result.customerInfo)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun `web Link accepts resolved Checkout email sources`(
        @TestParameter source: DefaultRetrieveCustomerEmailTest.CheckoutEmailSource,
    ) = runTest {
        val elementsSession = createElementsSession()
        val result = createLinkStateFactory(useNativeLink = false)(
            elementsSession = elementsSession,
            configuration = PaymentSheetFixtures.CONFIG_MINIMUM.asCommonConfiguration(),
            initializationMode = PaymentElementLoader.InitializationMode.CheckoutSession(
                instancesKey = "test",
                collectedEmail = "collected@example.com".takeUnless {
                    source == DefaultRetrieveCustomerEmailTest.CheckoutEmailSource.Absent
                },
                checkoutSessionResponse = CheckoutSessionResponseFactory.create(
                    customerEmail = "session@example.com".takeIf {
                        source == DefaultRetrieveCustomerEmailTest.CheckoutEmailSource.Session
                    },
                    customer = CheckoutSessionResponse.Customer(
                        id = "cus_test",
                        email = "attached@example.com".takeIf {
                            source == DefaultRetrieveCustomerEmailTest.CheckoutEmailSource.Attached
                        },
                        paymentMethods = emptyList(),
                        canDetachPaymentMethod = false,
                    ),
                ),
            ),
            customerMetadata = null,
            clientAttributionMetadata = DEFAULT_CLIENT_ATTRIBUTION_METADATA,
            apiConfiguration = DEFAULT_API_CONFIG,
        )
        assertThat(result.customerInfo.email).isEqualTo(source.email)
        assertThat(result is LinkState).isEqualTo(source.email != null)
    }

    private fun testLinkInlineSignupWithSavedPaymentMethodsEnabledFlag(
        flags: Map<ElementsSession.Flag, Boolean>,
        isLinkInlineSignupAvailableForSavedPaymentMethods: Boolean
    ) = runTest {
        val createLinkState = createLinkStateFactory()
        val configuration = PaymentSheetFixtures.CONFIG_MINIMUM.asCommonConfiguration()
        val elementsSession = createElementsSession(flags)

        val linkStateResult = createLinkState(
            elementsSession = elementsSession,
            configuration = configuration,
            initializationMode = PAYMENT_INTENT_INIT_MODE,
            customerMetadata = null,
            clientAttributionMetadata = DEFAULT_CLIENT_ATTRIBUTION_METADATA,
            apiConfiguration = DEFAULT_API_CONFIG,
        )

        assertThat(linkStateResult).isInstanceOf<LinkState>()

        val linkState = linkStateResult as LinkState

        assertThat(linkState.signupModeResult.availableForSavedPaymentMethods)
            .isEqualTo(isLinkInlineSignupAvailableForSavedPaymentMethods)
    }

    @OptIn(CardFundingFilteringPrivatePreview::class)
    private suspend fun testCardFundingFilterFactory(
        cardFundingTypes: List<PaymentSheet.CardFundingType>,
        enableCardFundFiltering: Boolean,
        expectedFundingTypes: List<PaymentSheet.CardFundingType>
    ) {
        val cardFundingFilterFactory = FakeCardFundingFilterFactory()
        val createLinkState = createLinkStateFactory(cardFundingFilterFactory = cardFundingFilterFactory)

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
            initializationMode = PAYMENT_INTENT_INIT_MODE,
            customerMetadata = null,
            clientAttributionMetadata = DEFAULT_CLIENT_ATTRIBUTION_METADATA,
            apiConfiguration = DEFAULT_API_CONFIG,
        )

        assertThat(cardFundingFilterFactory.invokedWith).isEqualTo(expectedFundingTypes)
    }

    private fun createLinkStateFactory(
        cardFundingFilterFactory: PaymentSheetCardFundingFilterFactory = FakeCardFundingFilterFactory(),
        retrieveCustomerEmail: RetrieveCustomerEmail = DefaultRetrieveCustomerEmail(
            FakeCustomerRepository(),
            FakeDurationProvider(),
        ),
        useNativeLink: Boolean = true,
    ): DefaultCreateLinkState {
        return DefaultCreateLinkState(
            accountStatusProvider = { AccountStatus.SignedOut },
            retrieveCustomerEmail = retrieveCustomerEmail,
            linkStore = FakeLinkStore(),
            linkGateFactory = FakeLinkGate.Factory(
                FakeLinkGate().apply { setUseNativeLink(useNativeLink) }
            ),
            cardFundingFilterFactory = cardFundingFilterFactory
        )
    }

    private fun testLinkEmailRequirement(
        useNativeLink: Boolean,
        useCheckoutSession: Boolean,
        checkoutSessionCustomerEmail: String?,
        defaultEmail: String?,
        expectedDisabledReason: LinkDisabledReason?,
    ) = runTest {
        val createLinkState = createLinkStateFactory(useNativeLink = useNativeLink)
        val elementsSession = createElementsSession()
        val configuration = PaymentSheetFixtures.CONFIG_MINIMUM.newBuilder().apply {
            defaultEmail?.let {
                defaultBillingDetails(PaymentSheet.BillingDetails(email = it))
            }
        }.build().asCommonConfiguration()
        val initializationMode = if (useCheckoutSession) {
            checkoutSessionInitializationMode(
                elementsSession = elementsSession,
                customerEmail = checkoutSessionCustomerEmail,
            )
        } else {
            PAYMENT_INTENT_INIT_MODE
        }

        val result = createLinkState(
            elementsSession = elementsSession,
            configuration = configuration,
            initializationMode = initializationMode,
            customerMetadata = null,
            clientAttributionMetadata = DEFAULT_CLIENT_ATTRIBUTION_METADATA,
            apiConfiguration = DEFAULT_API_CONFIG,
        )

        if (expectedDisabledReason == null) {
            assertThat(result).isInstanceOf<LinkState>()
        } else {
            assertThat(result).isInstanceOf<LinkDisabledState>()
            assertThat((result as LinkDisabledState).linkDisabledReasons)
                .containsExactly(expectedDisabledReason)
        }
    }

    private fun checkoutSessionInitializationMode(
        elementsSession: ElementsSession,
        customerEmail: String?,
    ): PaymentElementLoader.InitializationMode.CheckoutSession {
        return PaymentElementLoader.InitializationMode.CheckoutSession(
            collectedEmail = null,
            instancesKey = "DefaultCreateLinkStateTest",
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(
                elementsSession = elementsSession,
                customerEmail = customerEmail,
            ),
        )
    }

    private fun createElementsSession(
        flags: Map<ElementsSession.Flag, Boolean> = emptyMap(),
        customer: ElementsSession.Customer? = null,
    ): ElementsSession {
        return ElementsSession(
            linkSettings = null,
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            merchantCountry = "US",
            isGooglePayEnabled = false,
            sessionsError = null,
            externalPaymentMethodData = null,
            customer = customer,
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

    private val customerWithEmail = ElementsSession.Customer(
        paymentMethods = emptyList(),
        defaultPaymentMethod = null,
        email = "customer@example.com",
        session = ElementsSession.Customer.Session(
            id = "cuss_123",
            liveMode = false,
            apiKey = "ek_test_123",
            apiKeyExpiry = 999999999,
            customerId = "cus_123",
            components = ElementsSession.Customer.Components(
                mobilePaymentElement = ElementsSession.Customer.Components.MobilePaymentElement.Disabled,
                customerSheet = ElementsSession.Customer.Components.CustomerSheet.Disabled,
            )
        ),
    )

    private class FakeCardFundingFilterFactory : CardFundingFilter.Factory<List<PaymentSheet.CardFundingType>> {
        var invokedWith: List<PaymentSheet.CardFundingType>? = null

        override fun invoke(params: List<PaymentSheet.CardFundingType>): CardFundingFilter {
            invokedWith = params
            return PaymentSheetCardFundingFilter(params)
        }
    }

    private class FakeRetrieveCustomerEmail : RetrieveCustomerEmail {
        val calls = Turbine<Invocation>()

        override suspend fun invoke(
            configuration: CommonConfiguration,
            initializationMode: PaymentElementLoader.InitializationMode,
            customerMetadata: CustomerMetadata?,
            customerEmail: String?,
            apiConfiguration: ApiConfiguration.State,
        ): String? {
            calls.add(
                Invocation(
                    configuration = configuration,
                    initializationMode = initializationMode,
                    customerMetadata = customerMetadata,
                    customerEmail = customerEmail,
                    apiConfiguration = apiConfiguration,
                )
            )
            return customerEmail
        }

        data class Invocation(
            val configuration: CommonConfiguration,
            val initializationMode: PaymentElementLoader.InitializationMode,
            val customerMetadata: CustomerMetadata?,
            val customerEmail: String?,
            val apiConfiguration: ApiConfiguration.State,
        )
    }

    private companion object {
        val PAYMENT_INTENT_INIT_MODE = PaymentElementLoader.InitializationMode.PaymentIntent(
            clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value
        )

        val DEFAULT_CLIENT_ATTRIBUTION_METADATA = ClientAttributionMetadata(
            elementsSessionConfigId = FakeElementsSessionRepository.DEFAULT_ELEMENTS_SESSION_CONFIG_ID,
            paymentIntentCreationFlow = PaymentIntentCreationFlow.Standard,
            paymentMethodSelectionFlow = PaymentMethodSelectionFlow.MerchantSpecified,
            checkoutSessionId = null,
        )
    }
}
