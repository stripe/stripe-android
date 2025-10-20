package com.stripe.android.ui.core

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.ClientAttributionMetadata
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentIntentCreationFlow
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodSelectionFlow
import com.stripe.android.ui.core.FieldValuesToParamsMapConverter.Companion.addPath
import com.stripe.android.ui.core.FieldValuesToParamsMapConverter.Companion.getKeys
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.forms.FormFieldEntry
import org.junit.Test

class FieldValuesToParamsMapConverterTest {
    @Test
    fun `transform to payment method with bank`() {
        val paymentMethodParams = FieldValuesToParamsMapConverter
            .transformToPaymentMethodCreateParams(
                mapOf(
                    IdentifierSpec.Generic("ideal[bank]") to FormFieldEntry(
                        "abn_amro",
                        true
                    )
                ),
                PaymentMethod.Type.Ideal.code,
                PaymentMethod.Type.Ideal.requiresMandate,
                clientAttributionMetadata = clientAttributionMetadata,
            )

        val paramMap = paymentMethodParams.toParamMap()
        assertThat(paramMap).hasSize(3)
        assertThat(paramMap).containsEntry(
            "type",
            "ideal"
        )
        assertThat(paramMap).containsEntry(
            "ideal",
            mapOf("bank" to "abn_amro")
        )
        assertThat(paramMap).containsEntry(
            "client_attribution_metadata",
            clientAttributionMetadata.toParamMap()
        )
    }

    @Test
    fun `test function`() {
        val map: MutableMap<String, Any?> = mutableMapOf("type" to "card")
        mapOf(
            IdentifierSpec.Name to FormFieldEntry(
                "joe",
                true
            ),
            IdentifierSpec.Email to FormFieldEntry(
                "joe@gmail.com",
                true
            ),
            IdentifierSpec.Generic("billing_details[address][country]") to FormFieldEntry(
                "US",
                true
            )
        ).entries.forEach {
            addPath(map, getKeys(it.key.v1), it.value.value)
        }
        assertThat(map).isEqualTo(
            mapOf(
                "type" to "card",
                "billing_details" to mapOf(
                    "name" to "joe",
                    "email" to "joe@gmail.com",
                    "address" to mapOf(
                        "country" to "US"
                    )
                )
            )
        )
    }

    @Test
    fun `transform to payment method params`() {
        val name = "joe"
        val email = "joe@gmail.com"
        val country = "US"
        val line1 = "123 Main Street"
        val paymentMethodParams = FieldValuesToParamsMapConverter
            .transformToPaymentMethodCreateParams(
                mapOf(
                    IdentifierSpec.Name to FormFieldEntry(
                        name,
                        true
                    ),
                    IdentifierSpec.Email to FormFieldEntry(
                        email,
                        true
                    ),
                    IdentifierSpec.Generic("billing_details[address][country]") to FormFieldEntry(
                        country,
                        true
                    ),
                    IdentifierSpec.Line1 to FormFieldEntry(
                        line1,
                        true
                    )
                ),
                PaymentMethod.Type.Sofort.code,
                PaymentMethod.Type.Sofort.requiresMandate,
                clientAttributionMetadata = clientAttributionMetadata
            )

        val paramsMap = paymentMethodParams.toParamMap()
        assertThat(paramsMap).hasSize(3)
        assertThat(paramsMap).containsEntry(
            "type",
            "sofort"
        )
        assertThat(paramsMap).containsEntry("billing_details", paymentMethodParams.billingDetails!!.toParamMap())
        assertThat(paramsMap).containsEntry("client_attribution_metadata", clientAttributionMetadata.toParamMap())

        assertThat(paymentMethodParams.billingDetails?.name).isEqualTo(name)
        assertThat(paymentMethodParams.billingDetails?.email).isEqualTo(email)
        assertThat(paymentMethodParams.billingDetails?.address?.country).isEqualTo(country)
        assertThat(paymentMethodParams.billingDetails?.address?.line1).isEqualTo(line1)
        assertThat(paymentMethodParams.billingDetails?.address?.postalCode).isNull()
    }

    @Test
    fun `Client attribution metadata is set correctly`() {
        val paymentMethodParams = FieldValuesToParamsMapConverter
            .transformToPaymentMethodCreateParams(
                emptyMap(),
                PaymentMethod.Type.Sofort.code,
                PaymentMethod.Type.Sofort.requiresMandate,
                clientAttributionMetadata = ClientAttributionMetadata(
                    elementsSessionConfigId = "e961790f-43ed-4fcc-a534-74eeca28d042",
                    paymentIntentCreationFlow = PaymentIntentCreationFlow.Standard,
                    paymentMethodSelectionFlow = PaymentMethodSelectionFlow.Automatic,
                )
            )

        assertThat(paymentMethodParams.toParamMap()).containsKey("client_attribution_metadata")
    }

    @Test
    fun `billing details are empty if billing details are not collected`() {
        val paymentMethodParams = FieldValuesToParamsMapConverter
            .transformToPaymentMethodCreateParams(
                emptyMap(),
                PaymentMethod.Type.Sofort.code,
                PaymentMethod.Type.Sofort.requiresMandate,
                clientAttributionMetadata = clientAttributionMetadata
            )

        assertThat(paymentMethodParams.billingDetails).isNull()
    }

    @Test
    fun `transform to payment method params - ignores options params`() {
        val paymentMethodParams = FieldValuesToParamsMapConverter
            .transformToPaymentMethodCreateParams(
                mapOf(
                    IdentifierSpec.Name to FormFieldEntry(
                        "joe",
                        true
                    ),
                    IdentifierSpec.Email to FormFieldEntry(
                        "joe@gmail.com",
                        true
                    ),
                    IdentifierSpec.Generic("billing_details[address][country]") to FormFieldEntry(
                        "US",
                        true
                    ),
                    IdentifierSpec.Line1 to FormFieldEntry(
                        "123 Main Street",
                        true
                    ),
                    IdentifierSpec.BlikCode to FormFieldEntry(
                        "example_blik_code",
                        true,
                    )
                ),
                PaymentMethod.Type.Blik.code,
                PaymentMethod.Type.Blik.requiresMandate,
                clientAttributionMetadata = clientAttributionMetadata
            )

        assertThat(
            paymentMethodParams.toParamMap()
        ).doesNotContainKey(IdentifierSpec.BlikCode)
    }

    @Test
    fun `transform to payment method params - ignores SFU and CardBrand params`() {
        val paymentMethodParams = FieldValuesToParamsMapConverter
            .transformToPaymentMethodCreateParams(
                mapOf(
                    IdentifierSpec.Name to FormFieldEntry(
                        "joe",
                        true
                    ),
                    IdentifierSpec.Email to FormFieldEntry(
                        "joe@gmail.com",
                        true
                    ),
                    IdentifierSpec.Generic("billing_details[address][country]") to FormFieldEntry(
                        "US",
                        true
                    ),
                    IdentifierSpec.Line1 to FormFieldEntry(
                        "123 Main Street",
                        true
                    ),
                    IdentifierSpec.SaveForFutureUse to FormFieldEntry(
                        "true",
                        true,
                    ),
                    IdentifierSpec.CardBrand to FormFieldEntry(
                        "visa",
                        true,
                    )
                ),
                PaymentMethod.Type.Card.code,
                PaymentMethod.Type.Card.requiresMandate,
                clientAttributionMetadata = clientAttributionMetadata
            )

        val paramsMap = paymentMethodParams.toParamMap()
        assertThat(paramsMap).hasSize(3)
        assertThat(paramsMap).containsKey("type")
        assertThat(paramsMap).containsKey("billing_details")
        assertThat(paramsMap).containsKey("client_attribution_metadata")
    }

    @Test
    fun `transform to extra params if bacs`() {
        val paymentMethodParams = FieldValuesToParamsMapConverter
            .transformToPaymentMethodExtraParams(
                mapOf(
                    IdentifierSpec.Name to FormFieldEntry(
                        "joe",
                        true
                    ),
                    IdentifierSpec.BacsDebitConfirmed to FormFieldEntry(
                        "true",
                        true
                    )
                ),
                PaymentMethod.Type.BacsDebit.code
            )

        assertThat(
            paymentMethodParams?.toParamMap().toString()
        ).isEqualTo("{bacs_debit={confirmed=true}}")
    }

    @Test
    fun `test ignored fields`() {
        val paymentMethodParams = FieldValuesToParamsMapConverter
            .transformToPaymentMethodCreateParams(
                mapOf(
                    IdentifierSpec.Name to FormFieldEntry(
                        "joe",
                        true
                    ),
                    IdentifierSpec.SameAsShipping to FormFieldEntry(
                        "true",
                        true
                    )
                ),
                "some code",
                false,
                clientAttributionMetadata = clientAttributionMetadata
            )

        val paramsMap = paymentMethodParams.toParamMap()
        assertThat(paramsMap).hasSize(3)
        assertThat(paramsMap).containsEntry(
            "type",
            "some code"
        )
        assertThat(paramsMap).containsEntry("billing_details", mapOf("name" to "joe"))
        assertThat(paramsMap).containsEntry("client_attribution_metadata", clientAttributionMetadata.toParamMap())
    }

    @Test
    fun `transformToPaymentMethodOptionsParams returns correct params for Card with setupFutureUsage`() {
        val paymentMethodParams = FieldValuesToParamsMapConverter
            .transformToPaymentMethodOptionsParams(
                fieldValuePairs = emptyMap(),
                code = PaymentMethod.Type.Card.code,
                setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession,
            )

        assertThat(paymentMethodParams).isNotNull()
        assertThat(
            paymentMethodParams?.toParamMap().toString()
        ).isEqualTo(
            "{card={setup_future_usage=off_session}}"
        )
    }

    @Test
    fun `transformToPaymentMethodOptionsParams returns correct params for Card without setupFutureUsage`() {
        val paymentMethodParams = FieldValuesToParamsMapConverter
            .transformToPaymentMethodOptionsParams(
                fieldValuePairs = emptyMap(),
                code = PaymentMethod.Type.Card.code,
                setupFutureUsage = null
            )

        assertThat(paymentMethodParams).isNotNull()
        assertThat(paymentMethodParams?.toParamMap().toString()).isEqualTo("{}")
    }

    @Test
    fun `transformToPaymentMethodOptionsParams returns correct params for Blik`() {
        val paymentMethodParams = FieldValuesToParamsMapConverter
            .transformToPaymentMethodOptionsParams(
                mapOf(
                    IdentifierSpec.Name to FormFieldEntry(
                        "joe",
                        true
                    ),
                    IdentifierSpec.Email to FormFieldEntry(
                        "joe@gmail.com",
                        true
                    ),
                    IdentifierSpec.Generic("billing_details[address][country]") to FormFieldEntry(
                        "US",
                        true
                    ),
                    IdentifierSpec.Line1 to FormFieldEntry(
                        "123 Main Street",
                        true
                    ),
                    IdentifierSpec.BlikCode to FormFieldEntry(
                        "example_blik_code",
                        true,
                    )
                ),
                PaymentMethod.Type.Blik.code,
            )

        assertThat(paymentMethodParams).isNotNull()
        assertThat(
            paymentMethodParams?.toParamMap().toString()
        ).isEqualTo(
            "{blik={code=example_blik_code}}"
        )
    }

    @Test
    fun `transformToPaymentMethodOptionsParams returns correct params for WeChat`() {
        val paymentMethodParams = FieldValuesToParamsMapConverter
            .transformToPaymentMethodOptionsParams(
                mapOf(
                    IdentifierSpec.Name to FormFieldEntry(
                        "joe",
                        true
                    ),
                    IdentifierSpec.Email to FormFieldEntry(
                        "joe@gmail.com",
                        true
                    ),
                    IdentifierSpec.Generic("billing_details[address][country]") to FormFieldEntry(
                        "US",
                        true
                    ),
                    IdentifierSpec.Line1 to FormFieldEntry(
                        "123 Main Street",
                        true
                    ),
                ),
                PaymentMethod.Type.WeChatPay.code,
            )

        assertThat(paymentMethodParams).isNotNull()
        assertThat(
            paymentMethodParams?.toParamMap().toString()
        ).isEqualTo("{wechat_pay={client=mobile_web}}")
    }

    @Test
    fun `transformToPaymentMethodOptionsParams returns correct params for Konbini`() {
        val paymentMethodParams = FieldValuesToParamsMapConverter
            .transformToPaymentMethodOptionsParams(
                mapOf(
                    IdentifierSpec.Name to FormFieldEntry(
                        "joe",
                        true
                    ),
                    IdentifierSpec.Email to FormFieldEntry(
                        "joe@gmail.com",
                        true
                    ),
                    IdentifierSpec.Generic("billing_details[address][country]") to FormFieldEntry(
                        "US",
                        true
                    ),
                    IdentifierSpec.Line1 to FormFieldEntry(
                        "123 Main Street",
                        true
                    ),
                    IdentifierSpec.KonbiniConfirmationNumber to FormFieldEntry(
                        "example_confirmation_number",
                        true,
                    )
                ),
                PaymentMethod.Type.Konbini.code,
            )

        assertThat(paymentMethodParams).isNotNull()
        assertThat(
            paymentMethodParams?.toParamMap().toString()
        ).isEqualTo("{konbini={confirmation_number=example_confirmation_number}}")
    }

    @Test
    fun `transformToPaymentMethodOptionsParams returns correct params for SepaDebit with setupFutureUsage`() {
        val paymentMethodParams = FieldValuesToParamsMapConverter
            .transformToPaymentMethodOptionsParams(
                fieldValuePairs = emptyMap(),
                code = PaymentMethod.Type.SepaDebit.code,
                setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession,
            )

        assertThat(paymentMethodParams).isNotNull()
        assertThat(
            paymentMethodParams?.toParamMap().toString()
        ).isEqualTo(
            "{sepa_debit={setup_future_usage=off_session}}"
        )
    }

    @Test
    fun `transformToPaymentMethodOptionsParams returns correct params for SepaDebit without setupFutureUsage`() {
        val paymentMethodParams = FieldValuesToParamsMapConverter
            .transformToPaymentMethodOptionsParams(
                fieldValuePairs = emptyMap(),
                code = PaymentMethod.Type.SepaDebit.code,
                setupFutureUsage = null
            )

        assertThat(paymentMethodParams).isNotNull()
        assertThat(paymentMethodParams?.toParamMap().toString()).isEqualTo("{}")
    }

    @Test
    fun `transformToPaymentMethodExtraParams returns correct params for SepaDebit with setAsDefault`() {
        val paymentMethodExtraParams = FieldValuesToParamsMapConverter
            .transformToPaymentMethodExtraParams(
                mapOf(
                    IdentifierSpec.SetAsDefaultPaymentMethod to FormFieldEntry(
                        "true",
                        true
                    ),
                ),
                PaymentMethod.Type.SepaDebit.code,
            )

        assertThat(paymentMethodExtraParams).isNotNull()
        assertThat(paymentMethodExtraParams?.toParamMap()).isEqualTo(
            mapOf(
                "sepa_debit" to mapOf(
                    "set_as_default_payment_method" to "true"
                )
            )
        )
    }

    @Test
    fun `transformToPaymentMethodExtraParams returns correct params for SepaDebit without setAsDefault`() {
        val paymentMethodExtraParams = FieldValuesToParamsMapConverter
            .transformToPaymentMethodExtraParams(
                emptyMap(),
                PaymentMethod.Type.SepaDebit.code,
            )

        assertThat(paymentMethodExtraParams).isNotNull()
        assertThat(paymentMethodExtraParams?.toParamMap().toString()).isEqualTo("{}")
    }

    @Test
    fun `allowRedisplay is set to UNSPECIFIED in param map`() {
        val paymentMethodParams = FieldValuesToParamsMapConverter
            .transformToPaymentMethodCreateParams(
                fieldValuePairs = fieldValuePairs,
                code = PaymentMethod.Type.Ideal.code,
                requiresMandate = PaymentMethod.Type.Ideal.requiresMandate,
                allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
                clientAttributionMetadata = clientAttributionMetadata,
            )

        assertThat(paymentMethodParams.toParamMap()).containsEntry(
            "allow_redisplay",
            "unspecified"
        )
    }

    @Test
    fun `allowRedisplay is set to LIMITED in param map`() {
        val paymentMethodParams = FieldValuesToParamsMapConverter
            .transformToPaymentMethodCreateParams(
                fieldValuePairs = fieldValuePairs,
                code = PaymentMethod.Type.Ideal.code,
                requiresMandate = PaymentMethod.Type.Ideal.requiresMandate,
                PaymentMethod.AllowRedisplay.LIMITED,
                clientAttributionMetadata,
            )

        assertThat(paymentMethodParams.toParamMap()).containsEntry(
            "allow_redisplay",
            "limited"
        )
    }

    @Test
    fun `allowRedisplay is set to ALWAYS in param map`() {
        val paymentMethodParams = FieldValuesToParamsMapConverter
            .transformToPaymentMethodCreateParams(
                fieldValuePairs = fieldValuePairs,
                code = PaymentMethod.Type.Ideal.code,
                requiresMandate = PaymentMethod.Type.Ideal.requiresMandate,
                PaymentMethod.AllowRedisplay.ALWAYS,
                clientAttributionMetadata,
            )

        assertThat(paymentMethodParams.toParamMap()).containsEntry(
            "allow_redisplay",
            "always"
        )
    }

    private companion object {
        val fieldValuePairs = mapOf(
            IdentifierSpec.Generic("ideal[bank]") to FormFieldEntry(
                "abn_amro",
                true
            )
        )

        val clientAttributionMetadata = ClientAttributionMetadata(
            elementsSessionConfigId = "elements_session_123",
            paymentIntentCreationFlow = PaymentIntentCreationFlow.Standard,
            paymentMethodSelectionFlow = PaymentMethodSelectionFlow.Automatic,
        )
    }
}
