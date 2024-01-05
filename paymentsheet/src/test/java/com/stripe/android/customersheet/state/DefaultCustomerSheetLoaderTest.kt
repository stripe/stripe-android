package com.stripe.android.customersheet.state

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.customersheet.CustomerAdapter
import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.CustomerSheetLoader
import com.stripe.android.customersheet.CustomerSheetState
import com.stripe.android.customersheet.DefaultCustomerSheetLoader
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import com.stripe.android.customersheet.FakeCustomerAdapter
import com.stripe.android.customersheet.utils.CustomerSheetTestHelper
import com.stripe.android.googlepaylauncher.GooglePayRepository
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.payments.financialconnections.IsFinancialConnectionsAvailable
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.ElementsSessionRepository
import com.stripe.android.testing.FeatureFlagTestRule
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.utils.FakeElementsSessionRepository
import com.stripe.android.utils.FeatureFlags
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCustomerSheetApi::class)
@RunWith(RobolectricTestRunner::class)
class DefaultCustomerSheetLoaderTest {
    @get:Rule
    val achFeatureFlagTestRule = FeatureFlagTestRule(
        featureFlag = FeatureFlags.customerSheetACHv2,
        isEnabled = false,
    )

    private val lpmRepository = LpmRepository(
        arguments = LpmRepository.LpmRepositoryArguments(
            resources = ApplicationProvider.getApplicationContext<Application>().resources,
        ),
        lpmInitialFormData = LpmRepository.LpmInitialFormData(),
    ).apply {
        this.update(
            PaymentIntentFactory.create(
                paymentMethodTypes = listOf(
                    PaymentMethod.Type.Card.code,
                    PaymentMethod.Type.USBankAccount.code,
                ),
            ),
            null
        )
    }

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

        assertThat(
            loader.load(config).getOrThrow()
        ).isEqualTo(
            CustomerSheetState.Full(
                config = config,
                stripeIntent = STRIPE_INTENT,
                customerPaymentMethods = listOf(
                    PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                    PaymentMethodFixtures.US_BANK_ACCOUNT,
                ),
                supportedPaymentMethods = listOf(
                    LpmRepository.HardcodedCard,
                ),
                isGooglePayReady = true,
                paymentSelection = PaymentSelection.Saved(
                    paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                ),
                cbcEligibility = CardBrandChoiceEligibility.Ineligible,
            )
        )

        val mode = elementsSessionRepository.lastGetParam as PaymentSheet.InitializationMode.DeferredIntent
        assertThat(mode.intentConfiguration.paymentMethodTypes)
            .isEmpty()
    }

    @Test
    fun `when setup intent cannot be created, elements session is null`() = runTest {
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

        assertThat(
            loader.load(config).getOrThrow().stripeIntent
        ).isNull()
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

        assertThat(
            loader.load(config).getOrThrow()
        ).isEqualTo(
            CustomerSheetState.Full(
                config = config,
                stripeIntent = null,
                customerPaymentMethods = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
                supportedPaymentMethods = listOf(
                    LpmRepository.HardcodedCard,
                ),
                isGooglePayReady = false,
                paymentSelection = null,
                cbcEligibility = CardBrandChoiceEligibility.Ineligible,
            )
        )
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

        assertThat(
            loader.load(config).getOrThrow()
        ).isEqualTo(
            CustomerSheetState.Full(
                config = config,
                stripeIntent = STRIPE_INTENT,
                customerPaymentMethods = listOf(
                    PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = "pm_3"),
                    PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = "pm_1"),
                    PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = "pm_2"),
                ),
                supportedPaymentMethods = listOf(
                    LpmRepository.HardcodedCard,
                ),
                isGooglePayReady = false,
                paymentSelection = PaymentSelection.Saved(
                    PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = "pm_3")
                ),
                cbcEligibility = CardBrandChoiceEligibility.Ineligible,
            )
        )
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

        assertThat(
            loader.load(config).getOrThrow()
        ).isEqualTo(
            CustomerSheetState.Full(
                config = config,
                stripeIntent = STRIPE_INTENT,
                customerPaymentMethods = listOf(
                    PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = "pm_1"),
                    PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = "pm_2"),
                    PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = "pm_3"),
                ),
                supportedPaymentMethods = listOf(
                    LpmRepository.HardcodedCard,
                ),
                isGooglePayReady = false,
                paymentSelection = null,
                cbcEligibility = CardBrandChoiceEligibility.Ineligible,
            )
        )
    }

    @Test
    fun `LPM repository is initialized with the necessary payment methods`() = runTest {
        val lpmRepository = LpmRepository(
            arguments = LpmRepository.LpmRepositoryArguments(
                resources = CustomerSheetTestHelper.application.resources,
                isFinancialConnectionsAvailable = { true },
            ),
            lpmInitialFormData = LpmRepository.LpmInitialFormData()
        )

        var card = lpmRepository.fromCode("card")
        assertThat(card).isNull()

        val loader = createCustomerSheetLoader(
            lpmRepository = lpmRepository,
        )
        loader.load(CustomerSheet.Configuration(merchantDisplayName = "Example"))

        card = lpmRepository.fromCode("card")
        assertThat(card).isNotNull()
    }

    @Test
    fun `When the FC unavailable, flag disabled, us bank not in intent, then us bank account is not available`() = runTest {
        achFeatureFlagTestRule.setEnabled(false)

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
            loader.load(config).getOrThrow().supportedPaymentMethods
        ).doesNotContain(LpmRepository.hardCodedUsBankAccount)
    }

    @Test
    fun `When the FC unavailable, flag disabled, us bank in intent, then us bank account is not available`() = runTest {
        achFeatureFlagTestRule.setEnabled(false)

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
            loader.load(config).getOrThrow().supportedPaymentMethods
        ).doesNotContain(LpmRepository.hardCodedUsBankAccount)
    }

    @Test
    fun `When the FC unavailable, flag enabled, us bank not in intent, then us bank account is not available`() = runTest {
        achFeatureFlagTestRule.setEnabled(true)

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
            loader.load(config).getOrThrow().supportedPaymentMethods
        ).doesNotContain(LpmRepository.hardCodedUsBankAccount)
    }

    @Test
    fun `When the FC unavailable, flag enabled, us bank in intent, then us bank account is not available`() = runTest {
        achFeatureFlagTestRule.setEnabled(true)

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
            loader.load(config).getOrThrow().supportedPaymentMethods
        ).doesNotContain(LpmRepository.hardCodedUsBankAccount)
    }

    @Test
    fun `When the FC available, flag disabled, us bank not in intent, then us bank account is not available`() = runTest {
        achFeatureFlagTestRule.setEnabled(false)

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
            loader.load(config).getOrThrow().supportedPaymentMethods
        ).doesNotContain(LpmRepository.hardCodedUsBankAccount)
    }

    @Test
    fun `When the FC available, flag disabled, us bank in intent, then us bank account is not available`() = runTest {
        achFeatureFlagTestRule.setEnabled(false)

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
            loader.load(config).getOrThrow().supportedPaymentMethods
        ).doesNotContain(LpmRepository.hardCodedUsBankAccount)
    }

    @Test
    fun `When the FC available, flag enabled, us bank not in intent, then us bank account is not available`() = runTest {
        achFeatureFlagTestRule.setEnabled(true)

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
            loader.load(config).getOrThrow().supportedPaymentMethods
        ).doesNotContain(LpmRepository.hardCodedUsBankAccount)
    }

    @Test
    fun `When the FC available, flag enabled, us bank in intent, then us bank account is available`() = runTest {
        achFeatureFlagTestRule.setEnabled(true)

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
            loader.load(config).getOrThrow().supportedPaymentMethods
        ).contains(LpmRepository.hardCodedUsBankAccount)
    }

    @Test
    fun `Loads correct CBC eligibility`() = runTest {
        val loader = createCustomerSheetLoader(isCbcEligible = true)
        val state = loader.load(CustomerSheet.Configuration(merchantDisplayName = "Example")).getOrThrow()
        assertThat(state.cbcEligibility).isEqualTo(CardBrandChoiceEligibility.Eligible(emptyList()))
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

        assertThat(state.cbcEligibility).isEqualTo(
            CardBrandChoiceEligibility.Eligible(preferredNetworks = listOf(CardBrand.CartesBancaires))
        )
    }

    private fun createCustomerSheetLoader(
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
    ): CustomerSheetLoader {
        return DefaultCustomerSheetLoader(
            isLiveModeProvider = isLiveModeProvider,
            googlePayRepositoryFactory = {
                if (isGooglePayReady) readyGooglePayRepository else unreadyGooglePayRepository
            },
            elementsSessionRepository = elementsSessionRepository,
            lpmRepository = lpmRepository,
            customerAdapter = customerAdapter,
            isFinancialConnectionsAvailable = isFinancialConnectionsAvailable,
        )
    }

    private companion object {
        private val STRIPE_INTENT = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
            paymentMethodTypes = listOf("card", "us_bank_account")
        )
    }
}
