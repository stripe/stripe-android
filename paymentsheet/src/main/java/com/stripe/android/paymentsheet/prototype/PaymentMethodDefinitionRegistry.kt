package com.stripe.android.paymentsheet.prototype

import com.stripe.android.paymentsheet.prototype.paymentmethods.AfterpayClearpayPaymentMethodDefinition
import com.stripe.android.paymentsheet.prototype.paymentmethods.CardPaymentMethodDefinition
import com.stripe.android.paymentsheet.prototype.paymentmethods.IdealPaymentMethodDefinition
import com.stripe.android.paymentsheet.prototype.paymentmethods.KlarnaPaymentMethodDefinition
import com.stripe.android.paymentsheet.prototype.paymentmethods.KonbiniPaymentMethodDefinition
import com.stripe.android.paymentsheet.prototype.paymentmethods.PayPalPaymentMethodDefinition
import com.stripe.android.paymentsheet.prototype.paymentmethods.RevolutPayPaymentMethodDefinition
import com.stripe.android.paymentsheet.prototype.paymentmethods.SepaDebitPaymentMethodDefinition
import com.stripe.android.paymentsheet.prototype.paymentmethods.SofortPaymentMethodDefinition
import com.stripe.android.paymentsheet.prototype.paymentmethods.UsBankAccountPaymentMethodDefinition

internal object PaymentMethodDefinitionRegistry {
    val all: Set<PaymentMethodDefinition> = setOf(
        AfterpayClearpayPaymentMethodDefinition,
        KonbiniPaymentMethodDefinition,
        CardPaymentMethodDefinition,
        IdealPaymentMethodDefinition,
        KlarnaPaymentMethodDefinition,
        PayPalPaymentMethodDefinition,
        RevolutPayPaymentMethodDefinition,
        SepaDebitPaymentMethodDefinition,
        SofortPaymentMethodDefinition,
        UsBankAccountPaymentMethodDefinition,
    )
}
