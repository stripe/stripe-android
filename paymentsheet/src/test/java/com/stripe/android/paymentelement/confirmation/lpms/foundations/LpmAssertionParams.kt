package com.stripe.android.paymentelement.confirmation.lpms.foundations

import com.stripe.android.elements.AddressDetails
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.state.PaymentElementLoader

internal data class LpmAssertionParams(
    val intent: StripeIntent,
    val createParams: PaymentMethodCreateParams,
    val optionsParams: PaymentMethodOptionsParams? = null,
    val extraParams: PaymentMethodExtraParams? = null,
    val shippingDetails: AddressDetails? = null,
    val customerRequestedSave: Boolean = false,
    val initializationMode: PaymentElementLoader.InitializationMode,
)
