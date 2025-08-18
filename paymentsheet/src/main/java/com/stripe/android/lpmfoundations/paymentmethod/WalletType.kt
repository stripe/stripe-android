package com.stripe.android.lpmfoundations.paymentmethod

import com.stripe.android.model.ElementsSession
import com.stripe.android.paymentsheet.state.LinkState

internal enum class WalletType(val code: String) {
    GooglePay(code = "google_pay"),
    Link(code = "link"),
    ShopPay(code = "shop_pay");

    companion object {
        fun listFrom(
            elementsSession: ElementsSession,
            isGooglePayReady: Boolean,
            linkState: LinkState?,
            isShopPayAvailable: Boolean
        ): List<WalletType> {
            val availableWallets = WalletType.entries.filter { type ->
                val isInOrderedPaymentMethods = elementsSession.orderedPaymentMethodTypesAndWallets.contains(type.code)
                when (type) {
                    GooglePay -> {
                        isGooglePayReady && isInOrderedPaymentMethods
                    }
                    Link -> linkState != null
                    ShopPay -> {
                        isShopPayAvailable && isInOrderedPaymentMethods
                    }
                }
            }

            val walletByIndex = availableWallets.associateWith { wallet ->
                val position = elementsSession.orderedPaymentMethodTypesAndWallets.indexOf(wallet.code)

                if (wallet == Link) {
                    -1
                } else if (position == -1) {
                    null
                } else {
                    position
                }
            }

            return availableWallets.sortedWith(
                compareBy(nullsLast()) {
                    walletByIndex[it]
                }
            )
        }
    }
}
