package com.stripe.android.link.ui.wallet

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.stripe.android.link.ui.menu.LinkMenu

@Composable
internal fun AddPaymentMethodMenu(
    modifier: Modifier = Modifier,
    options: List<AddPaymentMethodOption>,
    onOptionClick: (AddPaymentMethodOption) -> Unit,
) {
    LinkMenu(
        modifier = modifier,
        items = options,
        onItemPress = onOptionClick,
    )
}
