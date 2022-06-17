package com.stripe.android.paymentsheet.addresscollection

internal sealed class AddressCollectionScreen(
    open val route: String
){
    object Autocomplete : AddressCollectionScreen("AutoComplete")
    object InputAddress : AddressCollectionScreen("InputAddress")
}