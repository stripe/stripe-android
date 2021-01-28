package com.stripe.android.googlepay

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.StripeJsonUtils
import com.stripe.android.networking.AbsFakeStripeRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.json.JSONObject
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class StripeGooglePayViewModelTest {
    private val testDispatcher = TestCoroutineDispatcher()

    private val viewModel: StripeGooglePayViewModel by lazy { createViewModel() }

    @BeforeTest
    fun setup() {
        PaymentConfiguration.init(
            ApplicationProvider.getApplicationContext(),
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
        )
    }

    @AfterTest
    fun cleanup() {
        testDispatcher.cleanupTestCoroutines()
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

    private fun createViewModel(
        args: StripeGooglePayContract.Args = ARGS
    ): StripeGooglePayViewModel {
        return StripeGooglePayViewModel(
            ApplicationProvider.getApplicationContext(),
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            args,
            FakeStripeRepository(),
            "App Name",
            testDispatcher
        )
    }

    private class FakeStripeRepository : AbsFakeStripeRepository()

    private companion object {
        private val CONFIG = StripeGooglePayContract.GooglePayConfig(
            environment = StripeGooglePayEnvironment.Test,
            countryCode = "US",
            isEmailRequired = true
        )

        private val ARGS = StripeGooglePayContract.Args.ConfirmPaymentIntent(
            paymentIntent = PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
            config = CONFIG
        )
    }
}
