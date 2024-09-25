package com.stripe.android.customersheet.state

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.customersheet.CustomerAdapter
import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.CustomerSheetLoader
import com.stripe.android.customersheet.DefaultCustomerSheetLoader
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import com.stripe.android.customersheet.FakeCustomerAdapter
import com.stripe.android.customersheet.data.CustomerAdapterDataSource
import com.stripe.android.customersheet.data.CustomerSheetInitializationDataSource
import com.stripe.android.customersheet.util.CustomerSheetHacks
import com.stripe.android.googlepaylauncher.GooglePayRepository
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.luxe.LpmRepository
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.ElementsSessionParams
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.financialconnections.IsFinancialConnectionsAvailable
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.ElementsSessionRepository
import com.stripe.android.paymentsheet.repositories.toElementsSessionParams
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.utils.FakeElementsSessionRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Before
import org.junit.Test
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.milliseconds

@Suppress("LargeClass")
@OptIn(ExperimentalCustomerSheetApi::class)
class DefaultCustomerSheetLoaderTest {
    private val lpmRepository = LpmRepository()

    private val readyGooglePayRepository = mock<GooglePayRepository>()
    private val unreadyGooglePayRepository = mock<GooglePayRepository>()

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
        val elementsSessionRepository = FakeElementsSessionRepository(
            stripeIntent = STRIPE_INTENT,
            error = null,
            linkSettings = null,
        )
        val loader = createCustomerSheetLoader(
            customerAdapter = FakeCustomerAdapter(
                selectedPaymentOption = CustomerAdapter.Result.success(
                    CustomerAdapter.PaymentOption.fromId(
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD.id!!
                    )
                ),
                paymentMethods = CustomerAdapter.Result.success(
                    listOf(
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                        PaymentMethodFixtures.US_BANK_ACCOUNT,
                    )
                ),
            ),
            elementsSessionRepository = elementsSessionRepository,
        )

        val config = CustomerSheet.Configuration(
            merchantDisplayName = "Example",
            googlePayEnabled = true
        )

        val state = loader.load(config).getOrThrow()
        assertThat(state.config).isEqualTo(config)
        assertThat(state.paymentMethodMetadata.stripeIntent).isEqualTo(STRIPE_INTENT)
        assertThat(state.paymentMethodMetadata.cbcEligibility).isEqualTo(CardBrandChoiceEligibility.Ineligible)
        assertThat(state.paymentMethodMetadata.hasCustomerConfiguration).isTrue()
        assertThat(state.paymentMethodMetadata.isGooglePayReady).isTrue()
        assertThat(state.customerPaymentMethods).containsExactly(
            PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            PaymentMethodFixtures.US_BANK_ACCOUNT,
        )
        assertThat(state.customerPermissions.canRemovePaymentMethods).isTrue()
        assertThat(state.supportedPaymentMethods.map { it.code }).containsExactly("card")
        assertThat(state.paymentSelection).isEqualTo(
            PaymentSelection.Saved(
                paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            )
        )
        assertThat(state.validationError).isNull()

        val mode = elementsSessionRepository.lastParams?.initializationMode
            as PaymentSheet.InitializationMode.DeferredIntent
        assertThat(mode.intentConfiguration.paymentMethodTypes)
            .isEmpty()
    }

    @Test
    fun `load should return expected result when customer adapter has paymentMethodTypes`() = runTest {
        val elementsSessionRepository = FakeElementsSessionRepository(
            stripeIntent = STRIPE_INTENT,
            error = null,
            linkSettings = null,
        )
        val loader = createCustomerSheetLoader(
            customerAdapter = FakeCustomerAdapter(
                selectedPaymentOption = CustomerAdapter.Result.success(
                    CustomerAdapter.PaymentOption.fromId(
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD.id!!
                    )
                ),
                paymentMethods = CustomerAdapter.Result.success(
                    listOf(
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                        PaymentMethodFixtures.US_BANK_ACCOUNT,
                    )
                ),
                paymentMethodTypes = listOf("card", "us_bank_account"),
            ),
            elementsSessionRepository = elementsSessionRepository,
        )

        val config = CustomerSheet.Configuration(
            merchantDisplayName = "Example",
            googlePayEnabled = true
        )

        val state = loader.load(config).getOrThrow()
        assertThat(state.config).isEqualTo(config)
        assertThat(state.paymentMethodMetadata.stripeIntent).isEqualTo(STRIPE_INTENT)
        assertThat(state.paymentMethodMetadata.isGooglePayReady).isTrue()
        assertThat(state.customerPaymentMethods).containsExactly(
            PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            PaymentMethodFixtures.US_BANK_ACCOUNT,
        )
        assertThat(state.supportedPaymentMethods.map { it.code }).containsExactly("card")
        assertThat(state.paymentSelection).isEqualTo(
            PaymentSelection.Saved(
                paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            )
        )
        assertThat(state.customerPermissions.canRemovePaymentMethods).isTrue()
        assertThat(state.paymentMethodMetadata.cbcEligibility).isEqualTo(CardBrandChoiceEligibility.Ineligible)
        assertThat(state.validationError).isNull()

        val mode = elementsSessionRepository.lastParams?.initializationMode
            as PaymentSheet.InitializationMode.DeferredIntent
        assertThat(mode.intentConfiguration.paymentMethodTypes)
            .isEqualTo(listOf("card", "us_bank_account"))
    }

    @Test
    fun `when setup intent cannot be created, supported payment methods should contain at least card`() = runTest {
        val loader = createCustomerSheetLoader(
            customerAdapter = FakeCustomerAdapter(
                paymentMethods = CustomerAdapter.Result.success(
                    listOf(
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                    )
                ),
                canCreateSetupIntents = false,
            )
        )

        val config = CustomerSheet.Configuration(merchantDisplayName = "Example")

        val state = loader.load(config).getOrThrow()
        assertThat(state.config).isEqualTo(config)
        assertThat(state.paymentMethodMetadata).isNotNull()
        assertThat(state.paymentMethodMetadata.isGooglePayReady).isFalse()
        assertThat(state.customerPaymentMethods).containsExactly(
            PaymentMethodFixtures.CARD_PAYMENT_METHOD,
        )
        assertThat(state.supportedPaymentMethods.map { it.code }).containsExactly("card")
        assertThat(state.customerPermissions.canRemovePaymentMethods).isTrue()
        assertThat(state.paymentSelection).isNull()
        assertThat(state.paymentMethodMetadata.cbcEligibility).isEqualTo(CardBrandChoiceEligibility.Ineligible)
        assertThat(state.validationError).isNull()
    }

    @Test
    fun `when setup intent cannot be created, elements sessions is called with card only`() = runTest {
        val elementsSessionRepository = FakeElementsSessionRepository(
            stripeIntent = STRIPE_INTENT,
            error = null,
            linkSettings = null,
        )
        val loader = createCustomerSheetLoader(
            customerAdapter = FakeCustomerAdapter(
                paymentMethods = CustomerAdapter.Result.success(
                    listOf(
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                    )
                ),
                canCreateSetupIntents = false,
            ),
            elementsSessionRepository = elementsSessionRepository,
        )

        val config = CustomerSheet.Configuration(
            merchantDisplayName = "Example",
        )

        assertThat(loader.load(config).getOrThrow()).isNotNull()
        val params = elementsSessionRepository.lastParams?.initializationMode?.toElementsSessionParams(
            customer = null,
            externalPaymentMethods = emptyList(),
            defaultPaymentMethodId = null,
        ) as ElementsSessionParams.DeferredIntentType
        assertThat(params.deferredIntentParams.paymentMethodTypes).containsExactly("card")
    }

    @Test
    fun `when setup intent cannot be created, elements sessions is called with card only when paymentMethodTypes is set`() = runTest {
        val elementsSessionRepository = FakeElementsSessionRepository(
            stripeIntent = STRIPE_INTENT,
            error = null,
            linkSettings = null,
        )
        val loader = createCustomerSheetLoader(
            customerAdapter = FakeCustomerAdapter(
                paymentMethods = CustomerAdapter.Result.success(
                    listOf(
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                    )
                ),
                canCreateSetupIntents = false,
                paymentMethodTypes = listOf("card", "us_bank_account"),
            ),
            elementsSessionRepository = elementsSessionRepository,
        )

        val config = CustomerSheet.Configuration(
            merchantDisplayName = "Example",
        )

        assertThat(loader.load(config).getOrThrow()).isNotNull()
        val params = elementsSessionRepository.lastParams?.initializationMode?.toElementsSessionParams(
            customer = null,
            externalPaymentMethods = emptyList(),
            defaultPaymentMethodId = null,
        ) as ElementsSessionParams.DeferredIntentType
        assertThat(params.deferredIntentParams.paymentMethodTypes).containsExactly("card")
    }

    @Test
    fun `when there is a payment selection, the selected PM should be first in the list`() = runTest {
        val loader = createCustomerSheetLoader(
            customerAdapter = FakeCustomerAdapter(
                paymentMethods = CustomerAdapter.Result.success(
                    listOf(
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = "pm_1"),
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = "pm_2"),
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = "pm_3"),
                    )
                ),
                selectedPaymentOption = CustomerAdapter.Result.success(
                    CustomerAdapter.PaymentOption.fromId("pm_3")
                )
            )
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
        )
        assertThat(state.customerPermissions.canRemovePaymentMethods).isTrue()
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
            customerAdapter = FakeCustomerAdapter(
                paymentMethods = CustomerAdapter.Result.success(
                    listOf(
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = "pm_1"),
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = "pm_2"),
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = "pm_3"),
                    )
                ),
                selectedPaymentOption = CustomerAdapter.Result.success(null)
            )
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
        )
        assertThat(state.customerPermissions.canRemovePaymentMethods).isTrue()
        assertThat(state.supportedPaymentMethods.map { it.code }).containsExactly("card")
        assertThat(state.paymentSelection).isNull()
        assertThat(state.paymentMethodMetadata.cbcEligibility).isEqualTo(CardBrandChoiceEligibility.Ineligible)
        assertThat(state.validationError).isNull()
    }

    @Test
    fun `When the FC unavailable, flag disabled, us bank not in intent, then us bank account is not available`() = runTest {
        val loader = createCustomerSheetLoader(
            customerAdapter = FakeCustomerAdapter(
                canCreateSetupIntents = true,
            ),
            isFinancialConnectionsAvailable = { false },
            elementsSessionRepository = FakeElementsSessionRepository(
                stripeIntent = STRIPE_INTENT.copy(
                    paymentMethodTypes = listOf("card")
                ),
                error = null,
                linkSettings = null,
            )
        )

        val config = CustomerSheet.Configuration(merchantDisplayName = "Example")

        assertThat(
            loader.load(config).getOrThrow().supportedPaymentMethods.map { it.code }
        ).doesNotContain("us_bank_account")
    }

    @Test
    fun `When the FC unavailable, flag disabled, us bank in intent, then us bank account is not available`() = runTest {
        val loader = createCustomerSheetLoader(
            customerAdapter = FakeCustomerAdapter(
                canCreateSetupIntents = true,
            ),
            isFinancialConnectionsAvailable = { false },
            elementsSessionRepository = FakeElementsSessionRepository(
                stripeIntent = STRIPE_INTENT.copy(
                    paymentMethodTypes = listOf("card", "us_bank_account")
                ),
                error = null,
                linkSettings = null,
            )
        )

        val config = CustomerSheet.Configuration(merchantDisplayName = "Example")

        assertThat(
            loader.load(config).getOrThrow().supportedPaymentMethods.map { it.code }
        ).doesNotContain("us_bank_account")
    }

    @Test
    fun `When the FC unavailable, flag enabled, us bank not in intent, then us bank account is not available`() = runTest {
        val loader = createCustomerSheetLoader(
            customerAdapter = FakeCustomerAdapter(
                canCreateSetupIntents = true,
            ),
            isFinancialConnectionsAvailable = { false },
            elementsSessionRepository = FakeElementsSessionRepository(
                stripeIntent = STRIPE_INTENT.copy(
                    paymentMethodTypes = listOf("card")
                ),
                error = null,
                linkSettings = null,
            )
        )

        val config = CustomerSheet.Configuration(merchantDisplayName = "Example")

        assertThat(
            loader.load(config).getOrThrow().supportedPaymentMethods.map { it.code }
        ).doesNotContain("us_bank_account")
    }

    @Test
    fun `When the FC unavailable, flag enabled, us bank in intent, then us bank account is not available`() = runTest {
        val loader = createCustomerSheetLoader(
            customerAdapter = FakeCustomerAdapter(
                canCreateSetupIntents = true,
            ),
            isFinancialConnectionsAvailable = { false },
            elementsSessionRepository = FakeElementsSessionRepository(
                stripeIntent = STRIPE_INTENT.copy(
                    paymentMethodTypes = listOf("card", "us_bank_account")
                ),
                error = null,
                linkSettings = null,
            )
        )

        val config = CustomerSheet.Configuration(merchantDisplayName = "Example")

        assertThat(
            loader.load(config).getOrThrow().supportedPaymentMethods.map { it.code }
        ).doesNotContain("us_bank_account")
    }

    @Test
    fun `When the FC available, flag disabled, us bank not in intent, then us bank account is not available`() = runTest {
        val loader = createCustomerSheetLoader(
            customerAdapter = FakeCustomerAdapter(
                canCreateSetupIntents = true,
            ),
            isFinancialConnectionsAvailable = { true },
            elementsSessionRepository = FakeElementsSessionRepository(
                stripeIntent = STRIPE_INTENT.copy(
                    paymentMethodTypes = listOf("card")
                ),
                error = null,
                linkSettings = null,
            )
        )

        val config = CustomerSheet.Configuration(merchantDisplayName = "Example")

        assertThat(
            loader.load(config).getOrThrow().supportedPaymentMethods.map { it.code }
        ).doesNotContain("us_bank_account")
    }

    @Test
    fun `When the FC available, flag disabled, us bank in intent, then us bank account is not available`() = runTest {
        val loader = createCustomerSheetLoader(
            customerAdapter = FakeCustomerAdapter(
                canCreateSetupIntents = true,
            ),
            isFinancialConnectionsAvailable = { true },
            elementsSessionRepository = FakeElementsSessionRepository(
                stripeIntent = STRIPE_INTENT.copy(
                    paymentMethodTypes = listOf("card", "us_bank_account")
                ),
                error = null,
                linkSettings = null,
            )
        )

        val config = CustomerSheet.Configuration(merchantDisplayName = "Example")

        assertThat(
            loader.load(config).getOrThrow().supportedPaymentMethods.map { it.code }
        ).doesNotContain("us_bank_account")
    }

    @Test
    fun `When the FC available, flag enabled, us bank not in intent, then us bank account is not available`() = runTest {
        val loader = createCustomerSheetLoader(
            customerAdapter = FakeCustomerAdapter(
                canCreateSetupIntents = true,
            ),
            isFinancialConnectionsAvailable = { true },
            elementsSessionRepository = FakeElementsSessionRepository(
                stripeIntent = STRIPE_INTENT.copy(
                    paymentMethodTypes = listOf("card")
                ),
                error = null,
                linkSettings = null,
            )
        )

        val config = CustomerSheet.Configuration(merchantDisplayName = "Example")

        assertThat(
            loader.load(config).getOrThrow().supportedPaymentMethods.map { it.code }
        ).doesNotContain("us_bank_account")
    }

    @Test
    fun `When the FC available, flag enabled, us bank in intent, then us bank account is available`() = runTest {
        val loader = createCustomerSheetLoader(
            customerAdapter = FakeCustomerAdapter(
                canCreateSetupIntents = true,
            ),
            isFinancialConnectionsAvailable = { true },
            elementsSessionRepository = FakeElementsSessionRepository(
                stripeIntent = STRIPE_INTENT.copy(
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
                error = null,
                linkSettings = null,
            )
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
        val initDataSource = CompletableDeferred<CustomerSheetInitializationDataSource>()

        val configuration = CustomerSheet.Configuration(merchantDisplayName = "Merchant, Inc.")
        val loader = DefaultCustomerSheetLoader(
            isLiveModeProvider = { false },
            googlePayRepositoryFactory = { readyGooglePayRepository },
            initializationDataSourceProvider = initDataSource,
            lpmRepository = lpmRepository,
            isFinancialConnectionsAvailable = { false },
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
            errorReporter = FakeErrorReporter(),
            workContext = coroutineContext,
        )

        val result = loader.load(configuration)

        assertThat(result.exceptionOrNull()).isInstanceOf<IllegalStateException>()
    }

    @Test
    fun `On failed to load elements session, show report error`() = runTest {
        val errorReporter = FakeErrorReporter()

        val loader = createCustomerSheetLoader(
            elementsSessionRepository = FakeElementsSessionRepository(
                stripeIntent = STRIPE_INTENT,
                error = APIConnectionException("Connection failure!"),
                linkSettings = null,
            ),
            errorReporter = errorReporter,
        )

        loader.load(
            configuration = CustomerSheet.Configuration(merchantDisplayName = "Merchant, Inc.")
        )

        assertThat(
            errorReporter.getLoggedErrors().first()
        ).isEqualTo(
            ErrorReporter.ExpectedErrorEvent.CUSTOMER_SHEET_ELEMENTS_SESSION_LOAD_FAILURE.eventName
        )
    }

    @Test
    fun `On failed to load payment methods, show report error`() = runTest {
        val errorReporter = FakeErrorReporter()

        val loader = createCustomerSheetLoader(
            customerAdapter = FakeCustomerAdapter(
                paymentMethods = CustomerAdapter.Result.failure(
                    cause = APIConnectionException("Connection failure!"),
                    displayMessage = null
                )
            ),
            errorReporter = errorReporter,
        )

        loader.load(
            configuration = CustomerSheet.Configuration(merchantDisplayName = "Merchant, Inc.")
        )

        assertThat(
            errorReporter.getLoggedErrors()
        ).containsExactly(
            ErrorReporter.SuccessEvent.CUSTOMER_SHEET_ELEMENTS_SESSION_LOAD_SUCCESS.eventName,
            ErrorReporter.ExpectedErrorEvent.CUSTOMER_SHEET_PAYMENT_METHODS_LOAD_FAILURE.eventName
        )
    }

    @Test
    fun `On timeout while waiting for 'CustomerAdapter' instance, show report error`() = runTest {
        val errorReporter = FakeErrorReporter()

        val loader = createCustomerSheetLoader(
            initializationDataSourceProvider = CompletableDeferred(),
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

    private fun createCustomerSheetLoader(
        isGooglePayReady: Boolean = true,
        isLiveModeProvider: () -> Boolean = { false },
        isCbcEligible: Boolean? = null,
        isFinancialConnectionsAvailable: IsFinancialConnectionsAvailable = IsFinancialConnectionsAvailable { false },
        elementsSessionRepository: ElementsSessionRepository = FakeElementsSessionRepository(
            stripeIntent = STRIPE_INTENT,
            error = null,
            linkSettings = null,
            cardBrandChoice = createCardBrandChoice(isCbcEligible),
        ),
        customerAdapter: CustomerAdapter = FakeCustomerAdapter(),
        lpmRepository: LpmRepository = this.lpmRepository,
        errorReporter: ErrorReporter = FakeErrorReporter(),
    ): CustomerSheetLoader {
        return createCustomerSheetLoader(
            initializationDataSourceProvider = CompletableDeferred(
                CustomerAdapterDataSource(
                    elementsSessionRepository = elementsSessionRepository,
                    workContext = UnconfinedTestDispatcher(),
                    customerAdapter = customerAdapter,
                    errorReporter = errorReporter,
                )
            ),
            isGooglePayReady = isGooglePayReady,
            isLiveModeProvider = isLiveModeProvider,
            isFinancialConnectionsAvailable = isFinancialConnectionsAvailable,
            lpmRepository = lpmRepository,
            errorReporter = errorReporter,
        )
    }

    private fun createCustomerSheetLoader(
        initializationDataSourceProvider: Deferred<CustomerSheetInitializationDataSource>,
        isGooglePayReady: Boolean = true,
        isLiveModeProvider: () -> Boolean = { false },
        isFinancialConnectionsAvailable: IsFinancialConnectionsAvailable = IsFinancialConnectionsAvailable { false },
        lpmRepository: LpmRepository = this.lpmRepository,
        errorReporter: ErrorReporter = FakeErrorReporter(),
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
