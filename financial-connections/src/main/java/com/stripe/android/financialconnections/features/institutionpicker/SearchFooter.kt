package com.stripe.android.financialconnections.features.institutionpicker

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.features.consent.FinancialConnectionsUrlResolver
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme

@Composable
internal fun SearchFooter(
    onManualEntryClick: () -> Unit,
    manualEntryEnabled: Boolean,
) {
    Column(
        modifier = Modifier
            .background(color = FinancialConnectionsTheme.colors.backgroundContainer)
            .padding(16.dp),
        content = {
            val uriHandler = LocalUriHandler.current
            Text(
                text = "CAN'T FIND YOUR BANK?",
                style = FinancialConnectionsTheme.typography.caption.copy(
                    color = FinancialConnectionsTheme.colors.textSecondary
                )
            )
            SearchFooterRow(
                title = "Double check your spelling and search terms",
                titleColor = FinancialConnectionsTheme.colors.textPrimary,
                subtitle = "You can also search by your bank’s login URL.",
                icon = R.drawable.stripe_ic_check_nocircle,
                iconColor = FinancialConnectionsTheme.colors.textSecondary,
                iconBackgroundColor = FinancialConnectionsTheme.colors.borderDefault
            )
            if (manualEntryEnabled) {
                SearchFooterRow(
                    title = "Manually add your account",
                    titleColor = FinancialConnectionsTheme.colors.textBrand,
                    icon = R.drawable.stripe_ic_edit,
                    iconColor = FinancialConnectionsTheme.colors.iconBrand,
                    iconBackgroundColor = FinancialConnectionsTheme.colors.borderFocus,
                    modifier = Modifier.clickable { onManualEntryClick() }
                )
            }
            SearchFooterRow(
                title = "Questions? Email support@stripe.com",
                titleColor = FinancialConnectionsTheme.colors.textBrand,
                icon = R.drawable.stripe_ic_mail,
                iconColor = FinancialConnectionsTheme.colors.iconBrand,
                iconBackgroundColor = FinancialConnectionsTheme.colors.borderFocus,
                modifier = Modifier.clickable {
                    uriHandler.openUri(FinancialConnectionsUrlResolver.getSupportUrl())
                }
            )
        }
    )
}

@Composable
private fun SearchFooterRow(
    title: String,
    titleColor: Color,
    @DrawableRes icon: Int,
    iconColor: Color,
    iconBackgroundColor: Color,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Image(
            painter = painterResource(id = icon),
            colorFilter = ColorFilter.tint(iconColor),
            contentDescription = null,
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(iconBackgroundColor)
                .padding(8.dp)
        )
        Spacer(modifier = Modifier.size(8.dp))
        Column {
            Text(
                text = title,
                color = titleColor,
                style = FinancialConnectionsTheme.typography.caption
            )
            subtitle?.let {
                Text(
                    text = it,
                    color = FinancialConnectionsTheme.colors.textSecondary,
                    style = FinancialConnectionsTheme.typography.caption,
                )
            }
        }
    }
}

@Composable
@Preview
internal fun SearchFooterTest() {
    FinancialConnectionsTheme {
        SearchFooter(
            onManualEntryClick = {},
            manualEntryEnabled = false
        )
    }
}