package com.stripe.android.paymentsheet.stripebalance

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.model.PaymentMethodRemovePermission
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.SetupIntentFactory
import com.stripe.android.utils.FakeDurationProvider
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class DefaultLoadStripeBalanceFinancialAccountsTest {

    @Test
    fun `loads the buyer's financial accounts with the CustomerSession`() = runScenario {
        val accounts = listOf(ACCOUNT)
        repository.result = { Result.success(accounts) }

        val result = load()

        assertThat(result).isEqualTo(StripeBalanceFinancialAccounts(accounts = accounts, currency = "usd"))
        assertThat(repository.listFinancialAccountsCalls.awaitItem()).isEqualTo(
            FakeStripeBalanceRepository.ListFinancialAccountsCall(
                customerSessionClientSecret = "cuss_123_secret_456",
                ephemeralKeySecret = "ek_123",
                amount = 5099,
                currency = "usd",
            )
        )
        assertFetchTimed()
    }

    @Test
    fun `doesn't load when Stripe balance isn't offered`() = runScenario {
        val result = load(stripeIntent = PAYMENT_INTENT.copy(paymentMethodTypes = listOf("card")))

        assertThat(result).isNull()
    }

    @Test
    fun `doesn't load without a CustomerSession`() = runScenario {
        val result = load(customerMetadata = LEGACY_EPHEMERAL_KEY_METADATA)

        assertThat(result).isNull()
    }

    @Test
    fun `loads without an amount or currency when setting up`() = runScenario {
        repository.result = { Result.success(listOf(ACCOUNT)) }

        val result = load(stripeIntent = SetupIntentFactory.create(paymentMethodTypes = listOf("stripe_balance")))

        assertThat(result).isEqualTo(StripeBalanceFinancialAccounts(accounts = listOf(ACCOUNT), currency = null))
        assertThat(repository.listFinancialAccountsCalls.awaitItem()).isEqualTo(
            FakeStripeBalanceRepository.ListFinancialAccountsCall(
                customerSessionClientSecret = "cuss_123_secret_456",
                ephemeralKeySecret = "ek_123",
                amount = null,
                currency = null,
            )
        )
        assertFetchTimed()
    }

    @Test
    fun `reports that Checkout Sessions aren't supported`() = runScenario {
        val result = load(
            initializationMode = PaymentElementLoader.InitializationMode.CheckoutSession(
                instancesKey = "instances_key",
                checkoutSessionResponse = CheckoutSessionResponseFactory.create(id = "cs_test_123"),
                collectedEmail = null,
                configuredElements = emptyList(),
            ),
        )

        assertThat(result).isNull()
        assertThat(errorReporter.awaitCall().errorEvent)
            .isEqualTo(ErrorReporter.UnexpectedErrorEvent.STRIPE_BALANCE_CHECKOUT_SESSION_UNSUPPORTED)
    }

    @Test
    fun `reports and returns null when the accounts fail to load`() = runScenario {
        repository.result = { Result.failure(IllegalStateException("Failed")) }

        val result = load()

        assertThat(result).isNull()
        repository.listFinancialAccountsCalls.awaitItem()
        assertFetchTimed()
        assertThat(errorReporter.awaitCall().errorEvent)
            .isEqualTo(ErrorReporter.ExpectedErrorEvent.STRIPE_BALANCE_FINANCIAL_ACCOUNTS_LOAD_FAILURE)
    }

    @Test
    fun `reports and returns null when the accounts take too long to load`() = runScenario {
        repository.result = { awaitCancellation() }

        val result = load()

        assertThat(result).isNull()
        repository.listFinancialAccountsCalls.awaitItem()
        assertFetchTimed()
        assertThat(errorReporter.awaitCall().errorEvent)
            .isEqualTo(ErrorReporter.ExpectedErrorEvent.STRIPE_BALANCE_FINANCIAL_ACCOUNTS_LOAD_FAILURE)
    }

    private fun runScenario(
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val repository = FakeStripeBalanceRepository()
        val errorReporter = FakeErrorReporter()
        val durationProvider = FakeDurationProvider()
        Scenario(
            loadStripeBalanceFinancialAccounts = DefaultLoadStripeBalanceFinancialAccounts(
                stripeBalanceRepository = repository,
                errorReporter = errorReporter,
                durationProvider = durationProvider,
            ),
            repository = repository,
            errorReporter = errorReporter,
            durationProvider = durationProvider,
        ).block()
        repository.ensureAllEventsConsumed()
        errorReporter.ensureAllEventsConsumed()
        durationProvider.validate()
    }

    private class Scenario(
        val loadStripeBalanceFinancialAccounts: DefaultLoadStripeBalanceFinancialAccounts,
        val repository: FakeStripeBalanceRepository,
        val errorReporter: FakeErrorReporter,
        val durationProvider: FakeDurationProvider,
    ) {
        suspend fun assertFetchTimed() {
            val key = DurationProvider.Key.PaymentSheetLoadFetchStripeBalanceFinancialAccounts
            assertThat(durationProvider.callsTurbine.awaitItem()).isEqualTo(FakeDurationProvider.Call.Start(key, true))
            assertThat(durationProvider.callsTurbine.awaitItem()).isEqualTo(FakeDurationProvider.Call.End(key))
        }

        suspend fun load(
            initializationMode: PaymentElementLoader.InitializationMode =
                PaymentElementLoader.InitializationMode.PaymentIntent(clientSecret = "pi_123_secret_456"),
            stripeIntent: StripeIntent = PAYMENT_INTENT,
            customerMetadata: CustomerMetadata = CUSTOMER_SESSION_METADATA,
        ): StripeBalanceFinancialAccounts? {
            return loadStripeBalanceFinancialAccounts(
                initializationMode = initializationMode,
                elementsSession = createElementsSession(stripeIntent),
                customerMetadata = customerMetadata,
                apiConfiguration = PaymentMethodMetadataFixtures.DEFAULT_API_CONFIG,
            )
        }
    }

    private companion object {
        val PAYMENT_INTENT = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
            paymentMethodTypes = listOf("card", "stripe_balance"),
            amount = 5099,
            currency = "usd",
        )

        val ACCOUNT = StripeBalanceFinancialAccount(
            id = "fa_123",
            displayName = "Operating account",
            availableBalances = mapOf("usd" to 10_000L),
            eligible = true,
            ineligibleReason = null,
        )

        val CUSTOMER_SESSION_METADATA = CustomerMetadata.CustomerSession(
            id = "cus_123",
            ephemeralKeySecret = "ek_123",
            customerSessionClientSecret = "cuss_123_secret_456",
            isPaymentMethodSetAsDefaultEnabled = false,
            removePaymentMethod = PaymentMethodRemovePermission.None,
            saveConsent = PaymentMethodSaveConsentBehavior.Disabled(overrideAllowRedisplay = null),
            canRemoveLastPaymentMethod = false,
            canUpdateCardExpiryAndBillingDetails = false,
        )

        val LEGACY_EPHEMERAL_KEY_METADATA = CustomerMetadata.LegacyEphemeralKey(
            id = "cus_123",
            ephemeralKeySecret = "ek_123",
            isPaymentMethodSetAsDefaultEnabled = false,
            removePaymentMethod = PaymentMethodRemovePermission.Full,
            saveConsent = PaymentMethodSaveConsentBehavior.Legacy,
            canRemoveLastPaymentMethod = true,
            canUpdateCardExpiryAndBillingDetails = false,
        )

        fun createElementsSession(stripeIntent: StripeIntent): ElementsSession {
            return ElementsSession(
                stripeIntent = stripeIntent,
                cardBrandChoice = null,
                merchantCountry = null,
                isGooglePayEnabled = false,
                customer = null,
                linkSettings = null,
                orderedPaymentMethodTypesAndWallets = stripeIntent.paymentMethodTypes,
                customPaymentMethods = emptyList(),
                externalPaymentMethodData = null,
                elementsSessionId = "session_1234",
                flags = emptyMap(),
                experimentsData = null,
                passiveCaptcha = null,
                merchantLogoUrl = null,
                elementsSessionConfigId = null,
                accountId = "acct_123",
                merchantId = "acct_123",
            )
        }
    }
}
