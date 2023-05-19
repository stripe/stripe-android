package com.stripe.android.paymentsheet.repositories

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ExperimentalSavedPaymentMethodsApi
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.FakePrefsRepository
import com.stripe.android.paymentsheet.repositories.StripeCustomerAdapter.Companion.CACHED_CUSTOMER_MAX_AGE_MILLIS
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalSavedPaymentMethodsApi::class)
class CustomerAdapterTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val customerRepository = mock<CustomerRepository>()

    @Before
    fun setup() {
        PaymentConfiguration.init(
            context = context,
            publishableKey = "pk_123",
        )
    }

    @Test
    fun `CustomerAdapter can be created`() {
        val adapter = createAdapter()
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

        advanceTimeBy(CACHED_CUSTOMER_MAX_AGE_MILLIS + 1)
        customer = adapter.getCustomer()
        assertThat(customer.getOrNull()?.customerId)
            .isEqualTo("${CACHED_CUSTOMER_MAX_AGE_MILLIS + 1}")
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
        canCreateSetupIntents: Boolean = true,
        timeProvider: () -> Long = { 1L }
    ): StripeCustomerAdapter {
        return StripeCustomerAdapter(
            context = context,
            customerEphemeralKeyProvider = customerEphemeralKeyProvider,
            setupIntentClientSecretProvider = setupIntentClientSecretProvider,
            canCreateSetupIntents = canCreateSetupIntents,
            timeProvider = timeProvider,
            customerRepository = customerRepository,
            prefsRepositoryFactory = { FakePrefsRepository() }
        )
    }
}
