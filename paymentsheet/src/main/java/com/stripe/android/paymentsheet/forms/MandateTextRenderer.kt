package com.stripe.android.paymentsheet.forms

import android.text.TextUtils
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.MandateTextElement
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.paymentsheet.forms.generated.FormElementSpecV1 as FormElementSpec

/** Localized text capabilities addressable by Mint-owned JSON form specs. */
internal object MandateTextRenderer {
    fun render(
        spec: FormElementSpec,
        metadata: PaymentMethodMetadata,
        paymentMethodType: String,
        arguments: UiDefinitionFactory.Arguments,
    ): FormElement? {
        if (spec.setupFutureUsageRequired == true && !metadata.hasIntentToSetup(paymentMethodType)) {
            return null
        }

        spec.localizedTextTemplate?.let { template ->
            return MandateTextElement(
                stringResId = 0,
                args = emptyList(),
                rawText = template.replace(
                    MERCHANT_DISPLAY_NAME_PLACEHOLDER,
                    TextUtils.htmlEncode(arguments.merchantName),
                ),
            )
        }

        return when (spec.textKey) {
            "cash_app_pay" -> MandateTextElement(
                identifier = IdentifierSpec.Generic("cashapp_mandate"),
                stringResId = R.string.stripe_cash_app_pay_mandate,
                args = listOf(arguments.merchantName, arguments.merchantName),
            )
            "paypal" -> MandateTextElement(
                stringResId = R.string.stripe_paypal_mandate,
                args = listOf(arguments.merchantName),
            )
            "revolut_pay" -> MandateTextElement(
                stringResId = R.string.stripe_revolut_mandate,
                args = listOf(arguments.merchantName),
            )
            "amazon_pay" -> MandateTextElement(
                identifier = IdentifierSpec.Generic("mandate"),
                stringResId = R.string.stripe_amazon_pay_mandate,
                args = listOf(arguments.merchantName),
            )
            "satispay" -> MandateTextElement(
                stringResId = R.string.stripe_satispay_mandate,
                args = listOf(arguments.merchantName),
            )
            "twint" -> MandateTextElement(
                stringResId = R.string.stripe_twint_mandate,
                args = listOf(arguments.merchantName),
            )
            "sepa" -> MandateTextElement(
                stringResId = R.string.stripe_sepa_mandate,
                args = listOf(arguments.merchantName),
            )
            "klarna" -> MandateTextElement(
                stringResId = R.string.stripe_klarna_mandate,
                args = listOf(arguments.merchantName, arguments.merchantName),
            )
            else -> throw ServerDrivenFormRenderException(
                ServerDrivenFormRenderException.ErrorCode.UnsupportedMandateText
            )
        }
    }

    private const val MERCHANT_DISPLAY_NAME_PLACEHOLDER = "{{merchant_display_name}}"
}
