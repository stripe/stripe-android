package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

class ConfirmPaymentIntentParamsTest {

    @Test
    fun toParamMap_withSourceParams_shouldCreateExpectedMap() {
        val sourceParams = SourceParams.createCardParams(CardParamsFixtures.MINIMUM)
        val params = ConfirmPaymentIntentParams
            .createWithSourceParams(sourceParams, CLIENT_SECRET, RETURN_URL)
            .toParamMap()

        assertThat(params)
            .isEqualTo(
                mapOf(
                    "client_secret" to CLIENT_SECRET,
                    "return_url" to RETURN_URL,
                    "use_stripe_sdk" to false,
                    "source_data" to sourceParams.toParamMap()
                )
            )
    }

    @Test
    fun toParamMap_withSourceId_shouldCreateExpectedMap() {
        val params = ConfirmPaymentIntentParams
            .createWithSourceId(SOURCE_ID, CLIENT_SECRET, RETURN_URL)
            .toParamMap()

        assertThat(params)
            .isEqualTo(
                mapOf(
                    "client_secret" to CLIENT_SECRET,
                    "return_url" to RETURN_URL,
                    "use_stripe_sdk" to false,
                    "source" to SOURCE_ID
                )
            )
    }

    @Test
    fun toParamMap_withSavePaymentMethod_shouldCreateExpectedMap() {
        val params = ConfirmPaymentIntentParams
            .createWithSourceId(SOURCE_ID, CLIENT_SECRET, RETURN_URL, true)
            .toParamMap()

        assertThat(params)
            .isEqualTo(
                mapOf(
                    "client_secret" to CLIENT_SECRET,
                    "return_url" to RETURN_URL,
                    "use_stripe_sdk" to false,
                    "source" to SOURCE_ID,
                    "save_payment_method" to true
                )
            )
    }

    @Test
    fun toParamMap_withPaymentMethodCreateParams_shouldCreateExpectedMap() {
        val params = ConfirmPaymentIntentParams
            .createWithPaymentMethodCreateParams(
                PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                CLIENT_SECRET
            )
            .toParamMap()

        assertThat(params)
            .isEqualTo(
                mapOf(
                    "client_secret" to CLIENT_SECRET,
                    "use_stripe_sdk" to false,
                    "payment_method_data" to PaymentMethodCreateParamsFixtures.DEFAULT_CARD.toParamMap()
                )
            )
    }

    @Test
    fun toParamMap_withSetAsDefaultPaymentMethod_shouldCreateExpectedMap() {
        val params = ConfirmPaymentIntentParams
            .createWithSetAsDefaultPaymentMethod(
                PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                CLIENT_SECRET,
                setAsDefaultPaymentMethod = true,
            )
            .toParamMap()

        assertThat(params)
            .isEqualTo(
                mapOf(
                    "client_secret" to CLIENT_SECRET,
                    "use_stripe_sdk" to false,
                    "payment_method_data" to PaymentMethodCreateParamsFixtures.DEFAULT_CARD.toParamMap(),
                    "set_as_default_payment_method" to true,
                )
            )
    }

    @Test
    fun toParamMap_withPaymentMethodId_shouldCreateExpectedMap() {
        val params = ConfirmPaymentIntentParams
            .createWithPaymentMethodId(PM_ID, CLIENT_SECRET)
            .toParamMap()

        assertThat(params)
            .isEqualTo(
                mapOf(
                    "client_secret" to CLIENT_SECRET,
                    "use_stripe_sdk" to false,
                    "payment_method" to PM_ID
                )
            )
    }

    @Test
    fun toParamMap_withPaymentMethodCreateParamsAndSavePaymentMethod_shouldCreateExpectedMap() {
        val paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD
        val params = ConfirmPaymentIntentParams
            .createWithPaymentMethodCreateParams(
                paymentMethodCreateParams,
                CLIENT_SECRET,
                savePaymentMethod = true
            )
            .toParamMap()

        assertThat(params)
            .isEqualTo(
                mapOf(
                    "client_secret" to CLIENT_SECRET,
                    "use_stripe_sdk" to false,
                    "payment_method_data" to PaymentMethodCreateParamsFixtures.DEFAULT_CARD.toParamMap(),
                    "save_payment_method" to true
                )
            )
    }

    @Test
    fun toParamMap_withPaymentMethodIdAndSavePaymentMethod_shouldCreateExpectedMap() {
        val params = ConfirmPaymentIntentParams
            .createWithPaymentMethodId(PM_ID, CLIENT_SECRET, true)
            .toParamMap()

        assertThat(params)
            .isEqualTo(
                mapOf(
                    "client_secret" to CLIENT_SECRET,
                    "use_stripe_sdk" to false,
                    "payment_method" to PM_ID,
                    "save_payment_method" to true
                )
            )
    }

    @Test
    fun toParamMap_withExtraParams_shouldCreateExpectedMap() {
        val params = ConfirmPaymentIntentParams
            .createWithPaymentMethodId(
                "pm_123",
                CLIENT_SECRET,
                setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OnSession
            )
            .toParamMap()

        assertThat(params)
            .isEqualTo(
                mapOf(
                    "client_secret" to CLIENT_SECRET,
                    "use_stripe_sdk" to false,
                    "payment_method" to "pm_123",
                    "setup_future_usage" to "on_session"
                )
            )
    }

    @Test
    fun toParamMap_withClientSecret_shouldCreateExpectedMap() {
        assertThat(
            ConfirmPaymentIntentParams.create(CLIENT_SECRET).toParamMap()
        ).isEqualTo(
            mapOf(
                "client_secret" to CLIENT_SECRET,
                "use_stripe_sdk" to false
            )
        )
    }

    @Test
    fun shouldUseStripeSdk() {
        val params = ConfirmPaymentIntentParams.create(CLIENT_SECRET)

        assertThat(params.shouldUseStripeSdk())
            .isFalse()

        assertThat(
            params
                .withShouldUseStripeSdk(true)
                .shouldUseStripeSdk()
        ).isTrue()
    }

    @Test
    fun toParamMap_withSepaDebitPaymentMethodParams_shouldUseDefaultMandateDataIfNotSpecified() {
        assertThat(
            ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                clientSecret = CLIENT_SECRET,
                paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.DEFAULT_SEPA_DEBIT,
                savePaymentMethod = false
            ).toParamMap()
        ).isEqualTo(
            mapOf(
                "client_secret" to CLIENT_SECRET,
                "save_payment_method" to false,
                "use_stripe_sdk" to false,
                "mandate_data" to mapOf(
                    "customer_acceptance" to mapOf(
                        "type" to "online",
                        "online" to mapOf(
                            "infer_from_client" to true
                        )
                    )
                ),
                "payment_method_data" to mapOf(
                    "type" to "sepa_debit",
                    "sepa_debit" to mapOf(
                        "iban" to "my_iban"
                    )
                )
            )
        )
    }

    @Test
    fun toParamMap_withIdealPaymentMethodParams_shouldUseDefaultMandateDataIfNotSpecified() {
        assertThat(
            ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                clientSecret = CLIENT_SECRET,
                paymentMethodCreateParams = PaymentMethodCreateParams.create(
                    PaymentMethodCreateParams.Ideal(bank = "my_bank")
                ),
                savePaymentMethod = false
            ).toParamMap()
        ).isEqualTo(
            mapOf(
                "client_secret" to CLIENT_SECRET,
                "save_payment_method" to false,
                "use_stripe_sdk" to false,
                "mandate_data" to mapOf(
                    "customer_acceptance" to mapOf(
                        "type" to "online",
                        "online" to mapOf(
                            "infer_from_client" to true
                        )
                    )
                ),
                "payment_method_data" to mapOf(
                    "type" to "ideal",
                    "ideal" to mapOf(
                        "bank" to "my_bank"
                    )
                )
            )
        )
    }

    @Test
    fun toParamMap_withSepaDebitPaymentMethodParams_shouldUseMandateDataIfSpecified() {
        assertThat(
            ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                clientSecret = CLIENT_SECRET,
                paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.DEFAULT_SEPA_DEBIT,
                mandateData = MandateDataParamsFixtures.DEFAULT,
                savePaymentMethod = false
            ).toParamMap()
        ).isEqualTo(
            mapOf(
                "client_secret" to CLIENT_SECRET,
                "save_payment_method" to false,
                "use_stripe_sdk" to false,
                "mandate_data" to mapOf(
                    "customer_acceptance" to mapOf(
                        "type" to "online",
                        "online" to mapOf(
                            "ip_address" to "127.0.0.1",
                            "user_agent" to "my_user_agent"
                        )
                    )
                ),
                "payment_method_data" to mapOf(
                    "type" to "sepa_debit",
                    "sepa_debit" to mapOf(
                        "iban" to "my_iban"
                    )
                )
            )
        )
    }

    @Test
    fun toParamMap_withSepaDebitPaymentMethodParams_shouldUseMandateIdIfSpecified() {
        assertThat(
            ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                clientSecret = CLIENT_SECRET,
                paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.DEFAULT_SEPA_DEBIT,
                mandateId = "mandate_123456789",
                savePaymentMethod = false
            ).toParamMap()
        ).isEqualTo(
            mapOf(
                "client_secret" to CLIENT_SECRET,
                "save_payment_method" to false,
                "use_stripe_sdk" to false,
                "mandate" to "mandate_123456789",
                "payment_method_data" to mapOf(
                    "type" to "sepa_debit",
                    "sepa_debit" to mapOf(
                        "iban" to "my_iban"
                    )
                )
            )
        )
    }

    @Test
    fun toParamMap_withSepaDebitPaymentMethodId_shouldUseMandateDataIfSpecified() {
        assertThat(
            ConfirmPaymentIntentParams.createWithPaymentMethodId(
                clientSecret = CLIENT_SECRET,
                paymentMethodId = "pm_12345",
                mandateData = MandateDataParamsFixtures.DEFAULT
            ).toParamMap()
        ).isEqualTo(
            mapOf(
                "client_secret" to CLIENT_SECRET,
                "use_stripe_sdk" to false,
                "mandate_data" to mapOf(
                    "customer_acceptance" to mapOf(
                        "type" to "online",
                        "online" to mapOf(
                            "ip_address" to "127.0.0.1",
                            "user_agent" to "my_user_agent"
                        )
                    )
                ),
                "payment_method" to "pm_12345"
            )
        )
    }

    @Test
    fun toParamMap_withCardPaymentMethodOptions_shouldCreateExpectedMap() {
        assertThat(
            ConfirmPaymentIntentParams(
                paymentMethodId = "pm_123",
                paymentMethodOptions = PaymentMethodOptionsParams.Card(
                    cvc = "123"
                ),
                clientSecret = CLIENT_SECRET
            ).toParamMap()
        ).isEqualTo(
            mapOf(
                "payment_method" to "pm_123",
                "payment_method_options" to mapOf("card" to mapOf("cvc" to "123")),
                "client_secret" to CLIENT_SECRET,
                "use_stripe_sdk" to false
            )
        )
    }

    @Test
    fun toParamMap_withBlikPaymentMethodOptions_shouldCreateExpectedMap() {
        val blikCode = "123456"
        assertThat(
            ConfirmPaymentIntentParams(
                paymentMethodId = "pm_123",
                paymentMethodOptions = PaymentMethodOptionsParams.Blik(
                    code = blikCode
                ),
                clientSecret = CLIENT_SECRET
            ).toParamMap()
        ).isEqualTo(
            mapOf(
                "payment_method" to "pm_123",
                "payment_method_options" to mapOf(
                    PaymentMethod.Type.Blik.code to mapOf(
                        PaymentMethodOptionsParams.Blik.PARAM_CODE to blikCode
                    )
                ),
                "client_secret" to CLIENT_SECRET,
                "use_stripe_sdk" to false
            )
        )
    }

    @Test
    fun toParamMap_withWeChatPayPaymentMethodOptions_shouldCreateExpectedMap() {
        val appId = "appId123456"
        assertThat(
            ConfirmPaymentIntentParams(
                paymentMethodId = "pm_123",
                paymentMethodOptions = PaymentMethodOptionsParams.WeChatPay(
                    appId = appId
                ),
                clientSecret = CLIENT_SECRET
            ).toParamMap()
        ).isEqualTo(
            mapOf(
                "payment_method" to "pm_123",
                "payment_method_options" to mapOf(
                    PaymentMethod.Type.WeChatPay.code to mapOf(
                        PaymentMethodOptionsParams.WeChatPay.PARAM_CLIENT to "android",
                        PaymentMethodOptionsParams.WeChatPay.PARAM_APP_ID to appId
                    )
                ),
                "client_secret" to CLIENT_SECRET,
                "use_stripe_sdk" to false
            )
        )
    }

    @Test
    fun toParamMap_withUSBankAccountPaymentMethodOptions_shouldCreateExpectedMap() {
        assertThat(
            ConfirmPaymentIntentParams(
                paymentMethodId = "pm_123",
                paymentMethodOptions = PaymentMethodOptionsParams.USBankAccount(
                    setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
                ),
                clientSecret = CLIENT_SECRET
            ).toParamMap()
        ).isEqualTo(
            mapOf(
                "payment_method" to "pm_123",
                "payment_method_options" to mapOf(
                    "us_bank_account" to mapOf(
                        "setup_future_usage" to "off_session"
                    )
                ),
                "client_secret" to CLIENT_SECRET,
                "use_stripe_sdk" to false
            )
        )
    }

    @Test
    fun toParamMap_withReceiptEmail_shouldCreateExpectedMap() {
        assertThat(
            ConfirmPaymentIntentParams(
                paymentMethodId = "pm_123",
                clientSecret = CLIENT_SECRET,
                receiptEmail = "jenny.rosen@example.com"
            ).toParamMap()
        ).isEqualTo(
            mapOf(
                "client_secret" to CLIENT_SECRET,
                "use_stripe_sdk" to false,
                "payment_method" to "pm_123",
                "receipt_email" to "jenny.rosen@example.com"
            )
        )
    }

    @Test
    fun shipping_toParamMap_shouldReturnExpectedMap() {
        val shipping = ConfirmPaymentIntentParams.Shipping(
            address = Address.Builder()
                .setCity("San Francisco")
                .setCountry("US")
                .setLine1("123 Market St")
                .setLine2("#345")
                .setPostalCode("94107")
                .setState("CA")
                .build(),
            name = "Jenny Rosen",
            carrier = "Fedex",
            trackingNumber = "12345"
        )
        assertThat(shipping.toParamMap())
            .isEqualTo(
                mapOf(
                    "address" to mapOf(
                        "line1" to "123 Market St",
                        "line2" to "#345",
                        "city" to "San Francisco",
                        "state" to "CA",
                        "postal_code" to "94107",
                        "country" to "US"
                    ),
                    "name" to "Jenny Rosen",
                    "carrier" to "Fedex",
                    "tracking_number" to "12345"
                )
            )
    }

    @Test
    fun toParamMap_withAlipay_shouldCreateExpectedMap() {
        assertThat(
            ConfirmPaymentIntentParams.createAlipay(
                CLIENT_SECRET
            ).toParamMap()
        )
            .isEqualTo(
                mapOf(
                    "client_secret" to CLIENT_SECRET,
                    "use_stripe_sdk" to false,
                    "return_url" to "stripe://return_url",
                    "payment_method_data" to mapOf(
                        "type" to "alipay"
                    )
                )
            )
    }

    @Test
    fun create_withAttachedPaymentMethodRequiringMandate_shouldIncludeDefaultMandateParams() {
        val params = ConfirmPaymentIntentParams
            .create(CLIENT_SECRET, PaymentMethod.Type.USBankAccount)
            .toParamMap()

        assertThat(params)
            .isEqualTo(
                mapOf(
                    "client_secret" to CLIENT_SECRET,
                    "use_stripe_sdk" to false,
                    "mandate_data" to mapOf(
                        "customer_acceptance" to mapOf(
                            "type" to "online",
                            "online" to mapOf(
                                "infer_from_client" to true
                            )
                        )
                    )
                )
            )
    }

    @Test
    fun create_withAttachedPaymentMethodNotRequiringMandate_shouldNotIncludeMandateParams() {
        val params = ConfirmPaymentIntentParams
            .create(CLIENT_SECRET, PaymentMethod.Type.Affirm)
            .toParamMap()

        assertThat(params)
            .isEqualTo(
                mapOf(
                    "client_secret" to CLIENT_SECRET,
                    "use_stripe_sdk" to false
                )
            )
    }

    private companion object {
        private const val CLIENT_SECRET = "pi_1CkiBMLENEVhOs7YMtUehLau_secret_s4O8SDh7s6spSmHDw1VaYPGZA"

        private const val RETURN_URL = "stripe://return_url"
        private const val SOURCE_ID = "src_123testsourceid"
        private const val PM_ID = "pm_123456789"
    }
}
