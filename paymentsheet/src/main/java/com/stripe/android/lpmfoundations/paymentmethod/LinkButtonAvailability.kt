package com.stripe.android.lpmfoundations.paymentmethod

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.state.LinkState

internal sealed interface LinkButtonAvailability {
    data object Available : LinkButtonAvailability

    data class Unavailable(val reasons: List<LinkButtonUnavailableReason>) : LinkButtonAvailability
}

internal enum class LinkButtonUnavailableReason(val errorMessage: String) {
    LinkConfiguration("LinkConfiguration.display is set to Never."),
    WalletButtonHidden(
        "LinkConfiguration.display is set to WalletButtonHidden and no existing Link user was found."
    ),
    ShippingAddressRequired(
        "The Link button in Express Checkout Element does not support collecting a shipping address."
    ),
}

internal fun PaymentMethodMetadata.linkButtonAvailability(
    requiresShippingAddress: Boolean,
): LinkButtonAvailability {
    val state = linkState
    val reasons = buildList {
        if (state != null) {
            when (linkConfiguration.display) {
                PaymentSheet.LinkConfiguration.Display.Automatic -> Unit
                PaymentSheet.LinkConfiguration.Display.Never -> add(LinkButtonUnavailableReason.LinkConfiguration)
                PaymentSheet.LinkConfiguration.Display.WalletButtonHidden -> {
                    if (state.loginState == LinkState.LoginState.LoggedOut) {
                        add(LinkButtonUnavailableReason.WalletButtonHidden)
                    }
                }
            }
        }
        if (requiresShippingAddress) {
            add(LinkButtonUnavailableReason.ShippingAddressRequired)
        }
    }
    return if (state != null && reasons.isEmpty()) {
        LinkButtonAvailability.Available
    } else {
        LinkButtonAvailability.Unavailable(reasons)
    }
}
