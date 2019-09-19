package com.stripe.android.model.wallets

import android.os.Parcel
import android.os.Parcelable

class ApplePayWallet : Wallet {
    private constructor(builder: Builder) : super(Type.ApplePay, builder)

    private constructor(parcel: Parcel) : super(parcel)

    internal class Builder : Wallet.Builder<ApplePayWallet>() {
        override fun build(): ApplePayWallet {
            return ApplePayWallet(this)
        }
    }

    companion object {
        internal fun fromJson(): Builder {
            return Builder()
        }

        @JvmField
        val CREATOR: Parcelable.Creator<ApplePayWallet> =
            object : Parcelable.Creator<ApplePayWallet> {
                override fun createFromParcel(parcel: Parcel): ApplePayWallet {
                    return ApplePayWallet(parcel)
                }

                override fun newArray(size: Int): Array<ApplePayWallet?> {
                    return arrayOfNulls(size)
                }
            }
    }
}
