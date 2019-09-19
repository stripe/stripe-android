package com.stripe.android.model.wallets

import android.os.Parcel
import android.os.Parcelable

class AmexExpressCheckoutWallet : Wallet {
    private constructor(builder: Builder) : super(Type.AmexExpressCheckout, builder)

    private constructor(parcel: Parcel) : super(parcel)

    internal class Builder : Wallet.Builder<AmexExpressCheckoutWallet>() {
        override fun build(): AmexExpressCheckoutWallet {
            return AmexExpressCheckoutWallet(this)
        }
    }

    companion object {
        internal fun fromJson(): Builder {
            return Builder()
        }

        @JvmField
        val CREATOR: Parcelable.Creator<AmexExpressCheckoutWallet> =
            object : Parcelable.Creator<AmexExpressCheckoutWallet> {
                override fun createFromParcel(parcel: Parcel): AmexExpressCheckoutWallet {
                    return AmexExpressCheckoutWallet(parcel)
                }

                override fun newArray(size: Int): Array<AmexExpressCheckoutWallet?> {
                    return arrayOfNulls(size)
                }
            }
    }
}
