package com.stripe.android.paymentsheet

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.PaymentSessionPrefs
import com.stripe.android.model.ListPaymentMethodsParams
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.networking.AbsFakeStripeRepository
import com.stripe.android.networking.ApiRequest
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
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val testDispatcher = TestCoroutineDispatcher()

    private val factory = PaymentSheetFlowControllerFactory(
        context,
        FakeStripeRepository(),
        ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY,
        null,
        FakePaymentSessionPrefs(),
        testDispatcher
    )

    @BeforeTest
    fun before() {
        PaymentConfiguration.init(context, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)

        Dispatchers.setMain(testDispatcher)
    }

    @Test
    fun `create() with customer config should create object with expected properties`() {
        var result: PaymentSheetFlowController.Result? = null
        factory.create(
            "client_secret",
            PaymentSheet.Configuration(
                customer = PaymentSheet.CustomerConfiguration(
                    id = "cus_123",
                    ephemeralKeySecret = "eph_key"
                )
            )
        ) {
            result = it
        }

        val successResult =
            result as PaymentSheetFlowController.Result.Success
        val flowController =
            successResult.paymentSheetFlowController as DefaultPaymentSheetFlowController

        assertThat(flowController.paymentMethodTypes)
            .containsExactly(PaymentMethod.Type.Card)
        assertThat(flowController.paymentMethods)
            .hasSize(1)
    }

    private class FakePaymentSessionPrefs : PaymentSessionPrefs {
        override fun getPaymentMethodId(customerId: String?): String? = "pm_123"

        override fun savePaymentMethodId(customerId: String, paymentMethodId: String?) {
        }
    }

    private class FakeStripeRepository : AbsFakeStripeRepository() {
        override suspend fun retrievePaymentIntent(
            clientSecret: String,
            options: ApiRequest.Options,
            expandFields: List<String>
        ) = PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2

        override suspend fun getPaymentMethods(
            listPaymentMethodsParams: ListPaymentMethodsParams,
            publishableKey: String,
            productUsageTokens: Set<String>,
            requestOptions: ApiRequest.Options
        ): List<PaymentMethod> {
            return listOf(
                PaymentMethodFixtures.createPaymentMethod(
                    listPaymentMethodsParams.paymentMethodType
                )
            )
        }
    }
}
