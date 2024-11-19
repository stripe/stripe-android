package com.stripe.android.model.parsers

import com.stripe.android.core.model.CountryCode
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConsumerFixtures
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.CvcCheck
import org.json.JSONObject
import org.junit.Test
import kotlin.test.assertEquals

class ConsumerPaymentDetailsJsonParserTest {

    @Test
    fun `parse single card payment details`() {
        assertEquals(
            ConsumerPaymentDetailsJsonParser
                .parse(ConsumerFixtures.CONSUMER_SINGLE_CARD_PAYMENT_DETAILS_JSON),
            ConsumerPaymentDetails(
                listOf(
                    ConsumerPaymentDetails.Card(
                        id = "QAAAKJ6",
                        expiryYear = 2023,
                        expiryMonth = 12,
                        isDefault = true,
                        brand = CardBrand.MasterCard,
                        last4 = "4444",
                        cvcCheck = CvcCheck.Pass,
                        billingAddress = ConsumerPaymentDetails.BillingAddress(
                            countryCode = CountryCode.US,
                            postalCode = "12312"
                        )
                    )
                )
            )
        )
    }

    @Test
    fun `parse single bank account payment details`() {
        assertEquals(
            ConsumerPaymentDetails(
                listOf(
                    ConsumerPaymentDetails.BankAccount(
                        id = "wAAACGA",
                        last4 = "6789",
                        bankName = "STRIPE TEST BANK",
                        bankIconCode = null,
                        isDefault = true,
                    )
                )
            ),
            ConsumerPaymentDetailsJsonParser
                .parse(ConsumerFixtures.CONSUMER_SINGLE_BANK_ACCOUNT_PAYMENT_DETAILS_JSON),
        )
    }

    @Test
    fun `parse multiple payment details`() {
        assertEquals(
            ConsumerPaymentDetails(
                listOf(
                    ConsumerPaymentDetails.Card(
                        id = "QAAAKJ6",
                        last4 = "4444",
                        expiryYear = 2023,
                        expiryMonth = 12,
                        isDefault = true,
                        brand = CardBrand.MasterCard,
                        cvcCheck = CvcCheck.Pass,
                        billingAddress = ConsumerPaymentDetails.BillingAddress(
                            countryCode = CountryCode.US,
                            postalCode = "12312"
                        )
                    ),
                    ConsumerPaymentDetails.Card(
                        id = "QAAAKIL",
                        last4 = "4242",
                        expiryYear = 2024,
                        expiryMonth = 4,
                        brand = CardBrand.Visa,
                        cvcCheck = CvcCheck.Fail,
                        isDefault = false,
                        billingAddress = ConsumerPaymentDetails.BillingAddress(
                            countryCode = CountryCode.US,
                            postalCode = "42424"
                        )
                    ),
                    ConsumerPaymentDetails.BankAccount(
                        id = "wAAACGA",
                        last4 = "6789",
                        bankName = "STRIPE TEST BANK",
                        bankIconCode = null,
                        isDefault = false,
                    )
                )
            ),
            ConsumerPaymentDetailsJsonParser.parse(ConsumerFixtures.CONSUMER_PAYMENT_DETAILS_JSON),
        )
    }

    @Suppress("LongMethod")
    @Test
    fun `AMERICAN_EXPRESS and DINERS_CLUB card brands are fixed`() {
        val json = JSONObject(
            """
            {
              "redacted_payment_details": [
                {
                  "id": "QAAAKJ6",
                  "bank_account_details": null,
                  "billing_address": {
                    "administrative_area": null,
                    "country_code": "US",
                    "dependent_locality": null,
                    "line_1": null,
                    "line_2": null,
                    "locality": null,
                    "name": null,
                    "postal_code": "12312",
                    "sorting_code": null
                  },
                  "billing_email_address": "",
                  "card_details": {
                    "brand": "AMERICAN_EXPRESS",
                    "checks": {
                      "address_line1_check": "STATE_INVALID",
                      "address_postal_code_check": "PASS",
                      "cvc_check": "PASS"
                    },
                    "exp_month": 12,
                    "exp_year": 2023,
                    "last4": "4444"
                  },
                  "is_default": true,
                  "type": "CARD"
                },
                {
                  "id": "QAAAKIL",
                  "bank_account_details": null,
                  "billing_address": {
                    "administrative_area": null,
                    "country_code": "US",
                    "dependent_locality": null,
                    "line_1": null,
                    "line_2": null,
                    "locality": null,
                    "name": null,
                    "postal_code": "42424",
                    "sorting_code": null
                  },
                  "billing_email_address": "",
                  "card_details": {
                    "brand": "DINERS_CLUB",
                    "checks": {
                      "address_line1_check": "STATE_INVALID",
                      "address_postal_code_check": "PASS",
                      "cvc_check": "FAIL"
                    },
                    "exp_month": 4,
                    "exp_year": 2024,
                    "last4": "4242"
                  },
                  "is_default": false,
                  "type": "CARD"
                }
              ]
            }
            """.trimIndent()
        )

        assertEquals(
            ConsumerPaymentDetailsJsonParser
                .parse(json),
            ConsumerPaymentDetails(
                listOf(
                    ConsumerPaymentDetails.Card(
                        id = "QAAAKJ6",
                        last4 = "4444",
                        expiryYear = 2023,
                        expiryMonth = 12,
                        brand = CardBrand.AmericanExpress,
                        cvcCheck = CvcCheck.Pass,
                        isDefault = true,
                        billingAddress = ConsumerPaymentDetails.BillingAddress(
                            countryCode = CountryCode.US,
                            postalCode = "12312"
                        )
                    ),
                    ConsumerPaymentDetails.Card(
                        id = "QAAAKIL",
                        last4 = "4242",
                        expiryYear = 2024,
                        expiryMonth = 4,
                        brand = CardBrand.DinersClub,
                        cvcCheck = CvcCheck.Fail,
                        isDefault = false,
                        billingAddress = ConsumerPaymentDetails.BillingAddress(
                            countryCode = CountryCode.US,
                            postalCode = "42424"
                        )
                    )
                )
            )
        )
    }
}
