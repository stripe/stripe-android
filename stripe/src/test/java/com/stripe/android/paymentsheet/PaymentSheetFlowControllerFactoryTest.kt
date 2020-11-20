package com.stripe.android.paymentsheet

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
    private val testDispatcher = TestCoroutineDispatcher()

    private val factory = PaymentSheetFlowControllerFactory(
        FakeStripeRepository(),
        PaymentConfiguration(
            ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY
        ),
        FakePaymentSessionPrefs(),
        testDispatcher
    )

    @BeforeTest
    fun before() {
        Dispatchers.setMain(testDispatcher)
    }

    @Test
    fun `create() in default mode should create object with expected properties`() {
        var result: PaymentSheetFlowController.Result? = null
        factory.create(
            "client_secret",
            "eph_key",
            "cus_123"
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
