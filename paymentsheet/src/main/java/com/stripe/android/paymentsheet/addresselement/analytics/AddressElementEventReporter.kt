package com.stripe.android.paymentsheet.addresselement.analytics

internal interface AddressElementEventReporter {
    fun onShown(country: String?)

    fun onSaveCompleted(
        country: String?,
        autocompleteResultSelected: Boolean,
        editDistance: Int?,
    )
}

internal class StandaloneAddressElementEventReporter(
    private val addressLauncherEventReporter: AddressLauncherEventReporter,
) : AddressElementEventReporter {
    override fun onShown(country: String?) {
        addressLauncherEventReporter.onShow(country.orEmpty())
    }

    override fun onSaveCompleted(
        country: String?,
        autocompleteResultSelected: Boolean,
        editDistance: Int?,
    ) {
        val savedCountry = country ?: return
        addressLauncherEventReporter.onCompleted(
            country = savedCountry,
            autocompleteResultSelected = autocompleteResultSelected,
            editDistance = editDistance,
        )
    }
}
