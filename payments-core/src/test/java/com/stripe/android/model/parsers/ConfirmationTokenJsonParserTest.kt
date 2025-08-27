package com.stripe.android.model.parsers

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.Address
import com.stripe.android.model.ConfirmationToken
import com.stripe.android.model.MandateDataParams
import com.stripe.android.model.PaymentMethod
import org.json.JSONObject
import kotlin.test.Test

class ConfirmationTokenJsonParserTest {

    private val parser = ConfirmationTokenJsonParser()

    @Test
    fun parse_withValidConfirmationToken_shouldCreateExpectedObject() {
        val json = JSONObject(VALID_CONFIRMATION_TOKEN_JSON)
        
        val result = parser.parse(json)
        
        assertThat(result).isNotNull()
        assertThat(result!!.id).isEqualTo("ctoken_1234567890")
        assertThat(result.`object`).isEqualTo("confirmation_token")
        assertThat(result.created).isEqualTo(1609459200L)
        assertThat(result.liveMode).isFalse()
        assertThat(result.returnUrl).isEqualTo("https://example.com/return")
    }

    @Test
    fun parse_withIncorrectObjectType_shouldReturnNull() {
        val json = JSONObject("""
            {
                "id": "ctoken_1234567890",
                "object": "invalid_object_type",
                "created": 1609459200,
                "livemode": false
            }
        """.trimIndent())
        
        val result = parser.parse(json)
        
        assertThat(result).isNull()
    }

    @Test
    fun parse_withMissingId_shouldReturnNull() {
        val json = JSONObject("""
            {
                "object": "confirmation_token",
                "created": 1609459200,
                "livemode": false
            }
        """.trimIndent())
        
        val result = parser.parse(json)
        
        assertThat(result).isNull()
    }

    @Test
    fun parse_withCardPaymentMethodData_shouldParseCorrectly() {
        val json = JSONObject(CONFIRMATION_TOKEN_WITH_CARD_JSON)
        
        val result = parser.parse(json)
        
        assertThat(result).isNotNull()
        assertThat(result!!.paymentMethodData).isNotNull()
        assertThat(result.paymentMethodData!!.type).isEqualTo(PaymentMethod.Type.Card)
        assertThat(result.paymentMethodData!!.card).isNotNull()
        assertThat(result.paymentMethodData!!.card!!.cvcToken).isEqualTo("cvc_token_123")
        assertThat(result.paymentMethodData!!.card!!.encryptedData).isEqualTo("encrypted_card_data_abc")
    }

    @Test
    fun parse_withUSBankAccountPaymentMethodData_shouldParseCorrectly() {
        val json = JSONObject(CONFIRMATION_TOKEN_WITH_US_BANK_ACCOUNT_JSON)
        
        val result = parser.parse(json)
        
        assertThat(result).isNotNull()
        assertThat(result!!.paymentMethodData).isNotNull()
        assertThat(result.paymentMethodData!!.type).isEqualTo(PaymentMethod.Type.USBankAccount)
        assertThat(result.paymentMethodData!!.usBankAccount).isNotNull()
        assertThat(result.paymentMethodData!!.usBankAccount!!.accountHolderType)
            .isEqualTo(PaymentMethod.USBankAccount.USBankAccountHolderType.INDIVIDUAL)
        assertThat(result.paymentMethodData!!.usBankAccount!!.accountType)
            .isEqualTo(PaymentMethod.USBankAccount.USBankAccountType.CHECKING)
        assertThat(result.paymentMethodData!!.usBankAccount!!.financialConnectionsAccount)
            .isEqualTo("fca_123456")
    }

    @Test
    fun parse_withSepaDebitPaymentMethodData_shouldParseCorrectly() {
        val json = JSONObject(CONFIRMATION_TOKEN_WITH_SEPA_DEBIT_JSON)
        
        val result = parser.parse(json)
        
        assertThat(result).isNotNull()
        assertThat(result!!.paymentMethodData).isNotNull()
        assertThat(result.paymentMethodData!!.type).isEqualTo(PaymentMethod.Type.SepaDebit)
        assertThat(result.paymentMethodData!!.sepaDebit).isNotNull()
        assertThat(result.paymentMethodData!!.sepaDebit!!.iban).isEqualTo("DE89370400440532013000")
    }

    @Test
    fun parse_withBillingDetails_shouldParseCorrectly() {
        val json = JSONObject(CONFIRMATION_TOKEN_WITH_BILLING_DETAILS_JSON)
        
        val result = parser.parse(json)
        
        assertThat(result).isNotNull()
        assertThat(result!!.paymentMethodData).isNotNull()
        assertThat(result.paymentMethodData!!.billingDetails).isNotNull()
        
        val billingDetails = result.paymentMethodData!!.billingDetails!!
        assertThat(billingDetails.name).isEqualTo("Jenny Rosen")
        assertThat(billingDetails.email).isEqualTo("jenny@example.com")
        assertThat(billingDetails.phone).isEqualTo("+1234567890")
        assertThat(billingDetails.address).isNotNull()
        assertThat(billingDetails.address!!.line1).isEqualTo("510 Townsend St")
        assertThat(billingDetails.address!!.city).isEqualTo("San Francisco")
        assertThat(billingDetails.address!!.state).isEqualTo("CA")
        assertThat(billingDetails.address!!.postalCode).isEqualTo("94103")
        assertThat(billingDetails.address!!.country).isEqualTo("US")
    }

    @Test
    fun parse_withShippingDetails_shouldParseCorrectly() {
        val json = JSONObject(CONFIRMATION_TOKEN_WITH_SHIPPING_JSON)
        
        val result = parser.parse(json)
        
        assertThat(result).isNotNull()
        assertThat(result!!.shipping).isNotNull()
        
        val shipping = result.shipping!!
        assertThat(shipping.name).isEqualTo("John Doe")
        assertThat(shipping.phone).isEqualTo("+1987654321")
        assertThat(shipping.address).isNotNull()
        assertThat(shipping.address.line1).isEqualTo("123 Main St")
        assertThat(shipping.address.city).isEqualTo("New York")
        assertThat(shipping.address.state).isEqualTo("NY")
        assertThat(shipping.address.postalCode).isEqualTo("10001")
        assertThat(shipping.address.country).isEqualTo("US")
    }

    @Test
    fun parse_withSetupFutureUsageOnSession_shouldParseCorrectly() {
        val json = JSONObject("""
            {
                "id": "ctoken_1234567890",
                "object": "confirmation_token",
                "created": 1609459200,
                "livemode": false,
                "setup_future_usage": "on_session"
            }
        """.trimIndent())
        
        val result = parser.parse(json)
        
        assertThat(result).isNotNull()
        assertThat(result!!.setupFutureUsage).isEqualTo(ConfirmationToken.SetupFutureUsage.OnSession)
    }

    @Test
    fun parse_withSetupFutureUsageOffSession_shouldParseCorrectly() {
        val json = JSONObject("""
            {
                "id": "ctoken_1234567890",
                "object": "confirmation_token",
                "created": 1609459200,
                "livemode": false,
                "setup_future_usage": "off_session"
            }
        """.trimIndent())
        
        val result = parser.parse(json)
        
        assertThat(result).isNotNull()
        assertThat(result!!.setupFutureUsage).isEqualTo(ConfirmationToken.SetupFutureUsage.OffSession)
    }

    @Test
    fun parse_withInvalidSetupFutureUsage_shouldReturnNull() {
        val json = JSONObject("""
            {
                "id": "ctoken_1234567890",
                "object": "confirmation_token",
                "created": 1609459200,
                "livemode": false,
                "setup_future_usage": "invalid_usage"
            }
        """.trimIndent())
        
        val result = parser.parse(json)
        
        assertThat(result).isNotNull()
        assertThat(result!!.setupFutureUsage).isNull()
    }

    @Test
    fun parse_withPaymentMethodOptions_shouldParseCorrectly() {
        val json = JSONObject(CONFIRMATION_TOKEN_WITH_PAYMENT_METHOD_OPTIONS_JSON)
        
        val result = parser.parse(json)
        
        assertThat(result).isNotNull()
        assertThat(result!!.paymentMethodOptions).isNotNull()
        
        val options = result.paymentMethodOptions!!
        assertThat(options.card).isNotNull()
        assertThat(options.card!!.cvcToken).isEqualTo("cvc_token_456")
        assertThat(options.card!!.network).isEqualTo("visa")
        assertThat(options.card!!.setupFutureUsage).isEqualTo(ConfirmationToken.SetupFutureUsage.OnSession)
        
        assertThat(options.usBankAccount).isNotNull()
        assertThat(options.usBankAccount!!.verificationMethod).isEqualTo("automatic")
        
        assertThat(options.sepaDebit).isNotNull()
        assertThat(options.sepaDebit!!.setupFutureUsage).isEqualTo(ConfirmationToken.SetupFutureUsage.OffSession)
    }

    @Test
    fun parse_withMandateData_shouldReturnNullForMandateData() {
        // MandateDataParams doesn't have a JSON parser as noted in the implementation
        val json = JSONObject("""
            {
                "id": "ctoken_1234567890",
                "object": "confirmation_token",
                "created": 1609459200,
                "livemode": false,
                "mandate_data": {
                    "customer_acceptance": {
                        "type": "online"
                    }
                }
            }
        """.trimIndent())
        
        val result = parser.parse(json)
        
        assertThat(result).isNotNull()
        assertThat(result!!.mandateData).isNull()
    }

    @Test
    fun parse_withMetadata_shouldParseCorrectly() {
        val json = JSONObject(CONFIRMATION_TOKEN_WITH_METADATA_JSON)
        
        val result = parser.parse(json)
        
        assertThat(result).isNotNull()
        assertThat(result!!.paymentMethodData).isNotNull()
        assertThat(result.paymentMethodData!!.metadata).isNotNull()
        
        val metadata = result.paymentMethodData!!.metadata!!
        assertThat(metadata["order_id"]).isEqualTo("order_123")
        assertThat(metadata["customer_id"]).isEqualTo("cus_abc")
    }

    @Test
    fun fromJson_withValidJson_shouldReturnParsedObject() {
        val json = JSONObject(VALID_CONFIRMATION_TOKEN_JSON)
        
        val result = ConfirmationTokenJsonParser.fromJson(json)
        
        assertThat(result).isNotNull()
        assertThat(result!!.id).isEqualTo("ctoken_1234567890")
    }

    @Test
    fun fromJson_withNullJson_shouldReturnNull() {
        val result = ConfirmationTokenJsonParser.fromJson(null)
        
        assertThat(result).isNull()
    }

    companion object {
        private const val VALID_CONFIRMATION_TOKEN_JSON = """
            {
                "id": "ctoken_1234567890",
                "object": "confirmation_token",
                "created": 1609459200,
                "livemode": false,
                "return_url": "https://example.com/return"
            }
        """

        private const val CONFIRMATION_TOKEN_WITH_CARD_JSON = """
            {
                "id": "ctoken_1234567890",
                "object": "confirmation_token",
                "created": 1609459200,
                "livemode": false,
                "payment_method_data": {
                    "type": "card",
                    "card": {
                        "cvc_token": "cvc_token_123",
                        "encrypted_data": "encrypted_card_data_abc"
                    }
                }
            }
        """

        private const val CONFIRMATION_TOKEN_WITH_US_BANK_ACCOUNT_JSON = """
            {
                "id": "ctoken_1234567890",
                "object": "confirmation_token",
                "created": 1609459200,
                "livemode": false,
                "payment_method_data": {
                    "type": "us_bank_account",
                    "us_bank_account": {
                        "account_holder_type": "individual",
                        "account_type": "checking",
                        "financial_connections_account": "fca_123456"
                    }
                }
            }
        """

        private const val CONFIRMATION_TOKEN_WITH_SEPA_DEBIT_JSON = """
            {
                "id": "ctoken_1234567890",
                "object": "confirmation_token",
                "created": 1609459200,
                "livemode": false,
                "payment_method_data": {
                    "type": "sepa_debit",
                    "sepa_debit": {
                        "iban": "DE89370400440532013000"
                    }
                }
            }
        """

        private const val CONFIRMATION_TOKEN_WITH_BILLING_DETAILS_JSON = """
            {
                "id": "ctoken_1234567890",
                "object": "confirmation_token",
                "created": 1609459200,
                "livemode": false,
                "payment_method_data": {
                    "type": "card",
                    "billing_details": {
                        "name": "Jenny Rosen",
                        "email": "jenny@example.com",
                        "phone": "+1234567890",
                        "address": {
                            "line1": "510 Townsend St",
                            "city": "San Francisco",
                            "state": "CA",
                            "postal_code": "94103",
                            "country": "US"
                        }
                    }
                }
            }
        """

        private const val CONFIRMATION_TOKEN_WITH_SHIPPING_JSON = """
            {
                "id": "ctoken_1234567890",
                "object": "confirmation_token",
                "created": 1609459200,
                "livemode": false,
                "shipping": {
                    "name": "John Doe",
                    "phone": "+1987654321",
                    "address": {
                        "line1": "123 Main St",
                        "city": "New York",
                        "state": "NY",
                        "postal_code": "10001",
                        "country": "US"
                    }
                }
            }
        """

        private const val CONFIRMATION_TOKEN_WITH_PAYMENT_METHOD_OPTIONS_JSON = """
            {
                "id": "ctoken_1234567890",
                "object": "confirmation_token",
                "created": 1609459200,
                "livemode": false,
                "payment_method_options": {
                    "card": {
                        "cvc_token": "cvc_token_456",
                        "network": "visa",
                        "setup_future_usage": "on_session"
                    },
                    "us_bank_account": {
                        "verification_method": "automatic"
                    },
                    "sepa_debit": {
                        "setup_future_usage": "off_session"
                    }
                }
            }
        """

        private const val CONFIRMATION_TOKEN_WITH_METADATA_JSON = """
            {
                "id": "ctoken_1234567890",
                "object": "confirmation_token",
                "created": 1609459200,
                "livemode": false,
                "payment_method_data": {
                    "type": "card",
                    "metadata": {
                        "order_id": "order_123",
                        "customer_id": "cus_abc"
                    }
                }
            }
        """
    }
}