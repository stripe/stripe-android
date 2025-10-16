package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.model.CountryCode
import com.stripe.android.model.parsers.ConsumerPaymentDetailsJsonParser
import org.json.JSONObject
import org.junit.Test
import java.util.Locale

class ConsumerPaymentDetailsCreateParamsTest {

    @Test
    fun `ConsumerPaymentDetailsCreateParams_Card_toParamMap_createsCorrectParameters`() {
        assertThat(
            ConsumerPaymentDetailsCreateParams.Card(
                cardPaymentMethodCreateParamsMap = mapOf(
                    "ignored" to "none",
                    "card" to mapOf(
                        "number" to "123",
                        "cvc" to "321",
                        "brand" to "visa",
                        "exp_month" to "12",
                        "exp_year" to "2050"
                    ),
                    "billing_details" to mapOf<String, Any>(
                        "address" to mapOf(
                            "country" to "US",
                            "postal_code" to "12345",
                            "extra" to "1"
                        )
                    )
                ),
                email = "email@stripe.com",
            ).toParamMap()
        ).isEqualTo(
            mapOf(
                "type" to "card",
                "billing_email_address" to "email@stripe.com",
                "card" to mapOf(
                    "number" to "123",
                    "exp_month" to "12",
                    "exp_year" to "2050"
                ),
                "billing_address" to mapOf(
                    "country_code" to "US",
                    "postal_code" to "12345"
                ),
                "active" to true,
            )
        )
    }

    @Test
    fun consumerPaymentDetailsCreateParams_Card_toParamMap_includesClientAttributionMetadata() {
        val clientAttributionMetadataMap = mapOf(
            "elements_session_config_id" to "elements_session_123",
        )
        val params = ConsumerPaymentDetailsCreateParams.Card(
            cardPaymentMethodCreateParamsMap = mapOf(
                "client_attribution_metadata" to clientAttributionMetadataMap,
            ),
            email = "email@stripe.com",
        ).toParamMap()

        assertThat(params).containsEntry(
            "client_attribution_metadata",
            clientAttributionMetadataMap,
        )
    }

    @Test
    fun consumerPaymentDetailsCreateParams_Bank_toParamMap_includesClientAttributionMetadata() {
        val clientAttributionMetadataMap = mapOf(
            "elements_session_config_id" to "elements_session_123",
        )
        val params = ConsumerPaymentDetailsCreateParams.BankAccount(
            bankAccountId = "bank_123",
            billingAddress = emptyMap(),
            billingEmailAddress = null,
            clientAttributionMetadata = mapOf("client_attribution_metadata" to clientAttributionMetadataMap),
        ).toParamMap()

        assertThat(params).containsEntry(
            "client_attribution_metadata",
            clientAttributionMetadataMap,
        )
    }

    @Test
    fun `ConsumerPaymentDetailsCreateParams_Card_toParamMap_withPreferredNetwork`() {
        assertThat(
            ConsumerPaymentDetailsCreateParams.Card(
                cardPaymentMethodCreateParamsMap = mapOf(
                    "ignored" to "none",
                    "card" to mapOf(
                        "number" to "123",
                        "cvc" to "321",
                        "brand" to "visa",
                        "exp_month" to "12",
                        "exp_year" to "2050",
                        "networks" to mapOf(
                            "preferred" to "cartes_bancaires",
                        )
                    ),
                    "billing_details" to mapOf<String, Any>(
                        "address" to mapOf(
                            "country" to "US",
                            "postal_code" to "12345",
                            "extra" to "1"
                        )
                    )
                ),
                email = "email@stripe.com",
            ).toParamMap()
        ).isEqualTo(
            mapOf(
                "type" to "card",
                "billing_email_address" to "email@stripe.com",
                "card" to mapOf(
                    "number" to "123",
                    "exp_month" to "12",
                    "exp_year" to "2050",
                    "preferred_network" to "cartes_bancaires",
                ),
                "billing_address" to mapOf(
                    "country_code" to "US",
                    "postal_code" to "12345"
                ),
                "active" to true,
            )
        )
    }

    @Test
    fun `getConsumerPaymentDetailsAddressFromPaymentMethodCreateParams_withFullAddress_mapsAllFields`() {
        val params = mapOf(
            "billing_details" to mapOf(
                "name" to "John Doe",
                "address" to mapOf(
                    "country" to "US",
                    "postal_code" to "12345",
                    "line1" to "123 Main St",
                    "line2" to "Apt 4B",
                    "city" to "New York",
                    "state" to "NY"
                )
            )
        )

        val result = getConsumerPaymentDetailsAddressFromPaymentMethodCreateParams(params)

        assertThat(result).isEqualTo(
            "billing_address" to mapOf(
                "country_code" to "US",
                "postal_code" to "12345",
                "line_1" to "123 Main St",
                "line_2" to "Apt 4B",
                "locality" to "New York",
                "administrative_area" to "NY",
                "name" to "John Doe"
            )
        )
    }

    @Test
    fun `getConsumerPaymentDetailsAddressFromPaymentMethodCreateParams_withPartialAddress_mapsAvailableFields`() {
        val params = mapOf(
            "billing_details" to mapOf(
                "name" to "Jane Smith",
                "address" to mapOf(
                    "country" to "GB",
                    "postal_code" to "SW1A 1AA",
                    "line1" to "10 Downing Street"
                    // Missing line2, city, state
                )
            )
        )

        val result = getConsumerPaymentDetailsAddressFromPaymentMethodCreateParams(params)

        assertThat(result).isEqualTo(
            "billing_address" to mapOf(
                "country_code" to "GB",
                "postal_code" to "SW1A 1AA",
                "line_1" to "10 Downing Street",
                "name" to "Jane Smith"
            )
        )
    }

    @Test
    fun `getConsumerPaymentDetailsAddressFromPaymentMethodCreateParams_filtersEmptyValues`() {
        val params = mapOf(
            "billing_details" to mapOf(
                "name" to "",
                "address" to mapOf(
                    "country" to "FR",
                    "postal_code" to "75001",
                    "line1" to "",
                    "line2" to null,
                    "city" to "Paris",
                    "state" to ""
                )
            )
        )

        val result = getConsumerPaymentDetailsAddressFromPaymentMethodCreateParams(params)

        assertThat(result).isEqualTo(
            "billing_address" to mapOf(
                "country_code" to "FR",
                "postal_code" to "75001",
                "locality" to "Paris"
            )
        )
    }

    @Test
    fun `getConsumerPaymentDetailsAddressFromPaymentMethodCreateParams_noAddress_returnsDefault`() {
        val params = mapOf(
            "billing_details" to mapOf(
                "name" to "John Doe"
            )
        )

        val result = getConsumerPaymentDetailsAddressFromPaymentMethodCreateParams(params)
        val expected = "billing_address" to mapOf("country_code" to Locale.getDefault().country)

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `getConsumerPaymentDetailsAddressFromPaymentMethodCreateParams_noBillingDetails_returnsDefault`() {
        val params = mapOf(
            "card" to mapOf(
                "number" to "4242424242424242",
                "exp_month" to 12,
                "exp_year" to 2025
            )
        )

        val result = getConsumerPaymentDetailsAddressFromPaymentMethodCreateParams(params)
        val expected = "billing_address" to mapOf("country_code" to Locale.getDefault().country)

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `ConsumerPaymentDetailsUpdateParams_toParamMap_withAllFields`() {
        val updateParams = ConsumerPaymentDetailsUpdateParams(
            id = "card_123",
            isDefault = true,
            cardPaymentMethodCreateParamsMap = mapOf(
                "card" to mapOf(
                    "exp_month" to 6,
                    "exp_year" to 2026,
                    "networks" to mapOf(
                        "preferred" to "visa"
                    )
                ),
                "billing_details" to mapOf(
                    "name" to "Updated Name",
                    "address" to mapOf(
                        "country" to "CA",
                        "postal_code" to "K1A0A6",
                        "line1" to "123 Updated St",
                        "city" to "Ottawa",
                        "state" to "ON"
                    )
                )
            )
        )

        val result = updateParams.toParamMap()

        assertThat(result).containsEntry("is_default", true)
        assertThat(result).containsEntry("exp_month", 6)
        assertThat(result).containsEntry("exp_year", 2026)
        assertThat(result).containsEntry("preferred_network", "visa")
        assertThat(result).containsKey("billing_address")

        val billingAddress = result["billing_address"] as Map<*, *>
        assertThat(billingAddress).containsEntry("country_code", "CA")
        assertThat(billingAddress).containsEntry("postal_code", "K1A0A6")
        assertThat(billingAddress).containsEntry("line_1", "123 Updated St")
        assertThat(billingAddress).containsEntry("locality", "Ottawa")
        assertThat(billingAddress).containsEntry("administrative_area", "ON")
        assertThat(billingAddress).containsEntry("name", "Updated Name")
    }

    @Test
    fun `ConsumerPaymentDetailsUpdateParams_toParamMap_withMinimalFields`() {
        val updateParams = ConsumerPaymentDetailsUpdateParams(
            id = "card_123",
            isDefault = null,
            cardPaymentMethodCreateParamsMap = null
        )

        val result = updateParams.toParamMap()

        assertThat(result).isEmpty()
    }

    @Test
    fun `ConsumerPaymentDetailsUpdateParams_toParamMap_withCardFieldsOnly`() {
        val updateParams = ConsumerPaymentDetailsUpdateParams(
            id = "card_123",
            isDefault = false,
            cardPaymentMethodCreateParamsMap = mapOf(
                "card" to mapOf(
                    "exp_month" to 3,
                    "exp_year" to 2027
                )
            )
        )

        val result = updateParams.toParamMap()

        assertThat(result).doesNotContainKey("is_default")
        assertThat(result).containsEntry("exp_month", 3)
        assertThat(result).containsEntry("exp_year", 2027)
        assertThat(result).containsEntry("billing_address", mapOf("country_code" to Locale.getDefault().country))
        assertThat(result).doesNotContainKey("preferred_network")
    }

    @Test
    fun `ConsumerPaymentDetailsUpdateParams_toParamMap_withCardFieldsOnly_isDefault`() {
        val updateParams = ConsumerPaymentDetailsUpdateParams(
            id = "card_123",
            isDefault = true,
            cardPaymentMethodCreateParamsMap = mapOf(
                "card" to mapOf(
                    "exp_month" to 3,
                    "exp_year" to 2027
                )
            )
        )

        val result = updateParams.toParamMap()

        assertThat(result).containsEntry("is_default", true)
        assertThat(result).containsEntry("exp_month", 3)
        assertThat(result).containsEntry("exp_year", 2027)
        assertThat(result).containsEntry("billing_address", mapOf("country_code" to Locale.getDefault().country))
        assertThat(result).doesNotContainKey("preferred_network")
    }

    @Test
    fun `ConsumerPaymentDetails_BillingAddress_creationWithAllFields`() {
        val billingAddress = ConsumerPaymentDetails.BillingAddress(
            name = "John Doe",
            line1 = "123 Main St",
            line2 = "Apt 4B",
            administrativeArea = "NY",
            locality = "New York",
            postalCode = "10001",
            countryCode = CountryCode.US
        )

        assertThat(billingAddress.name).isEqualTo("John Doe")
        assertThat(billingAddress.line1).isEqualTo("123 Main St")
        assertThat(billingAddress.line2).isEqualTo("Apt 4B")
        assertThat(billingAddress.administrativeArea).isEqualTo("NY")
        assertThat(billingAddress.locality).isEqualTo("New York")
        assertThat(billingAddress.postalCode).isEqualTo("10001")
        assertThat(billingAddress.countryCode).isEqualTo(CountryCode.US)
    }

    @Test
    fun `ConsumerPaymentDetails_BillingAddress_creationWithNullFields`() {
        val billingAddress = ConsumerPaymentDetails.BillingAddress(
            name = null,
            line1 = null,
            line2 = null,
            administrativeArea = null,
            locality = null,
            postalCode = null,
            countryCode = null
        )

        assertThat(billingAddress.name).isNull()
        assertThat(billingAddress.line1).isNull()
        assertThat(billingAddress.line2).isNull()
        assertThat(billingAddress.administrativeArea).isNull()
        assertThat(billingAddress.locality).isNull()
        assertThat(billingAddress.postalCode).isNull()
        assertThat(billingAddress.countryCode).isNull()
    }

    @Test
    fun `ConsumerPaymentDetails_Card_withBillingAddress`() {
        val billingAddress = ConsumerPaymentDetails.BillingAddress(
            name = "John Doe",
            line1 = "123 Main St",
            locality = "New York",
            countryCode = CountryCode.US,
            postalCode = "10001",
            line2 = null,
            administrativeArea = null
        )

        val card = ConsumerPaymentDetails.Card(
            id = "card_123",
            last4 = "4242",
            isDefault = true,
            nickname = "My Card",
            expiryYear = 2025,
            expiryMonth = 12,
            brand = CardBrand.Visa,
            networks = listOf("visa"),
            cvcCheck = CvcCheck.Pass,
            funding = "credit",
            billingAddress = billingAddress,
            billingEmailAddress = "john@example.com"
        )

        assertThat(card.billingAddress).isEqualTo(billingAddress)
        assertThat(card.billingEmailAddress).isEqualTo("john@example.com")
    }

    @Test
    fun `ConsumerPaymentDetailsJsonParser_parsesBillingAddress_withAllFields`() {
        val json = JSONObject(
            """
            {
                "redacted_payment_details": {
                    "id": "card_123",
                    "type": "card",
                    "is_default": true,
                    "billing_address": {
                        "name": "Test User",
                        "line_1": "123 Test St",
                        "line_2": "Suite 100",
                        "locality": "Test City",
                        "administrative_area": "TS",
                        "postal_code": "12345",
                        "country_code": "US"
                    },
                    "billing_email_address": "test@example.com",
                    "card_details": {
                        "last4": "4242",
                        "exp_year": 2025,
                        "exp_month": 12,
                        "brand": "visa",
                        "networks": ["visa"],
                        "funding": "credit",
                        "checks": {
                            "cvc_check": "pass"
                        }
                    }
                }
            }
            """.trimIndent()
        )

        val result = ConsumerPaymentDetailsJsonParser.parse(json)

        assertThat(result.paymentDetails).hasSize(1)
        val card = result.paymentDetails[0] as ConsumerPaymentDetails.Card

        val billingAddress = requireNotNull(card.billingAddress)
        assertThat(billingAddress.name).isEqualTo("Test User")
        assertThat(billingAddress.line1).isEqualTo("123 Test St")
        assertThat(billingAddress.line2).isEqualTo("Suite 100")
        assertThat(billingAddress.locality).isEqualTo("Test City")
        assertThat(billingAddress.administrativeArea).isEqualTo("TS")
        assertThat(billingAddress.postalCode).isEqualTo("12345")
        assertThat(billingAddress.countryCode).isEqualTo(CountryCode.US)
        assertThat(card.billingEmailAddress).isEqualTo("test@example.com")
    }

    @Test
    fun `ConsumerPaymentDetailsJsonParser_parsesBillingAddress_withPartialFields`() {
        val json = JSONObject(
            """
            {
                "redacted_payment_details": {
                    "id": "card_123",
                    "type": "card",
                    "billing_address": {
                        "country_code": "GB",
                        "postal_code": "SW1A 1AA"
                    },
                    "card_details": {
                        "last4": "4242",
                        "exp_year": 2025,
                        "exp_month": 12,
                        "brand": "visa",
                        "networks": ["visa"],
                        "funding": "credit",
                        "checks": {
                            "cvc_check": "pass"
                        }
                    }
                }
            }
            """.trimIndent()
        )

        val result = ConsumerPaymentDetailsJsonParser.parse(json)

        val card = result.paymentDetails[0] as ConsumerPaymentDetails.Card

        assertThat(card.billingAddress).isNotNull()
        assertThat(card.billingAddress!!.countryCode).isEqualTo(CountryCode.create("GB"))
        assertThat(card.billingAddress!!.postalCode).isEqualTo("SW1A 1AA")
        assertThat(card.billingAddress!!.name).isNull()
        assertThat(card.billingAddress!!.line1).isNull()
        assertThat(card.billingAddress!!.line2).isNull()
        assertThat(card.billingAddress!!.locality).isNull()
        assertThat(card.billingAddress!!.administrativeArea).isNull()
    }

    @Test
    fun `ConsumerPaymentDetailsJsonParser_noBillingAddress_returnsNullFields`() {
        val json = JSONObject(
            """
            {
                "redacted_payment_details": {
                    "id": "card_123",
                    "type": "card",
                    "card_details": {
                        "last4": "4242",
                        "exp_year": 2025,
                        "exp_month": 12,
                        "brand": "visa",
                        "networks": ["visa"],
                        "funding": "credit",
                        "checks": {
                            "cvc_check": "pass"
                        }
                    }
                }
            }
            """.trimIndent()
        )

        val result = ConsumerPaymentDetailsJsonParser.parse(json)

        val card = result.paymentDetails[0] as ConsumerPaymentDetails.Card

        assertThat(card.billingAddress).isNull()
        assertThat(card.billingEmailAddress).isNull()
    }
}
