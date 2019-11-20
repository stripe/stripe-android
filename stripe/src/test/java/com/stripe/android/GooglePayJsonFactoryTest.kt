package com.stripe.android

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import org.json.JSONObject
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GooglePayJsonFactoryTest {

    private val googlePayConfig = GooglePayConfig(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY)
    private val factory = GooglePayJsonFactory(googlePayConfig)

    @Test
    fun testCreateIsReadyToPayRequestJson_withoutArgs() {
        val isReadyToPayRequestJson = factory.createIsReadyToPayRequest()
        val expectedJson = JSONObject("""
            {
                "apiVersion": 2,
                "apiVersionMinor": 0,
                "allowedPaymentMethods": [{
                    "type": "CARD",
                    "parameters": {
                        "allowedAuthMethods": ["PAN_ONLY", "CRYPTOGRAM_3DS"],
                        "allowedCardNetworks": ["AMEX", "DISCOVER", "INTERAC", "JCB", "MASTERCARD", "VISA"]
                    },
                    "tokenizationSpecification": {
                        "type": "PAYMENT_GATEWAY",
                        "parameters": {
                            "gateway": "stripe",
                            "stripe:version": "2019-11-05",
                            "stripe:publishableKey": "pk_test_vOo1umqsYxSrP5UXfOeL3ecm"
                        }
                    }
                }]
            }
        """.trimIndent())
        assertEquals(expectedJson.toString(), isReadyToPayRequestJson.toString())
    }

    @Test
    fun testCreateIsReadyToPayRequestJson_withArgs() {
        val isReadyToPayRequestJson = factory.createIsReadyToPayRequest(
            billingAddressParameters = GooglePayJsonFactory.BillingAddressParameters(
                isRequired = true,
                format = GooglePayJsonFactory.BillingAddressParameters.Format.Full,
                isPhoneNumberRequired = true
            ),
            existingPaymentMethodRequired = true
        )
        val expectedJson = JSONObject("""
            {
                "apiVersion": 2,
                "apiVersionMinor": 0,
                "allowedPaymentMethods": [{
                    "type": "CARD",
                    "parameters": {
                        "allowedAuthMethods": ["PAN_ONLY", "CRYPTOGRAM_3DS"],
                        "allowedCardNetworks": ["AMEX", "DISCOVER", "INTERAC", "JCB", "MASTERCARD", "VISA"],
                        "billingAddressRequired": true,
                        "billingAddressParameters": {
                            "phoneNumberRequired": true,
                            "format": "FULL"
                        }
                    },
                    "tokenizationSpecification": {
                        "type": "PAYMENT_GATEWAY",
                        "parameters": {
                            "gateway": "stripe",
                            "stripe:version": "2019-11-05",
                            "stripe:publishableKey": "pk_test_vOo1umqsYxSrP5UXfOeL3ecm"
                        }
                    }
                }],
                "existingPaymentMethodRequired": true
            }
        """.trimIndent())
        assertEquals(expectedJson.toString(), isReadyToPayRequestJson.toString())
    }

    @Test
    fun testCreatePaymentMethodRequestJson() {
        val transactionId = UUID.randomUUID().toString()
        val expectedJson = JSONObject("""
            {
                "apiVersion": 2,
                "apiVersionMinor": 0,
                "allowedPaymentMethods": [{
                    "type": "CARD",
                    "parameters": {
                        "allowedAuthMethods": ["PAN_ONLY", "CRYPTOGRAM_3DS"],
                        "allowedCardNetworks": ["AMEX", "DISCOVER", "INTERAC", "JCB", "MASTERCARD", "VISA"],
                        "billingAddressRequired": true,
                        "billingAddressParameters": {
                            "phoneNumberRequired": true,
                            "format": "FULL"
                        }
                    },
                    "tokenizationSpecification": {
                        "type": "PAYMENT_GATEWAY",
                        "parameters": {
                            "gateway": "stripe",
                            "stripe:version": "2019-11-05",
                            "stripe:publishableKey": "pk_test_vOo1umqsYxSrP5UXfOeL3ecm"
                        }
                    }
                }],
                "transactionInfo": {
                    "currencyCode": "USD",
                    "totalPriceStatus": "ESTIMATED",
                    "countryCode": "US",
                    "transactionId": "$transactionId",
                    "totalPrice": "5.00",
                    "totalPriceLabel": "Your total price",
                    "checkoutOption": "COMPLETE_IMMEDIATE_PURCHASE"
                },
                "emailRequired": false,
                "shippingAddressRequired": true,
                "shippingAddressParameters": {
                    "allowedCountryCodes": ["US", "DE"],
                    "phoneNumberRequired": true
                },
                "merchantInfo": {
                    "merchantName": "Widget Store"
                }
            }
        """.trimIndent())

        val createPaymentDataRequestJson = factory.createPaymentDataRequest(
            transactionInfo = GooglePayJsonFactory.TransactionInfo(
                currencyCode = "USD",
                totalPriceStatus = GooglePayJsonFactory.TransactionInfo.TotalPriceStatus.Estimated,
                totalPrice = 500,
                countryCode = "US",
                transactionId = transactionId,
                totalPriceLabel = "Your total price",
                checkoutOption = GooglePayJsonFactory.TransactionInfo.CheckoutOption.CompleteImmediatePurchase
            ),
            billingAddressParameters = GooglePayJsonFactory.BillingAddressParameters(
                isRequired = true,
                format = GooglePayJsonFactory.BillingAddressParameters.Format.Full,
                isPhoneNumberRequired = true
            ),
            merchantInfo = GooglePayJsonFactory.MerchantInfo(
                merchantName = "Widget Store"
            ),
            shippingAddressParameters = GooglePayJsonFactory.ShippingAddressParameters(
                isRequired = true,
                allowedCountryCodes = setOf("US", "DE"),
                phoneNumberRequired = true
            )
        )

        assertEquals(expectedJson.toString(), createPaymentDataRequestJson.toString())
    }
}
