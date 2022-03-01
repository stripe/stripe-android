package com.stripe.android.googlepaylauncher

import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.GooglePayFixtures.GOOGLE_PAY_RESULT_WITH_NO_BILLING_ADDRESS
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.StripeJsonUtils
import com.stripe.android.networking.AbsFakeStripeRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import org.json.JSONObject
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.BeforeTest
import kotlin.test.Test

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class StripeGooglePayViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    private val viewModel: StripeGooglePayViewModel by lazy { createViewModel() }

    @BeforeTest
    fun setup() {
        PaymentConfiguration.init(
            ApplicationProvider.getApplicationContext(),
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
        )
    }

    @Test
    fun `createIsReadyToPayRequest() returns expected object`() {
        val allowedPaymentMethods = JSONObject(viewModel.createIsReadyToPayRequest().toJson())
            .getJSONArray("allowedPaymentMethods")
        assertThat(allowedPaymentMethods.length())
            .isEqualTo(1)
        val allowedCardNetworks = allowedPaymentMethods
            .getJSONObject(0)
            .getJSONObject("parameters")
            .getJSONArray("allowedCardNetworks")

        assertThat(
            StripeJsonUtils.jsonArrayToList(allowedCardNetworks)
        ).isEqualTo(
            listOf("AMEX", "DISCOVER", "MASTERCARD", "VISA")
        )
    }

    @Test
    fun `createPaymentDataRequestForPaymentIntentArgs() without merchant name should return expected JSON`() {
        assertThat(viewModel.createPaymentDataRequestForPaymentIntentArgs().toString())
            .isEqualTo(
                JSONObject(
                    """
                    {
                        "apiVersion": 2,
                        "apiVersionMinor": 0,
                        "allowedPaymentMethods": [{
                            "type": "CARD",
                            "parameters": {
                                "allowedAuthMethods": ["PAN_ONLY", "CRYPTOGRAM_3DS"],
                                "allowedCardNetworks": ["AMEX", "DISCOVER", "MASTERCARD", "VISA"],
                                "billingAddressRequired": true,
                                "billingAddressParameters": {
                                    "phoneNumberRequired": false,
                                    "format": "MIN"
                                }
                            },
                            "tokenizationSpecification": {
                                "type": "PAYMENT_GATEWAY",
                                "parameters": {
                                    "gateway": "stripe",
                                    "stripe:version": "2020-03-02",
                                    "stripe:publishableKey": "pk_test_123"
                                }
                            }
                        }],
                        "transactionInfo": {
                            "currencyCode": "USD",
                            "totalPriceStatus": "FINAL",
                            "countryCode": "US",
                            "transactionId": "pi_1ExkUeAWhjPjYwPiXph9ouXa",
                            "totalPrice": "20.00",
                            "checkoutOption": "COMPLETE_IMMEDIATE_PURCHASE"
                        },
                        "emailRequired": true,
                        "merchantInfo": {
                            "merchantName": "App Name"
                        }
                    }
                    """.trimIndent()
                ).toString()
            )
    }

    @Test
    fun `createPaymentDataRequestForPaymentIntentArgs() with merchant name should return expected JSON`() {
        assertThat(
            createViewModel(
                ARGS.copy(
                    config = CONFIG.copy(
                        merchantName = "Widgets, Inc."
                    )
                )
            ).createPaymentDataRequestForPaymentIntentArgs().toString()
        ).isEqualTo(
            JSONObject(
                """
                    {
                        "apiVersion": 2,
                        "apiVersionMinor": 0,
                        "allowedPaymentMethods": [{
                            "type": "CARD",
                            "parameters": {
                                "allowedAuthMethods": ["PAN_ONLY", "CRYPTOGRAM_3DS"],
                                "allowedCardNetworks": ["AMEX", "DISCOVER", "MASTERCARD", "VISA"],
                                "billingAddressRequired": true,
                                "billingAddressParameters": {
                                    "phoneNumberRequired": false,
                                    "format": "MIN"
                                }
                            },
                            "tokenizationSpecification": {
                                "type": "PAYMENT_GATEWAY",
                                "parameters": {
                                    "gateway": "stripe",
                                    "stripe:version": "2020-03-02",
                                    "stripe:publishableKey": "pk_test_123"
                                }
                            }
                        }],
                        "transactionInfo": {
                            "currencyCode": "USD",
                            "totalPriceStatus": "FINAL",
                            "countryCode": "US",
                            "transactionId": "pi_1ExkUeAWhjPjYwPiXph9ouXa",
                            "totalPrice": "20.00",
                            "checkoutOption": "COMPLETE_IMMEDIATE_PURCHASE"
                        },
                        "emailRequired": true,
                        "merchantInfo": {
                            "merchantName": "Widgets, Inc."
                        }
                    }
                """.trimIndent()
            ).toString()
        )
    }

    @Test
    fun `createPaymentMethod() with stripeAccount should include stripeAccount in request`() {
        val stripeAccount = "account_id"
        var requestOptions: ApiRequest.Options? = null
        val stripeRepository = object : AbsFakeStripeRepository() {
            override suspend fun createPaymentMethod(
                paymentMethodCreateParams: PaymentMethodCreateParams,
                options: ApiRequest.Options
            ): PaymentMethod? {
                requestOptions = options
                return null
            }
        }
        val viewModel = StripeGooglePayViewModel(
            ApplicationProvider.getApplicationContext(),
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            stripeAccount,
            ARGS,
            stripeRepository,
            "App Name",
            testDispatcher
        )

        val params = PaymentMethodCreateParams.createFromGooglePay(
            GOOGLE_PAY_RESULT_WITH_NO_BILLING_ADDRESS
        )

        viewModel.createPaymentMethod(params).observeForever {
            assertThat(requestOptions?.stripeAccount).isEqualTo(stripeAccount)
        }
    }

    private fun createViewModel(
        args: StripeGooglePayContract.Args = ARGS
    ): StripeGooglePayViewModel {
        return StripeGooglePayViewModel(
            ApplicationProvider.getApplicationContext(),
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            null,
            args,
            FakeStripeRepository(),
            "App Name",
            testDispatcher
        )
    }

    private class FakeStripeRepository : AbsFakeStripeRepository()

    private companion object {
        private val CONFIG = GooglePayConfig(
            environment = GooglePayEnvironment.Test,
            amount = 2000,
            countryCode = "US",
            currencyCode = "usd",
            isEmailRequired = true,
            transactionId = "pi_1ExkUeAWhjPjYwPiXph9ouXa"
        )

        private val ARGS = StripeGooglePayContract.Args(
            config = CONFIG,
            statusBarColor = Color.RED
        )
    }
}
