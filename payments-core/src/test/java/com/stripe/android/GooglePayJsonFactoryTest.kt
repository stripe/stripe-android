package com.stripe.android

import android.os.Parcel
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.model.CardBrand
import org.json.JSONException
import org.json.JSONObject
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
class GooglePayJsonFactoryTest {

    private val googlePayConfig = GooglePayConfig(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY)
    private val factory = GooglePayJsonFactory(googlePayConfig)

    @AfterTest
    fun tearDown() {
        GooglePayJsonFactory.cardBrandFilter = DefaultCardBrandFilter
    }

    @Test
    fun testCreateIsReadyToPayRequestJson_withoutArgs() {
        val isReadyToPayRequestJson = factory.createIsReadyToPayRequest()
        val expectedJson = JSONObject(
            """
            {
                "apiVersion": 2,
                "apiVersionMinor": 0,
                "allowedPaymentMethods": [{
                    "type": "CARD",
                    "parameters": {
                        "allowedAuthMethods": ["PAN_ONLY", "CRYPTOGRAM_3DS"],
                        "allowedCardNetworks": ["AMEX", "DISCOVER", "MASTERCARD", "VISA"]
                    },
                    "tokenizationSpecification": {
                        "type": "PAYMENT_GATEWAY",
                        "parameters": {
                            "gateway": "stripe",
                            "stripe:version": "StripeAndroid/${StripeSdkVersion.VERSION_NAME}",
                            "stripe:publishableKey": "${ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY}"
                        }
                    }
                }]
            }
            """.trimIndent()
        )
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
        val expectedJson = JSONObject(
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
                            "phoneNumberRequired": true,
                            "format": "FULL"
                        }
                    },
                    "tokenizationSpecification": {
                        "type": "PAYMENT_GATEWAY",
                        "parameters": {
                            "gateway": "stripe",
                            "stripe:version": "StripeAndroid/${StripeSdkVersion.VERSION_NAME}",
                            "stripe:publishableKey": "${ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY}"
                        }
                    }
                }],
                "existingPaymentMethodRequired": true
            }
            """.trimIndent()
        )
        assertThat(isReadyToPayRequestJson.toString())
            .isEqualTo(expectedJson.toString())
    }

    @Test
    fun testCreatePaymentMethodRequestJson() {
        val transactionId = UUID.randomUUID().toString()
        val expectedJson = JSONObject(
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
                            "phoneNumberRequired": true,
                            "format": "FULL"
                        }
                    },
                    "tokenizationSpecification": {
                        "type": "PAYMENT_GATEWAY",
                        "parameters": {
                            "gateway": "stripe",
                            "stripe:version": "StripeAndroid/${StripeSdkVersion.VERSION_NAME}",
                            "stripe:publishableKey": "${ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY}"
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
            """.trimIndent()
        )

        val createPaymentDataRequestJson = factory.createPaymentDataRequest(
            transactionInfo = GooglePayJsonFactory.TransactionInfo(
                currencyCode = "USD",
                totalPriceStatus = GooglePayJsonFactory.TransactionInfo.TotalPriceStatus.Estimated,
                totalPrice = 500L,
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

    @Test
    fun countryCode_shouldBeCapitalized() {
        val createPaymentDataRequestJson = factory.createPaymentDataRequest(
            transactionInfo = GooglePayJsonFactory.TransactionInfo(
                currencyCode = "USD",
                totalPriceStatus = GooglePayJsonFactory.TransactionInfo.TotalPriceStatus.Estimated,
                countryCode = "us"
            )
        )
        val countryCode = createPaymentDataRequestJson
            .getJSONObject("transactionInfo")
            .getString("countryCode")
        assertThat(countryCode)
            .isEqualTo("US")
    }

    @Test
    fun currencyCode_shouldBeCapitalized() {
        val createPaymentDataRequestJson = factory.createPaymentDataRequest(
            transactionInfo = GooglePayJsonFactory.TransactionInfo(
                currencyCode = "usd",
                totalPriceStatus = GooglePayJsonFactory.TransactionInfo.TotalPriceStatus.Final
            )
        )
        val currencyCode = createPaymentDataRequestJson
            .getJSONObject("transactionInfo")
            .getString("currencyCode")
        assertThat(currencyCode)
            .isEqualTo("USD")
    }

    @Test
    fun shippingAddressAllowedCountryCodes_shouldBeCapitalized() {
        val createPaymentDataRequestJson = factory.createPaymentDataRequest(
            transactionInfo = GooglePayJsonFactory.TransactionInfo(
                currencyCode = "USD",
                totalPriceStatus = GooglePayJsonFactory.TransactionInfo.TotalPriceStatus.Estimated,
                totalPrice = 500,
                countryCode = "US",
                totalPriceLabel = "Your total price"
            ),
            shippingAddressParameters = GooglePayJsonFactory.ShippingAddressParameters(
                isRequired = true,
                allowedCountryCodes = setOf("us", "de")
            )
        )

        val allowedCountryCodes = createPaymentDataRequestJson
            .getJSONObject("shippingAddressParameters")
            .getJSONArray("allowedCountryCodes")
            .let {
                StripeJsonUtils.jsonArrayToList(it)
            }

        assertThat(allowedCountryCodes)
            .containsExactly("US", "DE")
    }

    @Test
    fun allowedCardNetworks_whenJcbDisabled_shouldNotIncludeJcb() {
        val allowedCardNetworks = factory.createIsReadyToPayRequest()
            .getJSONArray("allowedPaymentMethods")
            .getJSONObject(0)
            .getJSONObject("parameters")
            .getJSONArray("allowedCardNetworks")
            .let {
                StripeJsonUtils.jsonArrayToList(it)
            }

        assertThat(allowedCardNetworks)
            .isEqualTo(listOf("AMEX", "DISCOVER", "MASTERCARD", "VISA"))
    }

    @Test
    fun allowedCardNetworks_whenJcbEnabled_shouldIncludeJcb() {
        val allowedCardNetworks =
            GooglePayJsonFactory(googlePayConfig, isJcbEnabled = true)
                .createIsReadyToPayRequest()
                .getJSONArray("allowedPaymentMethods")
                .getJSONObject(0)
                .getJSONObject("parameters")
                .getJSONArray("allowedCardNetworks")
                .let {
                    StripeJsonUtils.jsonArrayToList(it)
                }

        assertThat(allowedCardNetworks)
            .isEqualTo(listOf("AMEX", "DISCOVER", "MASTERCARD", "VISA", "JCB"))
    }

    @Test
    fun allowCreditCards_whenTrue_shouldIncludeAllowCreditCardsInRequest() {
        val allowCreditCards =
            GooglePayJsonFactory(googlePayConfig)
                .createIsReadyToPayRequest(
                    allowCreditCards = true
                )
                .getJSONArray("allowedPaymentMethods")
                .getJSONObject(0)
                .getJSONObject("parameters")
                .getBoolean("allowCreditCards")

        assertThat(allowCreditCards)
            .isEqualTo(true)
    }

    @Test
    fun allowCreditCards_whenFalse_shouldIncludeAllowCreditCardsInRequest() {
        val allowCreditCards =
            GooglePayJsonFactory(googlePayConfig)
                .createIsReadyToPayRequest(
                    allowCreditCards = false
                )
                .getJSONArray("allowedPaymentMethods")
                .getJSONObject(0)
                .getJSONObject("parameters")
                .getBoolean("allowCreditCards")

        assertThat(allowCreditCards)
            .isEqualTo(false)
    }

    @Test
    fun allowCreditCards_whenNull_shouldNotIncludeAllowCreditCardsInRequest() {
        assertFailsWith<JSONException> {
            GooglePayJsonFactory(googlePayConfig)
                .createIsReadyToPayRequest()
                .getJSONArray("allowedPaymentMethods")
                .getJSONObject(0)
                .getJSONObject("parameters")
                .getBoolean("allowCreditCards")
        }
    }

    @Test
    fun `allowedCardNetworks should reflect custom CardBrandFilter accepting Visa and MasterCard`() {
        val acceptedBrands = setOf(CardBrand.Visa, CardBrand.MasterCard)
        GooglePayJsonFactory.cardBrandFilter = CustomCardBrandFilter(acceptedBrands)

        val factory = GooglePayJsonFactory(googlePayConfig)
        val paymentDataRequestJson = factory.createPaymentDataRequest(
            transactionInfo = GooglePayJsonFactory.TransactionInfo(
                currencyCode = "USD",
                totalPriceStatus = GooglePayJsonFactory.TransactionInfo.TotalPriceStatus.Final,
                totalPrice = 1000
            )
        )

        val allowedCardNetworks = paymentDataRequestJson
            .getJSONArray("allowedPaymentMethods")
            .getJSONObject(0)
            .getJSONObject("parameters")
            .getJSONArray("allowedCardNetworks")
            .let { StripeJsonUtils.jsonArrayToList(it) }

        assertThat(allowedCardNetworks).containsExactly("MASTERCARD", "VISA")
    }

    @Test
    fun `allowedCardNetworks should reflect custom CardBrandFilter accepting only AMEX`() {
        val acceptedBrands = setOf(CardBrand.AmericanExpress)
        GooglePayJsonFactory.cardBrandFilter = CustomCardBrandFilter(acceptedBrands)

        val factory = GooglePayJsonFactory(googlePayConfig)
        val paymentDataRequestJson = factory.createPaymentDataRequest(
            transactionInfo = GooglePayJsonFactory.TransactionInfo(
                currencyCode = "USD",
                totalPriceStatus = GooglePayJsonFactory.TransactionInfo.TotalPriceStatus.Final,
                totalPrice = 1000
            )
        )

        val allowedCardNetworks = paymentDataRequestJson
            .getJSONArray("allowedPaymentMethods")
            .getJSONObject(0)
            .getJSONObject("parameters")
            .getJSONArray("allowedCardNetworks")
            .let { StripeJsonUtils.jsonArrayToList(it) }

        assertThat(allowedCardNetworks).containsExactly("AMEX")
    }

    @Test
    fun `allowedCardNetworks should be empty when CardBrandFilter rejects all brands`() {
        val acceptedBrands = emptySet<CardBrand>()
        GooglePayJsonFactory.cardBrandFilter = CustomCardBrandFilter(acceptedBrands)

        val factory = GooglePayJsonFactory(googlePayConfig)
        val paymentDataRequestJson = factory.createPaymentDataRequest(
            transactionInfo = GooglePayJsonFactory.TransactionInfo(
                currencyCode = "USD",
                totalPriceStatus = GooglePayJsonFactory.TransactionInfo.TotalPriceStatus.Final,
                totalPrice = 1000
            )
        )

        val allowedCardNetworks = paymentDataRequestJson
            .getJSONArray("allowedPaymentMethods")
            .getJSONObject(0)
            .getJSONObject("parameters")
            .getJSONArray("allowedCardNetworks")
            .let { StripeJsonUtils.jsonArrayToList(it) }

        assertThat(allowedCardNetworks).isEmpty()
    }

    @Test
    fun `allowedCardNetworks should include JCB when JCB is enabled and accepted by filter`() {
        val acceptedBrands = setOf(CardBrand.JCB, CardBrand.Visa)
        GooglePayJsonFactory.cardBrandFilter = CustomCardBrandFilter(acceptedBrands)

        val factory = GooglePayJsonFactory(googlePayConfig, isJcbEnabled = true)
        val paymentDataRequestJson = factory.createPaymentDataRequest(
            transactionInfo = GooglePayJsonFactory.TransactionInfo(
                currencyCode = "JPY",
                totalPriceStatus = GooglePayJsonFactory.TransactionInfo.TotalPriceStatus.Final,
                totalPrice = 1000
            )
        )

        val allowedCardNetworks = paymentDataRequestJson
            .getJSONArray("allowedPaymentMethods")
            .getJSONObject(0)
            .getJSONObject("parameters")
            .getJSONArray("allowedCardNetworks")
            .let { StripeJsonUtils.jsonArrayToList(it) }

        assertThat(allowedCardNetworks).containsExactly("VISA", "JCB")
    }

    @Test
    fun `allowedCardNetworks should not include JCB when JCB is disabled even if accepted by filter`() {
        val acceptedBrands = setOf(CardBrand.JCB, CardBrand.Visa)
        GooglePayJsonFactory.cardBrandFilter = CustomCardBrandFilter(acceptedBrands)

        val factory = GooglePayJsonFactory(googlePayConfig, isJcbEnabled = false)
        val paymentDataRequestJson = factory.createPaymentDataRequest(
            transactionInfo = GooglePayJsonFactory.TransactionInfo(
                currencyCode = "JPY",
                totalPriceStatus = GooglePayJsonFactory.TransactionInfo.TotalPriceStatus.Final,
                totalPrice = 1000
            )
        )

        val allowedCardNetworks = paymentDataRequestJson
            .getJSONArray("allowedPaymentMethods")
            .getJSONObject(0)
            .getJSONObject("parameters")
            .getJSONArray("allowedCardNetworks")
            .let { StripeJsonUtils.jsonArrayToList(it) }

        assertThat(allowedCardNetworks).containsExactly("VISA")
        assertThat(allowedCardNetworks).doesNotContain("JCB")
    }

    @Test
    fun `allowedCardNetworks should not include JCB when JCB is enabled but not accepted by filter`() {
        val acceptedBrands = setOf(CardBrand.Visa)
        GooglePayJsonFactory.cardBrandFilter = CustomCardBrandFilter(acceptedBrands)

        val factory = GooglePayJsonFactory(googlePayConfig, isJcbEnabled = true)
        val paymentDataRequestJson = factory.createPaymentDataRequest(
            transactionInfo = GooglePayJsonFactory.TransactionInfo(
                currencyCode = "JPY",
                totalPriceStatus = GooglePayJsonFactory.TransactionInfo.TotalPriceStatus.Final,
                totalPrice = 1000
            )
        )

        val allowedCardNetworks = paymentDataRequestJson
            .getJSONArray("allowedPaymentMethods")
            .getJSONObject(0)
            .getJSONObject("parameters")
            .getJSONArray("allowedCardNetworks")
            .let { StripeJsonUtils.jsonArrayToList(it) }

        assertThat(allowedCardNetworks).containsExactly("VISA")
        assertThat(allowedCardNetworks).doesNotContain("JCB")
    }

    class CustomCardBrandFilter(private val acceptedBrands: Set<CardBrand>) : CardBrandFilter {
        override fun isAccepted(cardBrand: CardBrand): Boolean {
            return acceptedBrands.contains(cardBrand)
        }

        override fun describeContents(): Int {
            throw IllegalStateException("describeContents should be not called.")
        }

        override fun writeToParcel(p0: Parcel, p1: Int) {
            throw IllegalStateException("writeToParcel should be not called.")
        }
    }
}
