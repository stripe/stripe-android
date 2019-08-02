package com.stripe.samplestore

import android.os.Parcel
import android.os.Parcelable

/**
 * Represents a single line item for purchase in this store.
 */
class StoreLineItem : Parcelable {

    val description: String
    val quantity: Int
    val unitPrice: Long

    internal val totalPrice: Long
        get() = unitPrice * quantity

    internal constructor(description: String, quantity: Int, unitPrice: Long) {
        this.description = description
        this.quantity = quantity
        this.unitPrice = unitPrice
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(this.description)
        dest.writeInt(this.quantity)
        dest.writeLong(this.unitPrice)
    }

    private constructor(input: Parcel) {
        this.description = input.readString()!!
        this.quantity = input.readInt()
        this.unitPrice = input.readLong()
    }

    companion object CREATOR : Parcelable.Creator<StoreLineItem> {
        override fun createFromParcel(parcel: Parcel): StoreLineItem {
            return StoreLineItem(parcel)
        }

        override fun newArray(size: Int): Array<StoreLineItem?> {
            return arrayOfNulls(size)
        }
    }
}
