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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
class GooglePayJsonFactoryTest {

    private val googlePayConfig = GooglePayConfig(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY)
    private val factory = GooglePayJsonFactory(googlePayConfig)

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
    fun allowedCardNetworks_withCustomCardBrandFilter_shouldIncludeOnlyFilteredBrands() {
        // Define a custom CardBrandFilter that only accepts Visa and Mastercard
        val customCardBrandFilter = object : CardBrandFilter {
            override fun isAccepted(cardBrand: CardBrand): Boolean {
                return cardBrand == CardBrand.Visa || cardBrand == CardBrand.MasterCard
            }

            override fun describeContents(): Int {
                TODO("Not yet implemented")
            }

            override fun writeToParcel(p0: Parcel, p1: Int) {
                TODO("Not yet implemented")
            }
        }

        val isReadyToPayRequestJson = factory.createIsReadyToPayRequest(
            cardBrandFilter = customCardBrandFilter
        )

        val allowedCardNetworks = isReadyToPayRequestJson
            .getJSONArray("allowedPaymentMethods")
            .getJSONObject(0)
            .getJSONObject("parameters")
            .getJSONArray("allowedCardNetworks")
            .let { StripeJsonUtils.jsonArrayToList(it) }

        // Verify that only Visa and Mastercard are included
        assertThat(allowedCardNetworks)
            .containsExactly("MASTERCARD", "VISA")
    }

    @Test
    fun allowedCardNetworks_withAllBrandsFilteredOut_shouldIncludeNoBrands() {
        // Define a CardBrandFilter that rejects all brands
        val rejectAllCardBrandFilter = object : CardBrandFilter {
            override fun isAccepted(cardBrand: CardBrand): Boolean {
                return false
            }

            override fun describeContents(): Int {
                TODO("Not yet implemented")
            }

            override fun writeToParcel(p0: Parcel, p1: Int) {
                TODO("Not yet implemented")
            }
        }

        val isReadyToPayRequestJson = factory.createIsReadyToPayRequest(
            cardBrandFilter = rejectAllCardBrandFilter
        )

        val allowedCardNetworks = isReadyToPayRequestJson
            .getJSONArray("allowedPaymentMethods")
            .getJSONObject(0)
            .getJSONObject("parameters")
            .optJSONArray("allowedCardNetworks")

        // Verify that allowedCardNetworks is an empty array
        assertThat(allowedCardNetworks!!.length()).isEqualTo(0)
    }

    @Test
    fun allowedCardNetworks_withDefaultCardBrandFilter_shouldIncludeAllBrands() {
        val isReadyToPayRequestJson = factory.createIsReadyToPayRequest(
            cardBrandFilter = DefaultCardBrandFilter
        )

        val allowedCardNetworks = isReadyToPayRequestJson
            .getJSONArray("allowedPaymentMethods")
            .getJSONObject(0)
            .getJSONObject("parameters")
            .getJSONArray("allowedCardNetworks")
            .let { StripeJsonUtils.jsonArrayToList(it) }

        // Verify that all default brands are included
        assertThat(allowedCardNetworks)
            .isEqualTo(listOf("AMEX", "DISCOVER", "MASTERCARD", "VISA"))
    }

    @Test
    fun allowedCardNetworks_whenJcbEnabled_andFilterAcceptsJcb_shouldIncludeJcb() {
        // Enable JCB in the factory
        val factoryWithJcbEnabled = GooglePayJsonFactory(googlePayConfig, isJcbEnabled = true)

        // Define a CardBrandFilter that accepts JCB
        val acceptJcbCardBrandFilter = object : CardBrandFilter {
            override fun isAccepted(cardBrand: CardBrand): Boolean {
                return cardBrand == CardBrand.JCB
            }

            override fun describeContents(): Int {
                TODO("Not yet implemented")
            }

            override fun writeToParcel(p0: Parcel, p1: Int) {
                TODO("Not yet implemented")
            }
        }

        val isReadyToPayRequestJson = factoryWithJcbEnabled.createIsReadyToPayRequest(
            cardBrandFilter = acceptJcbCardBrandFilter
        )

        val allowedCardNetworks = isReadyToPayRequestJson
            .getJSONArray("allowedPaymentMethods")
            .getJSONObject(0)
            .getJSONObject("parameters")
            .getJSONArray("allowedCardNetworks")
            .let { StripeJsonUtils.jsonArrayToList(it) }

        // Verify that only JCB is included
        assertThat(allowedCardNetworks)
            .containsExactly("JCB")
    }

    @Test
    fun allowedCardNetworks_whenJcbEnabled_butFilterRejectsJcb_shouldNotIncludeJcb() {
        // Enable JCB in the factory
        val factoryWithJcbEnabled = GooglePayJsonFactory(googlePayConfig, isJcbEnabled = true)

        // Define a CardBrandFilter that rejects JCB
        val rejectJcbCardBrandFilter = object : CardBrandFilter {
            override fun isAccepted(cardBrand: CardBrand): Boolean {
                return cardBrand != CardBrand.JCB
            }

            override fun describeContents(): Int {
                TODO("Not yet implemented")
            }

            override fun writeToParcel(p0: Parcel, p1: Int) {
                TODO("Not yet implemented")
            }
        }

        val isReadyToPayRequestJson = factoryWithJcbEnabled.createIsReadyToPayRequest(
            cardBrandFilter = rejectJcbCardBrandFilter
        )

        val allowedCardNetworks = isReadyToPayRequestJson
            .getJSONArray("allowedPaymentMethods")
            .getJSONObject(0)
            .getJSONObject("parameters")
            .getJSONArray("allowedCardNetworks")
            .let { StripeJsonUtils.jsonArrayToList(it) }

        // Verify that JCB is not included
        assertThat(allowedCardNetworks)
            .doesNotContain("JCB")
    }

    @Test
    fun allowedCardNetworks_whenNoCardBrandFilterProvided_shouldUseDefault() {
        // No CardBrandFilter is provided, so the default should be used
        val isReadyToPayRequestJson = factory.createIsReadyToPayRequest()

        val allowedCardNetworks = isReadyToPayRequestJson
            .getJSONArray("allowedPaymentMethods")
            .getJSONObject(0)
            .getJSONObject("parameters")
            .getJSONArray("allowedCardNetworks")
            .let { StripeJsonUtils.jsonArrayToList(it) }

        // Verify that all default brands are included
        assertThat(allowedCardNetworks)
            .isEqualTo(listOf("AMEX", "DISCOVER", "MASTERCARD", "VISA"))
    }
}
