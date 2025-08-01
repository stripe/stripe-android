package com.stripe.android.customersheet.state

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.coroutines.Single
import com.stripe.android.core.networking.AnalyticsEvent
import com.stripe.android.customersheet.CustomerPermissions
import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.CustomerSheetIntegration
import com.stripe.android.customersheet.CustomerSheetLoader
import com.stripe.android.customersheet.DefaultCustomerSheetLoader
import com.stripe.android.customersheet.FakeCustomerAdapter
import com.stripe.android.customersheet.analytics.CustomerSheetEventReporter
import com.stripe.android.customersheet.data.CustomerAdapterDataSource
import com.stripe.android.customersheet.data.CustomerSheetDataResult
import com.stripe.android.customersheet.data.CustomerSheetInitializationDataSource
import com.stripe.android.customersheet.data.CustomerSheetSession
import com.stripe.android.customersheet.data.FakeCustomerSheetInitializationDataSource
import com.stripe.android.customersheet.util.CustomerSheetHacks
import com.stripe.android.googlepaylauncher.GooglePayRepository
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.luxe.LpmRepository
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.financialconnections.IsFinancialConnectionsSdkAvailable
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.testing.PaymentMethodFactory.update
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.utils.CompletableSingle
import com.stripe.android.utils.FakeElementsSessionRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.milliseconds

@Suppress("LargeClass")
internal class DefaultCustomerSheetLoaderTest {
    private val dispatcher = StandardTestDispatcher()
    private val lpmRepository = LpmRepository()

    private val readyGooglePayRepository = mock<GooglePayRepository>()
    private val unreadyGooglePayRepository = mock<GooglePayRepository>()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(dispatcher)

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        whenever(readyGooglePayRepository.isReady()).thenReturn(
            flow {
                emit(true)
            }
        )

        whenever(unreadyGooglePayRepository.isReady()).thenReturn(
            flow {
                emit(false)
            }
        )

        CustomerSheetHacks.clear()
    }

    @Test
    fun `load with configuration should return expected result`() = runTest {
        val loader = createCustomerSheetLoader(
            savedSelection = SavedSelection.PaymentMethod(
                id = PaymentMethodFixtures.CARD_PAYMENT_METHOD.id!!
            ),
            paymentMethods = listOf(
                PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                PaymentMethodFixtures.US_BANK_ACCOUNT,
            ),
        )

        val config = CustomerSheet.Configuration(
            merchantDisplayName = "Example",
            googlePayEnabled = true
        )

        val state = loader.load(config).getOrThrow()
        assertThat(state.config).isEqualTo(config)
        assertThat(state.paymentMethodMetadata.stripeIntent).isEqualTo(STRIPE_INTENT)
        assertThat(state.paymentMethodMetadata.cbcEligibility).isEqualTo(CardBrandChoiceEligibility.Ineligible)
        assertThat(state.paymentMethodMetadata.customerMetadata?.hasCustomerConfiguration).isTrue()
        assertThat(state.paymentMethodMetadata.isGooglePayReady).isTrue()
        assertThat(state.customerPaymentMethods).containsExactly(
            PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            PaymentMethodFixtures.US_BANK_ACCOUNT,
        )
        assertThat(state.customerPermissions.canRemovePaymentMethods).isTrue()
        assertThat(state.customerPermissions.canRemoveLastPaymentMethod).isTrue()
        assertThat(state.supportedPaymentMethods.map { it.code }).containsExactly("card")
        assertThat(state.paymentSelection).isEqualTo(
            PaymentSelection.Saved(
                paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            )
        )
        assertThat(state.validationError).isNull()
    }

    @Test
    fun `when there is a payment selection, the selected PM should be first in the list`() = runTest {
        val loader = createCustomerSheetLoader(
            paymentMethods = listOf(
                PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = "pm_1"),
                PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = "pm_2"),
                PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = "pm_3"),
            ),
            savedSelection = SavedSelection.PaymentMethod(id = "pm_3"),
            isPaymentMethodSyncDefaultEnabled = false,
        )

        val config = CustomerSheet.Configuration(merchantDisplayName = "Example")

        val state = loader.load(config).getOrThrow()
        assertThat(state.config).isEqualTo(config)
        assertThat(state.paymentMethodMetadata.stripeIntent).isEqualTo(STRIPE_INTENT)
        assertThat(state.paymentMethodMetadata.isGooglePayReady).isFalse()
        assertThat(state.customerPaymentMethods).containsExactly(
            PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = "pm_3"),
            PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = "pm_1"),
            PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = "pm_2"),
        ).inOrder()
        assertThat(state.customerPermissions.canRemovePaymentMethods).isTrue()
        assertThat(state.customerPermissions.canRemoveLastPaymentMethod).isTrue()
        assertThat(state.supportedPaymentMethods.map { it.code }).containsExactly("card")
        assertThat(state.paymentSelection).isEqualTo(
            PaymentSelection.Saved(
                paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = "pm_3"),
            )
        )
        assertThat(state.paymentMethodMetadata.cbcEligibility).isEqualTo(CardBrandChoiceEligibility.Ineligible)
        assertThat(state.validationError).isNull()
    }

    @Test
    fun `when there is no payment selection, the order of the payment methods is preserved`() = runTest {
        val loader = createCustomerSheetLoader(
            paymentMethods = listOf(
                PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = "pm_1"),
                PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = "pm_2"),
                PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = "pm_3"),
            ),
            savedSelection = null,
            isPaymentMethodSyncDefaultEnabled = false,
        )

        val config = CustomerSheet.Configuration(merchantDisplayName = "Example")

        val state = loader.load(config).getOrThrow()
        assertThat(state.config).isEqualTo(config)
        assertThat(state.paymentMethodMetadata.stripeIntent).isEqualTo(STRIPE_INTENT)
        assertThat(state.paymentMethodMetadata.isGooglePayReady).isFalse()
        assertThat(state.customerPaymentMethods).containsExactly(
            PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = "pm_1"),
            PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = "pm_2"),
            PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = "pm_3"),
        ).inOrder()
        assertThat(state.customerPermissions.canRemovePaymentMethods).isTrue()
        assertThat(state.customerPermissions.canRemoveLastPaymentMethod).isTrue()
        assertThat(state.supportedPaymentMethods.map { it.code }).containsExactly("card")
        assertThat(state.paymentSelection).isNull()
        assertThat(state.paymentMethodMetadata.cbcEligibility).isEqualTo(CardBrandChoiceEligibility.Ineligible)
        assertThat(state.validationError).isNull()
    }

    @Test
    fun `when default payment method feature is enabled and default PM available, default PM is first and selected`() = runTest {
        val expectedPaymentMethods = listOf(
            PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = "pm_1"),
            PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = "pm_2"),
            PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = "pm_3")
        )
        val defaultPaymentMethod = expectedPaymentMethods[2]
        val loader = createCustomerSheetLoader(
            paymentMethods = expectedPaymentMethods,
            // Setting a saved selection here so we can validate that it is not used.
            savedSelection = SavedSelection.PaymentMethod(expectedPaymentMethods[1].id!!),
            isPaymentMethodSyncDefaultEnabled = true,
            defaultPaymentMethodId = defaultPaymentMethod.id,
        )

        val config = CustomerSheet.Configuration(merchantDisplayName = "Example")

        val state = loader.load(config).getOrThrow()
        assertThat(state.customerPaymentMethods).containsExactlyElementsIn(expectedPaymentMethods)
        assertThat(state.customerPaymentMethods.first()).isEqualTo(defaultPaymentMethod)
        assertThat(state.paymentSelection).isEqualTo(PaymentSelection.Saved(defaultPaymentMethod))
    }

    @Test
    fun `when default payment method feature is enabled and default PM null, PM order is preserved`() = runTest {
        val expectedPaymentMethods = listOf(
            PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = "pm_1"),
            PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = "pm_2"),
            PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = "pm_3")
        )
        val loader = createCustomerSheetLoader(
            paymentMethods = expectedPaymentMethods,
            // Setting a saved selection here so we can validate that it is not used.
            savedSelection = SavedSelection.PaymentMethod(expectedPaymentMethods[1].id!!),
            isPaymentMethodSyncDefaultEnabled = true,
            defaultPaymentMethodId = null,
        )

        val config = CustomerSheet.Configuration(merchantDisplayName = "Example")

        val state = loader.load(config).getOrThrow()
        assertThat(state.customerPaymentMethods).containsExactlyElementsIn(expectedPaymentMethods).inOrder()
        assertThat(state.paymentSelection).isNull()
    }

    @Test
    fun `when default payment method feature is enabled, sepa PMs are filtered out`() = runTest {
        val sepaPaymentMethod = PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD
        val expectedPaymentMethods = listOf(
            PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            PaymentMethodFixtures.US_BANK_ACCOUNT
        )
        val loader = createCustomerSheetLoader(
            paymentMethods = expectedPaymentMethods.plus(sepaPaymentMethod),
            isPaymentMethodSyncDefaultEnabled = true,
        )

        val config = CustomerSheet.Configuration(merchantDisplayName = "Example")

        val state = loader.load(config).getOrThrow()
        assertThat(state.customerPaymentMethods).containsExactlyElementsIn(expectedPaymentMethods).inOrder()
    }

    @Test
    fun `When the FC unavailable, flag disabled, us bank not in intent, then us bank account is not available`() = runTest {
        val loader = createCustomerSheetLoader(
            isFinancialConnectionsAvailable = { false },
            intent = STRIPE_INTENT.copy(
                paymentMethodTypes = listOf("card")
            ),
        )

        val config = CustomerSheet.Configuration(merchantDisplayName = "Example")

        assertThat(
            loader.load(config).getOrThrow().supportedPaymentMethods.map { it.code }
        ).doesNotContain("us_bank_account")
    }

    @Test
    fun `When the FC unavailable, flag disabled, us bank in intent, then us bank account is not available`() = runTest {
        val loader = createCustomerSheetLoader(
            isFinancialConnectionsAvailable = { false },
            intent = STRIPE_INTENT.copy(
                paymentMethodTypes = listOf("card", "us_bank_account")
            ),
        )

        val config = CustomerSheet.Configuration(merchantDisplayName = "Example")

        assertThat(
            loader.load(config).getOrThrow().supportedPaymentMethods.map { it.code }
        ).doesNotContain("us_bank_account")
    }

    @Test
    fun `When the FC unavailable, flag enabled, us bank not in intent, then us bank account is not available`() = runTest {
        val loader = createCustomerSheetLoader(
            isFinancialConnectionsAvailable = { false },
            intent = STRIPE_INTENT.copy(
                paymentMethodTypes = listOf("card")
            ),
        )

        val config = CustomerSheet.Configuration(merchantDisplayName = "Example")

        assertThat(
            loader.load(config).getOrThrow().supportedPaymentMethods.map { it.code }
        ).doesNotContain("us_bank_account")
    }

    @Test
    fun `When the FC unavailable, flag enabled, us bank in intent, then us bank account is not available`() = runTest {
        val loader = createCustomerSheetLoader(
            isFinancialConnectionsAvailable = { false },
            intent = STRIPE_INTENT.copy(
                paymentMethodTypes = listOf("card", "us_bank_account")
            ),
        )

        val config = CustomerSheet.Configuration(merchantDisplayName = "Example")

        assertThat(
            loader.load(config).getOrThrow().supportedPaymentMethods.map { it.code }
        ).doesNotContain("us_bank_account")
    }

    @Test
    fun `When the FC available, flag disabled, us bank not in intent, then us bank account is not available`() = runTest {
        val loader = createCustomerSheetLoader(
            isFinancialConnectionsAvailable = { true },
            intent = STRIPE_INTENT.copy(
                paymentMethodTypes = listOf("card")
            ),
        )

        val config = CustomerSheet.Configuration(merchantDisplayName = "Example")

        assertThat(
            loader.load(config).getOrThrow().supportedPaymentMethods.map { it.code }
        ).doesNotContain("us_bank_account")
    }

    @Test
    fun `When the FC available, flag disabled, us bank in intent, then us bank account is not available`() = runTest {
        val loader = createCustomerSheetLoader(
            isFinancialConnectionsAvailable = { true },
            intent = STRIPE_INTENT.copy(
                paymentMethodTypes = listOf("card", "us_bank_account")
            ),
        )

        val config = CustomerSheet.Configuration(merchantDisplayName = "Example")

        assertThat(
            loader.load(config).getOrThrow().supportedPaymentMethods.map { it.code }
        ).doesNotContain("us_bank_account")
    }

    @Test
    fun `When the FC available, flag enabled, us bank not in intent, then us bank account is not available`() = runTest {
        val loader = createCustomerSheetLoader(
            isFinancialConnectionsAvailable = { true },
            intent = STRIPE_INTENT.copy(
                paymentMethodTypes = listOf("card"),
            ),
        )

        val config = CustomerSheet.Configuration(merchantDisplayName = "Example")

        assertThat(
            loader.load(config).getOrThrow().supportedPaymentMethods.map { it.code }
        ).doesNotContain("us_bank_account")
    }

    @Test
    fun `When the FC available, flag enabled, us bank in intent, then us bank account is available`() = runTest {
        val loader = createCustomerSheetLoader(
            isFinancialConnectionsAvailable = { true },
            intent = STRIPE_INTENT.copy(
                clientSecret = null,
                paymentMethodTypes = listOf("card", "us_bank_account"),
                paymentMethodOptionsJsonString = """
                        {
                            "us_bank_account": {
                                "verification_method": "automatic"
                            }
                        }
                """.trimIndent(),
            ),
        )

        val config = CustomerSheet.Configuration(merchantDisplayName = "Example")

        val supportedPaymentMethods = loader.load(config).getOrThrow().supportedPaymentMethods
        assertThat(supportedPaymentMethods.map { it.code }).contains("us_bank_account")
    }

    @Test
    fun `Loads correct CBC eligibility`() = runTest {
        val loader = createCustomerSheetLoader(isCbcEligible = true)
        val state = loader.load(CustomerSheet.Configuration(merchantDisplayName = "Example")).getOrThrow()
        assertThat(state.paymentMethodMetadata.cbcEligibility)
            .isEqualTo(CardBrandChoiceEligibility.Eligible(emptyList()))
    }

    @Test
    fun `Loads correct CBC eligibility and merchant-preferred networks`() = runTest {
        val loader = createCustomerSheetLoader(isCbcEligible = true)

        val state = loader.load(
            CustomerSheet.Configuration(
                merchantDisplayName = "Example",
                preferredNetworks = listOf(CardBrand.CartesBancaires),
            )
        ).getOrThrow()

        assertThat(state.paymentMethodMetadata.cbcEligibility)
            .isEqualTo(CardBrandChoiceEligibility.Eligible(preferredNetworks = listOf(CardBrand.CartesBancaires)))
    }

    @Test
    fun `Awaits InitializationDataSource if InitializationDataSource is provided after loading started`() = runTest {
        val initDataSource = CompletableSingle<CustomerSheetInitializationDataSource>()

        val configuration = CustomerSheet.Configuration(merchantDisplayName = "Merchant, Inc.")
        val loader = DefaultCustomerSheetLoader(
            isLiveModeProvider = { false },
            googlePayRepositoryFactory = { readyGooglePayRepository },
            initializationDataSourceProvider = initDataSource,
            lpmRepository = lpmRepository,
            isFinancialConnectionsAvailable = { false },
            eventReporter = FakeCustomerSheetEventReporter(),
            errorReporter = FakeErrorReporter(),
            workContext = coroutineContext,
        )

        val completable = CompletableDeferred<Unit>()

        launch {
            loader.load(configuration)
            completable.complete(Unit)
        }

        assertThat(completable.isCompleted).isFalse()

        initDataSource.complete(
            CustomerAdapterDataSource(
                elementsSessionRepository = FakeElementsSessionRepository(
                    stripeIntent = STRIPE_INTENT,
                    error = null,
                    linkSettings = null,
                ),
                workContext = coroutineContext,
                customerAdapter = FakeCustomerAdapter(),
                errorReporter = FakeErrorReporter(),
            )
        )

        withTimeout(100.milliseconds) {
            completable.await()
        }
    }

    @Test
    fun `Fails if awaiting InitializationDataSource times out`() = runTest {
        val configuration = CustomerSheet.Configuration(merchantDisplayName = "Merchant, Inc.")
        val loader = DefaultCustomerSheetLoader(
            isLiveModeProvider = { false },
            googlePayRepositoryFactory = { readyGooglePayRepository },
            lpmRepository = lpmRepository,
            isFinancialConnectionsAvailable = { false },
            eventReporter = FakeCustomerSheetEventReporter(),
            errorReporter = FakeErrorReporter(),
            workContext = coroutineContext,
        )

        val result = loader.load(configuration)

        assertThat(result.exceptionOrNull()).isInstanceOf<IllegalStateException>()
    }

    @Test
    fun `On timeout while waiting for 'CustomerAdapter' instance, show report error`() = runTest {
        val errorReporter = FakeErrorReporter()

        val loader = createCustomerSheetLoader(
            initializationDataSourceProvider = CompletableSingle(),
            errorReporter = errorReporter,
        )

        // Timeouts are skipped in test coroutine contexts so the timeout failure is immediately returned
        loader.load(
            configuration = CustomerSheet.Configuration(merchantDisplayName = "Merchant, Inc.")
        )

        assertThat(
            errorReporter.getLoggedErrors().first()
        ).isEqualTo(
            ErrorReporter.ExpectedErrorEvent.CUSTOMER_SHEET_ADAPTER_NOT_FOUND.eventName
        )
    }

    @Test
    fun `when there are saved cards with disallowed brands they are filtered out`() = runTest {
        val loader = createCustomerSheetLoader(
            paymentMethods = listOf(
                PaymentMethodFactory.card(id = "pm_12345").update(
                    last4 = "1001",
                    addCbcNetworks = false,
                    brand = CardBrand.Visa,
                ),
                PaymentMethodFactory.card(id = "pm_123456").update(
                    last4 = "1000",
                    addCbcNetworks = false,
                    brand = CardBrand.AmericanExpress,
                )
            ),
            savedSelection = null,
        )

        val config = CustomerSheet.Configuration(
            merchantDisplayName = "Example",
            cardBrandAcceptance = PaymentSheet.CardBrandAcceptance.disallowed(
                listOf(PaymentSheet.CardBrandAcceptance.BrandCategory.Visa)
            )
        )

        val state = loader.load(config).getOrThrow()
        assertThat(state.customerPaymentMethods.count()).isEqualTo(
            1
        )
        assertThat(state.customerPaymentMethods.first().card?.brand).isEqualTo(
            CardBrand.AmericanExpress
        )
    }

    @Test
    fun `test load success fires success event`() = runTest {
        val eventReporter = FakeCustomerSheetEventReporter()

        val loader = createCustomerSheetLoader(eventReporter = eventReporter)

        val result = loader.load(
            configuration = CustomerSheet.Configuration(
                merchantDisplayName = "Example",
                googlePayEnabled = true
            )
        )

        assertThat(result.isSuccess).isTrue()
        assertThat(eventReporter.onLoadSucceededCalls.awaitItem()).isNotNull()
    }

    @Test
    fun `test load failure fires failure event`() = runTest {
        val eventReporter = FakeCustomerSheetEventReporter()

        val loader = createCustomerSheetLoader(
            eventReporter = eventReporter,
            initializationDataSource = FakeCustomerSheetInitializationDataSource(
                onLoadCustomerSheetSession = {
                    CustomerSheetDataResult.failure(
                        cause = Throwable("oops"),
                        displayMessage = null
                    )
                }
            )
        )

        val result = loader.load(
            configuration = CustomerSheet.Configuration(
                merchantDisplayName = "Example",
                googlePayEnabled = true
            )
        )

        assertThat(result.isFailure).isTrue()
        assertThat(eventReporter.onLoadFailedCalls.awaitItem().message).isEqualTo("oops")
    }

    private fun createCustomerSheetLoader(
        isGooglePayReady: Boolean = true,
        isLiveModeProvider: () -> Boolean = { false },
        isCbcEligible: Boolean? = null,
        isFinancialConnectionsAvailable: IsFinancialConnectionsSdkAvailable =
            IsFinancialConnectionsSdkAvailable { false },
        intent: StripeIntent = STRIPE_INTENT,
        paymentMethods: List<PaymentMethod> = listOf(),
        savedSelection: SavedSelection? = null,
        isPaymentMethodSyncDefaultEnabled: Boolean = false,
        defaultPaymentMethodId: String? = null,
        initializationDataSource: CustomerSheetInitializationDataSource = FakeCustomerSheetInitializationDataSource(
            onLoadCustomerSheetSession = {
                CustomerSheetDataResult.success(
                    CustomerSheetSession(
                        elementsSession = createElementsSession(
                            intent,
                            createCardBrandChoice(isCbcEligible),
                            isPaymentMethodSyncDefaultEnabled = isPaymentMethodSyncDefaultEnabled,
                        ),
                        paymentMethods = paymentMethods,
                        savedSelection = savedSelection,
                        paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Legacy,
                        permissions = CustomerPermissions(
                            canRemovePaymentMethods = true,
                            canRemoveLastPaymentMethod = true,
                            canUpdateFullPaymentMethodDetails = true,
                        ),
                        defaultPaymentMethodId = defaultPaymentMethodId,
                    )
                )
            }
        ),
        lpmRepository: LpmRepository = this.lpmRepository,
        errorReporter: ErrorReporter = FakeErrorReporter(),
        eventReporter: CustomerSheetEventReporter = FakeCustomerSheetEventReporter(),
    ): CustomerSheetLoader {
        return createCustomerSheetLoader(
            initializationDataSourceProvider = CompletableSingle(initializationDataSource),
            isGooglePayReady = isGooglePayReady,
            isLiveModeProvider = isLiveModeProvider,
            isFinancialConnectionsAvailable = isFinancialConnectionsAvailable,
            lpmRepository = lpmRepository,
            errorReporter = errorReporter,
            eventReporter = eventReporter,
        )
    }

    private fun createElementsSession(
        intent: StripeIntent,
        cardBrandChoice: ElementsSession.CardBrandChoice?,
        isPaymentMethodSyncDefaultEnabled: Boolean,
    ): ElementsSession {
        return ElementsSession(
            stripeIntent = intent,
            cardBrandChoice = cardBrandChoice,
            merchantCountry = null,
            isGooglePayEnabled = false,
            customer = ElementsSession.Customer(
                paymentMethods = listOf(),
                session = ElementsSession.Customer.Session(
                    id = "cuss_123",
                    customerId = "cus_123",
                    liveMode = false,
                    apiKey = "123",
                    apiKeyExpiry = 999999999,
                    components = ElementsSession.Customer.Components(
                        mobilePaymentElement = ElementsSession.Customer.Components.MobilePaymentElement.Disabled,
                        customerSheet = ElementsSession.Customer.Components.CustomerSheet.Enabled(
                            isPaymentMethodRemoveEnabled = false,
                            canRemoveLastPaymentMethod = true,
                            isPaymentMethodSyncDefaultEnabled = isPaymentMethodSyncDefaultEnabled,
                        ),
                    )
                ),
                defaultPaymentMethod = null,
            ),
            linkSettings = null,
            externalPaymentMethodData = null,
            customPaymentMethods = emptyList(),
            paymentMethodSpecs = null,
            flags = emptyMap(),
            elementsSessionId = "session_1234",
            orderedPaymentMethodTypesAndWallets = intent.paymentMethodTypes,
            experimentsData = null,
            passiveCaptcha = null
        )
    }

    private fun createCustomerSheetLoader(
        initializationDataSourceProvider: Single<CustomerSheetInitializationDataSource>,
        isGooglePayReady: Boolean = true,
        isLiveModeProvider: () -> Boolean = { false },
        isFinancialConnectionsAvailable: IsFinancialConnectionsSdkAvailable =
            IsFinancialConnectionsSdkAvailable { false },
        lpmRepository: LpmRepository = this.lpmRepository,
        errorReporter: ErrorReporter = FakeErrorReporter(),
        eventReporter: CustomerSheetEventReporter = FakeCustomerSheetEventReporter(),
        workContext: CoroutineContext = UnconfinedTestDispatcher()
    ): CustomerSheetLoader {
        return DefaultCustomerSheetLoader(
            isLiveModeProvider = isLiveModeProvider,
            googlePayRepositoryFactory = {
                if (isGooglePayReady) readyGooglePayRepository else unreadyGooglePayRepository
            },
            initializationDataSourceProvider = initializationDataSourceProvider,
            lpmRepository = lpmRepository,
            isFinancialConnectionsAvailable = isFinancialConnectionsAvailable,
            eventReporter = eventReporter,
            errorReporter = errorReporter,
            workContext = workContext,
        )
    }

    private fun createCardBrandChoice(isCbcEligible: Boolean?): ElementsSession.CardBrandChoice? {
        return isCbcEligible?.let {
            ElementsSession.CardBrandChoice(
                eligible = it,
                preferredNetworks = listOf("cartes_bancaires")
            )
        }
    }

    private companion object {
        private val STRIPE_INTENT = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
            paymentMethodTypes = listOf("card", "us_bank_account")
        )
    }
}

private class FakeCustomerSheetEventReporter : CustomerSheetEventReporter {
    val onLoadSucceededCalls = Turbine<CustomerSheetSession>()
    val onLoadFailedCalls = Turbine<Throwable>()

    override fun onInit(
        configuration: CustomerSheet.Configuration,
        integrationType: CustomerSheetIntegration.Type,
    ) = Unit

    override fun onLoadSucceeded(customerSheetSession: CustomerSheetSession) {
        onLoadSucceededCalls.add(customerSheetSession)
    }

    override fun onLoadFailed(error: Throwable) {
        onLoadFailedCalls.add(error)
    }

    override fun onScreenPresented(screen: CustomerSheetEventReporter.Screen) = Unit

    override fun onScreenHidden(screen: CustomerSheetEventReporter.Screen) = Unit

    override fun onPaymentMethodSelected(code: String) = Unit

    override fun onConfirmPaymentMethodSucceeded(
        type: String,
        syncDefaultEnabled: Boolean?,
    ) = Unit

    override fun onConfirmPaymentMethodFailed(
        type: String,
        syncDefaultEnabled: Boolean?,
    ) = Unit

    override fun onEditTapped() = Unit

    override fun onEditCompleted() = Unit

    override fun onRemovePaymentMethodSucceeded() = Unit

    override fun onRemovePaymentMethodFailed() = Unit

    override fun onAttachPaymentMethodSucceeded(style: CustomerSheetEventReporter.AddPaymentMethodStyle) = Unit

    override fun onAttachPaymentMethodCanceled(style: CustomerSheetEventReporter.AddPaymentMethodStyle) = Unit

    override fun onAttachPaymentMethodFailed(style: CustomerSheetEventReporter.AddPaymentMethodStyle) = Unit

    override fun onShowPaymentOptionBrands(
        source: CustomerSheetEventReporter.CardBrandChoiceEventSource,
        selectedBrand: CardBrand,
    ) = Unit

    override fun onHidePaymentOptionBrands(
        source: CustomerSheetEventReporter.CardBrandChoiceEventSource,
        selectedBrand: CardBrand?,
    ) = Unit

    override fun onBrandChoiceSelected(
        source: CustomerSheetEventReporter.CardBrandChoiceEventSource,
        selectedBrand: CardBrand,
    ) = Unit

    override fun onUpdatePaymentMethodSucceeded(selectedBrand: CardBrand?) = Unit

    override fun onUpdatePaymentMethodFailed(selectedBrand: CardBrand?, error: Throwable) = Unit

    override fun onCardNumberCompleted() = Unit

    override fun onDisallowedCardBrandEntered(brand: CardBrand) = Unit

    override fun onAnalyticsEvent(event: AnalyticsEvent) = Unit
}
