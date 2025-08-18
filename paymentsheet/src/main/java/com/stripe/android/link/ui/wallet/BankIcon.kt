package com.stripe.android.link.ui.wallet

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.stripe.android.link.theme.LinkTheme
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.paymentdatacollection.ach.transformBankIconCodeToBankIcon

/**
 * Composable that renders a bank icon with appropriate styling.
 * Handles both specific bank icons and generic bank icon with proper styling.
 *
 * @param bankIconCode The bank icon code from the API, or null for generic icon
 * @param modifier The modifier to be applied to the component
 */
@Composable
internal fun BankIcon(
    bankIconCode: String?,
    modifier: Modifier = Modifier
) {
    val icon = remember(bankIconCode) {
        transformBankIconCodeToBankIcon(
            iconCode = bankIconCode,
            fallbackIcon = R.drawable.stripe_link_bank_outlined,
        )
    }

    val isGenericIcon = icon == R.drawable.stripe_link_bank_outlined

    val containerModifier = if (isGenericIcon) {
        modifier
            .background(
                color = LinkTheme.colors.surfaceTertiary,
                shape = RoundedCornerShape(3.dp),
            )
            .padding(4.dp)
    } else {
        modifier
    }

    Box(modifier = containerModifier) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
            colorFilter = if (isGenericIcon) {
                ColorFilter.tint(LinkTheme.colors.textTertiary)
            } else {
                null
            },
        )
    }
}
