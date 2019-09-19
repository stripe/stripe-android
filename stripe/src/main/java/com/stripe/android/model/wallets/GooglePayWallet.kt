package com.stripe.android.model.wallets

import android.os.Parcel
import android.os.Parcelable

class GooglePayWallet : Wallet {
    private constructor(builder: Builder) : super(Type.GooglePay, builder)

    private constructor(parcel: Parcel) : super(parcel)

    internal class Builder : Wallet.Builder<GooglePayWallet>() {
        override fun build(): GooglePayWallet {
            return GooglePayWallet(this)
        }
    }

    companion object {
        internal fun fromJson(): Builder {
            return Builder()
        }

        @JvmField
        val CREATOR: Parcelable.Creator<GooglePayWallet> =
            object : Parcelable.Creator<GooglePayWallet> {
                override fun createFromParcel(parcel: Parcel): GooglePayWallet {
                    return GooglePayWallet(parcel)
                }

                override fun newArray(size: Int): Array<GooglePayWallet?> {
                    return arrayOfNulls(size)
                }
            }
    }
}
