package com.stripe.android.paymentsheet

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentSessionPrefs
import com.stripe.android.networking.AbsFakeStripeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.setMain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.BeforeTest
import kotlin.test.Test

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class PaymentSheetFlowControllerFactoryTest {
    private val testDispatcher = TestCoroutineDispatcher()

    private val factory = PaymentSheetFlowControllerFactory(
        ApplicationProvider.getApplicationContext(),
        FakeStripeRepository(),
        ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY,
        null,
        FakePaymentSessionPrefs(),
        testDispatcher
    )

    @BeforeTest
    fun before() {
        Dispatchers.setMain(testDispatcher)
    }

    @Test
    fun `create() in default mode should create expected object`() {
        var result: PaymentSheetFlowController.Result? = null
        factory.create(
            "client_secret",
            "eph_key",
            "cus_123"
        ) {
            result = it
        }

        assertThat(result)
            .isInstanceOf(PaymentSheetFlowController.Result::class.java)
    }

    private class FakePaymentSessionPrefs : PaymentSessionPrefs {
        override fun getPaymentMethodId(customerId: String?): String? = "pm_123"

        override fun savePaymentMethodId(customerId: String, paymentMethodId: String?) {
        }
    }

    private class FakeStripeRepository : AbsFakeStripeRepository()
}
