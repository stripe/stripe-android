package com.stripe.android.link.ui.wallet

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.ui.menu.LinkMenuItem

internal enum class AddPaymentMethodOption(override val text: ResolvableString) : LinkMenuItem {
    Bank(resolvableString("Bank")),
    Card(resolvableString("Credit or debit card")),
    ;

    override val testTag: String get() = name
    override val isDestructive: Boolean get() = false
}
