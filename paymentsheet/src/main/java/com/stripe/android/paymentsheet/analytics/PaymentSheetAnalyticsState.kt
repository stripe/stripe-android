package com.stripe.android.paymentsheet.analytics

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.model.Address
import com.stripe.android.paymentsheet.addresselement.computeBillingEditDistance
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.billingDetails

private const val PREVIOUSLY_SENT_DEEP_LINK_EVENT = "previously_sent_deep_link_event"
private const val AUTOCOMPLETE_USED_KEY = "BILLING_AUTOCOMPLETE_USED"
private const val AUTOCOMPLETE_EDIT_DISTANCE_KEY = "BILLING_AUTOCOMPLETE_EDIT_DISTANCE"

internal var SavedStateHandle.previouslySentDeepLinkEvent: Boolean
    get() = this[PREVIOUSLY_SENT_DEEP_LINK_EVENT] ?: false
    set(value) {
        this[PREVIOUSLY_SENT_DEEP_LINK_EVENT] = value
    }

internal fun SavedStateHandle.persistBillingAnalytics(
    paymentSelection: PaymentSelection?,
    autocompleteFilledAddress: Address?,
) {
    if (paymentSelection !is PaymentSelection.New) return
    val billingAddress = paymentSelection.billingDetails?.address ?: return

    this[AUTOCOMPLETE_USED_KEY] = autocompleteFilledAddress != null
    this[AUTOCOMPLETE_EDIT_DISTANCE_KEY] = autocompleteFilledAddress?.let {
        computeBillingEditDistance(it, billingAddress)
    }
}

internal fun SavedStateHandle.reportBillingAddressCompleted(
    paymentSelection: PaymentSelection,
    eventReporter: EventReporter,
) {
    if (paymentSelection !is PaymentSelection.New) return
    val countryCode = paymentSelection.billingDetails?.address?.country ?: return
    val autocompleteUsed = get<Boolean>(AUTOCOMPLETE_USED_KEY) == true
    val editDistance = get<Int>(AUTOCOMPLETE_EDIT_DISTANCE_KEY)

    remove<Boolean>(AUTOCOMPLETE_USED_KEY)
    remove<Int>(AUTOCOMPLETE_EDIT_DISTANCE_KEY)

    eventReporter.onBillingAddressCompleted(
        addressCountryCode = countryCode,
        autocompleteResultSelected = autocompleteUsed,
        editDistance = editDistance,
    )
}
