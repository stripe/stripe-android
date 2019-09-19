package com.stripe.android.model.wallets

import android.os.Parcel
import android.os.Parcelable

class SamsungPayWallet : Wallet {
    private constructor(builder: Builder) : super(Type.SamsungPay, builder)

    private constructor(parcel: Parcel) : super(parcel)

    internal class Builder : Wallet.Builder<SamsungPayWallet>() {
        override fun build(): SamsungPayWallet {
            return SamsungPayWallet(this)
        }
    }

    companion object {
        internal fun fromJson(): Builder {
            return Builder()
        }

        @JvmField
        val CREATOR: Parcelable.Creator<SamsungPayWallet> =
            object : Parcelable.Creator<SamsungPayWallet> {
                override fun createFromParcel(parcel: Parcel): SamsungPayWallet {
                    return SamsungPayWallet(parcel)
                }

                override fun newArray(size: Int): Array<SamsungPayWallet?> {
                    return arrayOfNulls(size)
                }
            }
    }
}
