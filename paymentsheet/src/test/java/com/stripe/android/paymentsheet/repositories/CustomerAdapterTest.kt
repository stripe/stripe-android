package com.stripe.android.paymentsheet.repositories

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ExperimentalSavedPaymentMethodsApi
import com.stripe.android.PaymentConfiguration
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
            setupIntentClientSecretProvider = setupIntentClientSecretProvider,
            canCreateSetupIntents = true
        )

        assertThat(adapter).isNotNull()
    }
}
