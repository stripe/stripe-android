package com.stripe.android.paymentsheet.addresselement

/**
 * Screens related to the address element.
 */
internal sealed class AddressElementScreen(
    open val route: String
) {

    object InputAddress : AddressElementScreen("InputAddress")

    class Autocomplete(
        val country: String
    ) : AddressElementScreen(
        "Autocomplete?$countryArg=$country"
    ) {
        companion object {
            const val countryArg = "country"
            const val route = "Autocomplete?$countryArg={$countryArg}"
        }
    }
}
