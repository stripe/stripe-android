package com.stripe.android.customersheet.data

import com.stripe.android.customersheet.CustomerPermissions
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.model.SavedSelection

internal data class CustomerSheetSession(
    val elementsSession: ElementsSession,
    val paymentMethods: List<PaymentMethod>,
    val savedSelection: SavedSelection?,
    val paymentMethodSaveConsentBehavior: PaymentMethodSaveConsentBehavior,
    val permissions: CustomerPermissions,
    val defaultPaymentMethodId: String?,
)
