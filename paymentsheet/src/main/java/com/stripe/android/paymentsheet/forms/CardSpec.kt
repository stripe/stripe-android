package com.stripe.android.paymentsheet.forms

import com.stripe.android.paymentsheet.elements.FormRequirement
import com.stripe.android.paymentsheet.elements.LayoutSpec
import com.stripe.android.paymentsheet.elements.PaymentMethodFormSpec
import com.stripe.android.paymentsheet.elements.Requirement
import com.stripe.android.paymentsheet.elements.SaveForFutureUseSpec
import com.stripe.android.paymentsheet.elements.SaveMode

internal val card = PaymentMethodFormSpec(
    mutableMapOf(),
    mapOf(
        // TODO(michelleb: Have to list spec twice to support different modes even though all the same form
        FormRequirement(
            SaveMode.PaymentIntentAndSetupFutureUsageNotSet,
            requirements = setOf(Requirement.Customer)
        ) to (LayoutSpec.create(
            SaveForFutureUseSpec(emptyList())
        )),

        FormRequirement(
            SaveMode.PaymentIntentAndSetupFutureUsageNotSet,
            requirements = setOf()
        ) to (LayoutSpec.create()),

        FormRequirement(
            SaveMode.SetupIntentOrPaymentIntentWithFutureUsageSet,
            requirements = emptySet()
        ) to (LayoutSpec.create())
    )
)
