package com.stripe.android.lpmfoundations.luxe

import com.stripe.android.lpmfoundations.paymentmethod.IS_PAYMENT_METHOD_SET_AS_DEFAULT_ENABLED_DEFAULT_VALUE
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.ui.core.elements.SaveForFutureUseElement
import com.stripe.android.ui.core.elements.SetAsDefaultPaymentMethodElement
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec

internal fun isSaveForFutureUseValueChangeable(
    code: PaymentMethodCode,
    metadata: PaymentMethodMetadata
): Boolean {
    return isSaveForFutureUseValueChangeable(
        code = code,
        intent = metadata.stripeIntent,
        paymentMethodSaveConsentBehavior = metadata.paymentMethodSaveConsentBehavior,
        hasCustomerConfiguration = metadata.customerMetadata?.hasCustomerConfiguration ?: false,
    )
}

internal fun isSaveForFutureUseValueChangeable(
    code: PaymentMethodCode,
    paymentMethodSaveConsentBehavior: PaymentMethodSaveConsentBehavior,
    intent: StripeIntent,
    hasCustomerConfiguration: Boolean,
): Boolean {
    return when (paymentMethodSaveConsentBehavior) {
        is PaymentMethodSaveConsentBehavior.Disabled -> false
        is PaymentMethodSaveConsentBehavior.Enabled -> hasCustomerConfiguration
        is PaymentMethodSaveConsentBehavior.Legacy -> {
            when (intent) {
                is PaymentIntent -> {
                    val isSetupFutureUsageSet = intent.isSetupFutureUsageSet(code)

                    if (isSetupFutureUsageSet) {
                        false
                    } else {
                        hasCustomerConfiguration
                    }
                }

                is SetupIntent -> {
                    false
                }
            }
        }
    }
}

internal fun MutableList<FormElement>.addSavePaymentOptionElements(
    metadata: PaymentMethodMetadata,
    arguments: UiDefinitionFactory.Arguments,
): Boolean {
    val saveForFutureUseElement =
        SaveForFutureUseElement(
            initialValue = arguments.saveForFutureUseInitialValue,
            merchantName = arguments.merchantName
        )

    val isSaveForFutureUseCheckedFlow = saveForFutureUseElement.controller.saveForFutureUse
    val isSetAsDefaultPaymentMethodEnabled = metadata.customerMetadata?.isPaymentMethodSetAsDefaultEnabled
        ?: IS_PAYMENT_METHOD_SET_AS_DEFAULT_ENABLED_DEFAULT_VALUE

    add(saveForFutureUseElement)

    if (isSetAsDefaultPaymentMethodEnabled) {
        add(
            SetAsDefaultPaymentMethodElement(
                initialValue = getSetAsDefaultInitialValueFromArguments(arguments),
                saveForFutureUseCheckedFlow = isSaveForFutureUseCheckedFlow,
                setAsDefaultMatchesSaveForFutureUse = arguments.setAsDefaultMatchesSaveForFutureUse,
            )
        )
    }

    return true
}

private fun getSetAsDefaultInitialValueFromArguments(
    arguments: UiDefinitionFactory.Arguments
): Boolean {
    return arguments.initialValues.entries.firstOrNull {
        it.key.v1.contains(IdentifierSpec.SetAsDefaultPaymentMethod.v1)
    }?.value?.toBoolean() ?: false
}
