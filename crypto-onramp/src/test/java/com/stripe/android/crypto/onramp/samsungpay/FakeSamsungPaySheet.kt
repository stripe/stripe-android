package com.stripe.android.crypto.onramp.samsungpay

open class SheetControl

class CustomSheet {
    val controls = mutableListOf<SheetControl>()

    fun getSheetControl(id: String): SheetControl? =
        controls.filterIsInstance<AddressControl>().firstOrNull { it.id == id }

    fun addControl(control: SheetControl) {
        controls += control
    }
}

class AmountBoxControl(
    val id: String,
    val currencyCode: String,
) : SheetControl() {
    val items = mutableListOf<Item>()
    var total: Double? = null
    var totalFormat: String? = null

    fun addItem(
        id: String,
        title: String,
        value: Double,
        description: String,
    ) {
        items += Item(id, title, value, description)
    }

    fun setAmountTotal(value: Double, format: String) {
        total = value
        totalFormat = format
    }

    data class Item(
        val id: String,
        val title: String,
        val value: Double,
        val description: String,
    )
}

class AmountConstants private constructor() {
    companion object {
        const val FORMAT_TOTAL_PRICE_ONLY = "_price_only_"
    }
}

class AddressControl(val id: String, val type: SheetItemType) : SheetControl() {
    var address: FakeSamsungContactAddress? = null
    var sheetUpdatedListener: SheetUpdatedListener? = null
}

enum class SheetItemType { BILLING_ADDRESS }

fun interface SheetUpdatedListener {
    fun onResult(controlId: String, sheet: CustomSheet)
}

class FakeSamsungContactAddress(
    val addressee: String? = null,
    val email: String? = null,
    val phoneNumber: String? = null,
    val countryCode: String? = null,
    val addressLine1: String? = null,
    val addressLine2: String? = null,
    val city: String? = null,
    val state: String? = null,
    val postalCode: String? = null,
)
