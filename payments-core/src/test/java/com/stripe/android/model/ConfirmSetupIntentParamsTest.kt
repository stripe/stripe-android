package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

class ConfirmSetupIntentParamsTest {

    @Test
    fun shouldUseStripeSdk_shouldReturnExpectedValues() {
        val params = ConfirmSetupIntentParams.create(
            "pm_123",
            CLIENT_SECRET
        )

        assertThat(params.shouldUseStripeSdk())
            .isFalse()

        assertThat(
            params
                .withShouldUseStripeSdk(true)
                .shouldUseStripeSdk()
        ).isTrue()
    }

    @Test
    fun toParamMap_withPaymentMethodId_shouldCreateExpectedMap() {
        assertThat(
            ConfirmSetupIntentParams.create(
                "pm_12345",
                CLIENT_SECRET
            ).toParamMap()
        ).isEqualTo(
            mapOf(
                "client_secret" to CLIENT_SECRET,
                "use_stripe_sdk" to false,
                "payment_method" to "pm_12345"
            )
        )
    }

    @Test
    fun toParamMap_withPaymentMethodCreateParams_shouldCreateExpectedMap() {
        assertThat(
            ConfirmSetupIntentParams.create(
                PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                CLIENT_SECRET
            ).toParamMap()
        ).isEqualTo(
            mapOf(
                "client_secret" to CLIENT_SECRET,
                "use_stripe_sdk" to false,
                "payment_method_data" to PaymentMethodCreateParamsFixtures.DEFAULT_CARD.toParamMap()
            )
        )
    }

    @Test
    fun toParamMap_withoutPaymentMethod_shouldCreateExpectedMap() {
        assertThat(
            ConfirmSetupIntentParams.createWithoutPaymentMethod(CLIENT_SECRET)
                .toParamMap()
        ).isEqualTo(
            mapOf(
                "client_secret" to CLIENT_SECRET,
                "use_stripe_sdk" to false
            )
        )
    }

    @Test
    fun toParamMap_withSetAsDefaultPaymentMethod_shouldCreateExpectedMap() {
        assertThat(
            ConfirmSetupIntentParams.createWithSetAsDefaultPaymentMethod(
                paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                clientSecret = CLIENT_SECRET,
                setAsDefaultPaymentMethod = true
            ).toParamMap()
        ).isEqualTo(
            mapOf(
                "client_secret" to CLIENT_SECRET,
                "use_stripe_sdk" to false,
                "payment_method_data" to PaymentMethodCreateParamsFixtures.DEFAULT_CARD.toParamMap(),
                "set_as_default_payment_method" to true,
            )
        )
    }

    @Test
    fun toParamMap_withSepaDebitPaymentMethodParams_shouldUseDefaultMandateDataIfNotSpecified() {
        assertThat(
            ConfirmSetupIntentParams.create(
                clientSecret = CLIENT_SECRET,
                paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.DEFAULT_SEPA_DEBIT
            ).toParamMap()
        ).isEqualTo(
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
    fun toParamMap_withSepaDebitPaymentMethodParams_shouldUseMandateDataIfSpecified() {
        assertThat(
            ConfirmSetupIntentParams.create(
                clientSecret = CLIENT_SECRET,
                paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.DEFAULT_SEPA_DEBIT,
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
            ConfirmSetupIntentParams.create(
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
    fun toParamMap_withSepaDebitPaymentMethodParams_shouldUseMandateIdIfSpecified() {
        assertThat(
            ConfirmSetupIntentParams.create(
                clientSecret = CLIENT_SECRET,
                paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.DEFAULT_SEPA_DEBIT,
                mandateId = "mandate_123456789"
            ).toParamMap()
        ).isEqualTo(
            mapOf(
                "client_secret" to CLIENT_SECRET,
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
    fun create_withAttachedPaymentMethodRequiringMandate_shouldIncludeDefaultMandateParams() {
        val params = ConfirmSetupIntentParams
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
        val params = ConfirmSetupIntentParams
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
        private const val CLIENT_SECRET = "seti_1CkiBMLENEVhOs7YMtUehLau_secret_sw1VaYPGZA"
    }
}
