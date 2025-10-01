package com.stripe.android.paymentelement.confirmation.utils

import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.model.updateSetupFutureUsageWithPmoSfu
import com.stripe.android.paymentelement.PaymentMethodOptionsSetupFutureUsagePreview
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentsheet.PaymentSheet

internal fun PaymentMethodConfirmationOption.New.updatedForDeferredIntent(
    intentConfiguration: PaymentSheet.IntentConfiguration,
): PaymentMethodConfirmationOption.New {
    val updatedCreateParams = createParams.updatedWithProductUsage(intentConfiguration)
    val updatedOptionsParams = optionsParams.updatedWithPmoSfu(
        code = updatedCreateParams.typeCode,
        intentConfiguration = intentConfiguration,
    )
    return copy(
        createParams = updatedCreateParams,
        optionsParams = updatedOptionsParams,
    )
}

internal fun PaymentMethodConfirmationOption.Saved.updatedForDeferredIntent(
    intentConfiguration: PaymentSheet.IntentConfiguration,
): PaymentMethodConfirmationOption.Saved {
    val updatedOptionsParams = optionsParams.updatedWithPmoSfu(
        code = paymentMethod.type?.code,
        intentConfiguration = intentConfiguration,
    )
    return copy(
        optionsParams = updatedOptionsParams,
    )
}

internal fun PaymentMethodCreateParams.updatedWithProductUsage(
    intentConfiguration: PaymentSheet.IntentConfiguration,
): PaymentMethodCreateParams {
    val productUsage = buildSet {
        addAll(attribution)
        add("deferred-intent")
        if (intentConfiguration.paymentMethodTypes.isEmpty()) {
            add("autopm")
        }
    }

    return copy(
        productUsage = productUsage,
    )
}

/**
 * [PaymentSheet.IntentConfiguration.Mode.Payment.PaymentMethodOptions] does not require setting PMO SFU on the
 * intent. If PMO SFU value exists in the configuration, set it in the PaymentMethodOptionsParams.
 */
@OptIn(PaymentMethodOptionsSetupFutureUsagePreview::class)
internal fun PaymentMethodOptionsParams?.updatedWithPmoSfu(
    code: PaymentMethodCode?,
    intentConfiguration: PaymentSheet.IntentConfiguration,
): PaymentMethodOptionsParams? {
    val paymentMethodType = PaymentMethod.Type.fromCode(code) ?: return this
    return (intentConfiguration.mode as? PaymentSheet.IntentConfiguration.Mode.Payment)
        ?.paymentMethodOptions
        ?.setupFutureUsageValues?.let { values ->
            values[paymentMethodType]?.toConfirmParamsSetupFutureUsage()?.let { configPmoSfu ->
                if (this != null) {
                    updateSetupFutureUsageWithPmoSfu(configPmoSfu)
                } else {
                    PaymentMethodOptionsParams.SetupFutureUsage(
                        paymentMethodType = paymentMethodType,
                        setupFutureUsage = configPmoSfu
                    )
                }
            }
        } ?: this
}

internal fun PaymentSheet.IntentConfiguration.SetupFutureUse.toConfirmParamsSetupFutureUsage():
    ConfirmPaymentIntentParams.SetupFutureUsage {
    return when (this) {
        PaymentSheet.IntentConfiguration.SetupFutureUse.OffSession -> {
            ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
        }
        PaymentSheet.IntentConfiguration.SetupFutureUse.OnSession -> {
            ConfirmPaymentIntentParams.SetupFutureUsage.OnSession
        }
        PaymentSheet.IntentConfiguration.SetupFutureUse.None -> {
            ConfirmPaymentIntentParams.SetupFutureUsage.None
        }
    }
}
