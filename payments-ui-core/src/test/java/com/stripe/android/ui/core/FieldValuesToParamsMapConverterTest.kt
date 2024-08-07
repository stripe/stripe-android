package com.stripe.android.ui.core

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethod
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
                PaymentMethod.Type.Ideal.requiresMandate
            )

        assertThat(paymentMethodParams.toParamMap().toString().replace("\\s".toRegex(), ""))
            .isEqualTo(
                """
                    {type=ideal,ideal={bank=abn_amro}}
                """.trimIndent()
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
                PaymentMethod.Type.Sofort.requiresMandate
            )

        assertThat(
            paymentMethodParams.toParamMap().toString()
        ).isEqualTo(
            "{" +
                "type=sofort, " +
                "billing_details={" +
                "name=joe, " +
                "email=joe@gmail.com, " +
                "address={" +
                "country=US, " +
                "line1=123 Main Street" +
                "}" +
                "}" +
                "}"
        )
        assertThat(paymentMethodParams.billingDetails?.name).isEqualTo(name)
        assertThat(paymentMethodParams.billingDetails?.email).isEqualTo(email)
        assertThat(paymentMethodParams.billingDetails?.address?.country).isEqualTo(country)
        assertThat(paymentMethodParams.billingDetails?.address?.line1).isEqualTo(line1)
        assertThat(paymentMethodParams.billingDetails?.address?.postalCode).isNull()
    }

    @Test
    fun `billing details are empty if billing details are not collected`() {
        val paymentMethodParams = FieldValuesToParamsMapConverter
            .transformToPaymentMethodCreateParams(
                emptyMap(),
                PaymentMethod.Type.Sofort.code,
                PaymentMethod.Type.Sofort.requiresMandate
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
                PaymentMethod.Type.Blik.requiresMandate
            )

        assertThat(
            paymentMethodParams.toParamMap().toString()
        ).isEqualTo(
            "{" +
                "type=blik, " +
                "billing_details={" +
                "name=joe, " +
                "email=joe@gmail.com, " +
                "address={" +
                "country=US, " +
                "line1=123 Main Street" +
                "}" +
                "}" +
                "}"
        )
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
                PaymentMethod.Type.Card.requiresMandate
            )

        assertThat(
            paymentMethodParams.toParamMap().toString()
        ).isEqualTo(
            "{" +
                "type=card, " +
                "billing_details={" +
                "name=joe, " +
                "email=joe@gmail.com, " +
                "address={" +
                "country=US, " +
                "line1=123 Main Street" +
                "}" +
                "}" +
                "}"
        )
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
                false
            )

        assertThat(
            paymentMethodParams.toParamMap().toString()
        ).isEqualTo(
            "{type=some code, billing_details={name=joe}}"
        )
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
    fun `transformToPaymentMethodOptionsParams returns null for card`() {
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
                PaymentMethod.Type.Card.code,
            )

        assertThat(paymentMethodParams).isNull()
    }

    @Test
    fun `allowRedisplay is set to UNSPECIFIED in param map`() {
        val paymentMethodParams = FieldValuesToParamsMapConverter
            .transformToPaymentMethodCreateParams(
                fieldValuePairs = fieldValuePairs,
                code = PaymentMethod.Type.Ideal.code,
                requiresMandate = PaymentMethod.Type.Ideal.requiresMandate,
                allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
            )

        assertThat(paymentMethodParams.toParamMap().toString().replace("\\s".toRegex(), ""))
            .isEqualTo(
                """
                    {type=ideal,ideal={bank=abn_amro},allow_redisplay=unspecified}
                """.trimIndent()
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
            )

        assertThat(paymentMethodParams.toParamMap().toString().replace("\\s".toRegex(), ""))
            .isEqualTo(
                """
                    {type=ideal,ideal={bank=abn_amro},allow_redisplay=limited}
                """.trimIndent()
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
            )

        assertThat(paymentMethodParams.toParamMap().toString().replace("\\s".toRegex(), ""))
            .isEqualTo(
                """
                    {type=ideal,ideal={bank=abn_amro},allow_redisplay=always}
                """.trimIndent()
            )
    }

    private companion object {
        val fieldValuePairs = mapOf(
            IdentifierSpec.Generic("ideal[bank]") to FormFieldEntry(
                "abn_amro",
                true
            )
        )
    }
}
