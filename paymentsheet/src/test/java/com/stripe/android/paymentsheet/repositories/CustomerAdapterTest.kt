package com.stripe.android.paymentsheet.repositories

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ExperimentalSavedPaymentMethodsApi
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.FakePrefsRepository
import com.stripe.android.paymentsheet.repositories.StripeCustomerAdapter.Companion.CACHED_CUSTOMER_MAX_AGE_MILLIS
import com.stripe.android.utils.FakeCustomerRepository
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalSavedPaymentMethodsApi::class)
class CustomerAdapterTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setup() {
        PaymentConfiguration.init(
            context = context,
            publishableKey = "pk_123",
        )
    }

    @Test
    fun `CustomerAdapter can be created`() {
        val customerEphemeralKeyProvider = CustomerEphemeralKeyProvider {
            Result.success(
                CustomerEphemeralKey(
                    customerId = "cus_123",
                    ephemeralKey = "ek_123"
                )
            )
        }

        val setupIntentClientSecretProvider = SetupIntentClientSecretProvider {
            Result.success("seti_123")
        }

        val adapter = CustomerAdapter.create(
            context = context,
            customerEphemeralKeyProvider = customerEphemeralKeyProvider,
            setupIntentClientSecretProvider = setupIntentClientSecretProvider
        )

        assertThat(adapter).isNotNull()
    }

    @Test
    fun `CustomerEphemeralKey is cached`() = runTest {
        val adapter = createAdapter(
            customerEphemeralKeyProvider = {
                Result.success(
                    CustomerEphemeralKey(
                        customerId = testScheduler.currentTime.toString(),
                        ephemeralKey = "ek_123"
                    )
                )
            },
            timeProvider = { testScheduler.currentTime }
        )
        assertThat(adapter.cachedCustomer).isNull()

        var customer = adapter.getCustomer()
        assertThat(customer.getOrNull()?.customerId).isEqualTo("0")
        assertThat(adapter.cachedCustomer).isNotNull()

        customer = adapter.getCustomer()
        assertThat(customer.getOrNull()?.customerId).isEqualTo("0")
    }

    @Test
    fun `CustomerEphemeralKey is expired and updated`() = runTest {
        val adapter = createAdapter(
            customerEphemeralKeyProvider = {
                Result.success(
                    CustomerEphemeralKey(
                        customerId = testScheduler.currentTime.toString(),
                        ephemeralKey = "ek_123"
                    )
                )
            },
            timeProvider = { testScheduler.currentTime }
        )
        assertThat(adapter.cachedCustomer).isNull()

        var customer = adapter.getCustomer()
        assertThat(customer.getOrNull()?.customerId).isEqualTo("0")
        assertThat(adapter.cachedCustomer).isNotNull()

        advanceTimeBy(CACHED_CUSTOMER_MAX_AGE_MILLIS)
        customer = adapter.getCustomer()
        assertThat(customer.getOrNull()?.customerId)
            .isEqualTo("0")

        advanceTimeBy(1)
        customer = adapter.getCustomer()
        assertThat(customer.getOrNull()?.customerId)
            .isEqualTo("${CACHED_CUSTOMER_MAX_AGE_MILLIS + 1}")
    }

    @Test
    fun `retrievePaymentMethods returns success`() = runTest {
        val adapter = createAdapter(
            customerRepository = FakeCustomerRepository(
                paymentMethods = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
            )
        )
        val paymentMethods = adapter.retrievePaymentMethods()
        assertThat(
            paymentMethods.getOrNull()
        ).contains(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
    }

    @Test
    fun `retrievePaymentMethods returns failure when customer is not fetched`() = runTest {
        val error = Result.failure<CustomerEphemeralKey>(Exception("Cannot get customer"))
        val adapter = createAdapter(
            customerEphemeralKeyProvider = { error }
        )
        val paymentMethods = adapter.retrievePaymentMethods()
        assertThat(paymentMethods).isEqualTo(error)
    }

    @Test
    fun `attachPaymentMethod succeeds`() = runTest {
        val adapter = createAdapter(
            customerRepository = FakeCustomerRepository(
                onAttachPaymentMethod = {
                    Result.success(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
                }
            )
        )
        val result = adapter.attachPaymentMethod("pm_1234")
        assertThat(result.getOrNull()).isNotNull()
    }

    @Test
    fun `attachPaymentMethod throws`() = runTest {
        val adapter = createAdapter(
            customerRepository = FakeCustomerRepository(
                onAttachPaymentMethod = {
                    Result.failure(Exception("could not attach payment method"))
                }
            )
        )
        val result = adapter.attachPaymentMethod("pm_1234")
        assertThat(result.isFailure).isTrue()
    }

    private fun createAdapter(
        customerEphemeralKeyProvider: CustomerEphemeralKeyProvider =
            CustomerEphemeralKeyProvider {
                Result.success(
                    CustomerEphemeralKey(
                        customerId = "cus_123",
                        ephemeralKey = "ek_123"
                    )
                )
            },
        setupIntentClientSecretProvider: SetupIntentClientSecretProvider =
            SetupIntentClientSecretProvider {
                Result.success("seti_123")
            },
        timeProvider: () -> Long = { 1L },
        customerRepository: CustomerRepository = FakeCustomerRepository(),
    ): StripeCustomerAdapter {
        return StripeCustomerAdapter(
            context = context,
            customerEphemeralKeyProvider = customerEphemeralKeyProvider,
            setupIntentClientSecretProvider = setupIntentClientSecretProvider,
            timeProvider = timeProvider,
            customerRepository = customerRepository,
            prefsRepositoryFactory = { FakePrefsRepository() }
        )
    }
}
