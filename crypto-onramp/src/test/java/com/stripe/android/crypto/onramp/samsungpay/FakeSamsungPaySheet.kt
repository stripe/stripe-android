package com.stripe.android.crypto.onramp.samsungpay

open class SheetControl

class CustomSheet {
    val controls = mutableListOf<SheetControl>()

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
