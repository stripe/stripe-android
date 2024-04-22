package com.stripe.android.customersheet.state

import android.app.Application
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.customersheet.CustomerAdapter
import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.CustomerSheetLoader
import com.stripe.android.customersheet.DefaultCustomerSheetLoader
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import com.stripe.android.customersheet.FakeCustomerAdapter
import com.stripe.android.customersheet.util.CustomerSheetHacks
import com.stripe.android.googlepaylauncher.GooglePayRepository
import com.stripe.android.lpmfoundations.luxe.LpmRepository
import com.stripe.android.model.CardBrand
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
import com.stripe.android.uicore.address.AddressRepository
import com.stripe.android.utils.FakeElementsSessionRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration.Companion.milliseconds

@Suppress("LargeClass")
@OptIn(ExperimentalCustomerSheetApi::class)
@RunWith(AndroidJUnit4::class)
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
        assertThat(state.customerPaymentMethods).containsExactly(
            PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            PaymentMethodFixtures.US_BANK_ACCOUNT,
        )
        assertThat(state.supportedPaymentMethods.map { it.code }).containsExactly("card")
        assertThat(state.isGooglePayReady).isTrue()
        assertThat(state.paymentSelection).isEqualTo(
            PaymentSelection.Saved(
                paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            )
        )
        assertThat(state.validationError).isNull()

        val mode = elementsSessionRepository.lastGetParam as PaymentSheet.InitializationMode.DeferredIntent
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
        assertThat(state.customerPaymentMethods).containsExactly(
            PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            PaymentMethodFixtures.US_BANK_ACCOUNT,
        )
        assertThat(state.supportedPaymentMethods.map { it.code }).containsExactly("card")
        assertThat(state.isGooglePayReady).isTrue()
        assertThat(state.paymentSelection).isEqualTo(
            PaymentSelection.Saved(
                paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            )
        )
        assertThat(state.paymentMethodMetadata.cbcEligibility).isEqualTo(CardBrandChoiceEligibility.Ineligible)
        assertThat(state.validationError).isNull()

        val mode = elementsSessionRepository.lastGetParam as PaymentSheet.InitializationMode.DeferredIntent
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
        assertThat(state.customerPaymentMethods).containsExactly(
            PaymentMethodFixtures.CARD_PAYMENT_METHOD,
        )
        assertThat(state.supportedPaymentMethods.map { it.code }).containsExactly("card")
        assertThat(state.isGooglePayReady).isFalse()
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
            isCbcEligible = false,
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
        val params = elementsSessionRepository.lastGetParam?.toElementsSessionParams(externalPaymentMethods = null)
            as ElementsSessionParams.DeferredIntentType
        assertThat(params.deferredIntentParams.paymentMethodTypes).containsExactly("card")
    }

    @Test
    fun `when setup intent cannot be created, elements sessions is called with card only when paymentMethodTypes is set`() = runTest {
        val elementsSessionRepository = FakeElementsSessionRepository(
            stripeIntent = STRIPE_INTENT,
            error = null,
            linkSettings = null,
            isCbcEligible = false,
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
        val params = elementsSessionRepository.lastGetParam?.toElementsSessionParams(externalPaymentMethods = null)
            as ElementsSessionParams.DeferredIntentType
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
        assertThat(state.customerPaymentMethods).containsExactly(
            PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = "pm_3"),
            PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = "pm_1"),
            PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = "pm_2"),
        )
        assertThat(state.supportedPaymentMethods.map { it.code }).containsExactly("card")
        assertThat(state.isGooglePayReady).isFalse()
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
        assertThat(state.customerPaymentMethods).containsExactly(
            PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = "pm_1"),
            PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = "pm_2"),
            PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = "pm_3"),
        )
        assertThat(state.supportedPaymentMethods.map { it.code }).containsExactly("card")
        assertThat(state.isGooglePayReady).isFalse()
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
    fun `Awaits CustomerAdapter if CustomerAdapter is provided after loader starts loading`() = runTest {
        val configuration = CustomerSheet.Configuration(merchantDisplayName = "Merchant, Inc.")
        val loader = DefaultCustomerSheetLoader(
            isLiveModeProvider = { false },
            googlePayRepositoryFactory = { readyGooglePayRepository },
            elementsSessionRepository = FakeElementsSessionRepository(
                stripeIntent = STRIPE_INTENT,
                error = null,
                linkSettings = null,
                isCbcEligible = false,
            ),
            lpmRepository = lpmRepository,
            addressRepository = createAddressRepository(),
            isFinancialConnectionsAvailable = { false },
            errorReporter = FakeErrorReporter(),
        )

        val completable = CompletableDeferred<Unit>()

        launch {
            loader.load(configuration)
            completable.complete(Unit)
        }

        assertThat(completable.isCompleted).isFalse()

        CustomerSheetHacks.initialize(
            lifecycleOwner = TestLifecycleOwner(),
            adapter = FakeCustomerAdapter(),
            configuration = configuration,
        )

        withTimeout(100.milliseconds) {
            completable.await()
        }
    }

    @Test
    fun `Fails if awaiting CustomerAdapter times out`() = runTest {
        val configuration = CustomerSheet.Configuration(merchantDisplayName = "Merchant, Inc.")
        val loader = DefaultCustomerSheetLoader(
            isLiveModeProvider = { false },
            googlePayRepositoryFactory = { readyGooglePayRepository },
            elementsSessionRepository = FakeElementsSessionRepository(
                stripeIntent = STRIPE_INTENT,
                error = null,
                linkSettings = null,
                isCbcEligible = false,
            ),
            lpmRepository = lpmRepository,
            addressRepository = createAddressRepository(),
            isFinancialConnectionsAvailable = { false },
            errorReporter = FakeErrorReporter(),
        )

        val result = loader.load(configuration)

        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `On failed to load elements session, show report error`() = runTest {
        val errorReporter = FakeErrorReporter()

        val loader = createCustomerSheetLoader(
            elementsSessionRepository = FakeElementsSessionRepository(
                stripeIntent = STRIPE_INTENT,
                error = APIConnectionException("Connection failure!"),
                linkSettings = null,
                isCbcEligible = false,
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
            errorReporter.getLoggedErrors().first()
        ).isEqualTo(
            ErrorReporter.ExpectedErrorEvent.CUSTOMER_SHEET_PAYMENT_METHODS_LOAD_FAILURE.eventName
        )
    }

    @Test
    fun `On timeout while waiting for 'CustomerAdapter' instance, show report error`() = runTest {
        val errorReporter = FakeErrorReporter()

        val loader = createCustomerSheetLoader(
            customerAdapterProvider = CompletableDeferred(),
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

    private suspend fun createCustomerSheetLoader(
        isGooglePayReady: Boolean = true,
        isLiveModeProvider: () -> Boolean = { false },
        isCbcEligible: Boolean = false,
        isFinancialConnectionsAvailable: IsFinancialConnectionsAvailable = IsFinancialConnectionsAvailable { false },
        elementsSessionRepository: ElementsSessionRepository = FakeElementsSessionRepository(
            stripeIntent = STRIPE_INTENT,
            error = null,
            linkSettings = null,
            isCbcEligible = isCbcEligible,
        ),
        customerAdapter: CustomerAdapter = FakeCustomerAdapter(),
        lpmRepository: LpmRepository = this.lpmRepository,
        errorReporter: ErrorReporter = FakeErrorReporter(),
    ): CustomerSheetLoader {
        return createCustomerSheetLoader(
            customerAdapterProvider = CompletableDeferred(customerAdapter),
            isGooglePayReady = isGooglePayReady,
            isLiveModeProvider = isLiveModeProvider,
            isCbcEligible = isCbcEligible,
            isFinancialConnectionsAvailable = isFinancialConnectionsAvailable,
            elementsSessionRepository = elementsSessionRepository,
            lpmRepository = lpmRepository,
            errorReporter = errorReporter,
        )
    }

    private suspend fun createCustomerSheetLoader(
        customerAdapterProvider: Deferred<CustomerAdapter>,
        isGooglePayReady: Boolean = true,
        isLiveModeProvider: () -> Boolean = { false },
        isCbcEligible: Boolean = false,
        isFinancialConnectionsAvailable: IsFinancialConnectionsAvailable = IsFinancialConnectionsAvailable { false },
        elementsSessionRepository: ElementsSessionRepository = FakeElementsSessionRepository(
            stripeIntent = STRIPE_INTENT,
            error = null,
            linkSettings = null,
            isCbcEligible = isCbcEligible,
        ),
        lpmRepository: LpmRepository = this.lpmRepository,
        errorReporter: ErrorReporter = FakeErrorReporter(),
    ): CustomerSheetLoader {
        return DefaultCustomerSheetLoader(
            isLiveModeProvider = isLiveModeProvider,
            googlePayRepositoryFactory = {
                if (isGooglePayReady) readyGooglePayRepository else unreadyGooglePayRepository
            },
            elementsSessionRepository = elementsSessionRepository,
            lpmRepository = lpmRepository,
            isFinancialConnectionsAvailable = isFinancialConnectionsAvailable,
            customerAdapterProvider = customerAdapterProvider,
            addressRepository = createAddressRepository(),
            errorReporter = errorReporter,
        )
    }

    private suspend fun createAddressRepository(): AddressRepository {
        return AddressRepository(
            resources = ApplicationProvider.getApplicationContext<Application>().resources,
            workContext = coroutineContext,
        )
    }

    private companion object {
        private val STRIPE_INTENT = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
            paymentMethodTypes = listOf("card", "us_bank_account")
        )
    }
}
