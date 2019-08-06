package com.stripe.samplestore

import android.os.Parcel
import android.os.Parcelable

import java.util.ArrayList
import java.util.Currency
import java.util.LinkedHashMap
import java.util.UUID

class StoreCart : Parcelable {

    val currency: Currency
    private val storeLineItems: LinkedHashMap<String, StoreLineItem>

    internal val lineItems: List<StoreLineItem>
        get() = ArrayList(storeLineItems.values)

    internal val totalPrice: Long
        get() {
            var total = 0L
            for (item in storeLineItems.values) {
                total += item.totalPrice
            }
            return total
        }

    internal constructor(currency: Currency) {
        this.currency = currency
        // LinkedHashMap because we want iteration order to be the same.
        storeLineItems = LinkedHashMap()
    }

    fun addStoreLineItem(description: String, quantity: Int, unitPrice: Long) {
        addStoreLineItem(StoreLineItem(description, quantity, unitPrice))
    }

    private fun addStoreLineItem(storeLineItem: StoreLineItem) {
        storeLineItems[UUID.randomUUID().toString()] = storeLineItem
    }

    fun removeLineItem(itemId: String): Boolean {
        return storeLineItems.remove(itemId) != null
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(currency.currencyCode)
        dest.writeInt(storeLineItems.size)
        for (key in storeLineItems.keys) {
            dest.writeString(key)
            dest.writeParcelable(storeLineItems[key], 0)
        }
    }

    private constructor(input: Parcel) {
        currency = Currency.getInstance(input.readString())
        val count = input.readInt()
        storeLineItems = LinkedHashMap()
        for (i in 0 until count) {
            val id = input.readString()!!
            val item = input
                .readParcelable<StoreLineItem>(StoreLineItem::class.java.classLoader)!!
            storeLineItems[id] = item
        }
    }

    companion object CREATOR : Parcelable.Creator<StoreCart> {
        override fun createFromParcel(parcel: Parcel): StoreCart {
            return StoreCart(parcel)
        }

        override fun newArray(size: Int): Array<StoreCart?> {
            return arrayOfNulls(size)
        }
    }
}
