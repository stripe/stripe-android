package com.stripe.android.paymentsheet.addresselement

/**
 * Screens related to the address element.
 */
internal sealed class AddressElementScreen(
    open val route: String
) {
    object Autocomplete : AddressElementScreen("AutoComplete")
    object InputAddress : AddressElementScreen("InputAddress")
}
