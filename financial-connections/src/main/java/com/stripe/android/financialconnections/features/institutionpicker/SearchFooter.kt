package com.stripe.android.financialconnections.features.institutionpicker

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.financialconnections.ui.theme.Info100

@Composable
internal fun SearchFooter(
    onManualEntryClick: () -> Unit,
    manualEntryEnabled: Boolean
) {
    Column(
        modifier = Modifier
            .background(color = FinancialConnectionsTheme.colors.backgroundContainer)
            .padding(16.dp),
        content = {
            Text(
                text = stringResource(id = R.string.stripe_institutionpicker_footer_title),
                style = FinancialConnectionsTheme.typography.kicker.copy(
                    color = FinancialConnectionsTheme.colors.textSecondary
                )
            )
            Box(modifier = Modifier.size(16.dp))
            SearchFooterRow(
                title = stringResource(id = R.string.stripe_institutionpicker_footer_item_spelling),
                titleColor = FinancialConnectionsTheme.colors.textPrimary,
                icon = R.drawable.stripe_ic_check,
                iconColor = FinancialConnectionsTheme.colors.textSecondary,
                iconBackgroundColor = FinancialConnectionsTheme.colors.borderDefault
            )
            if (manualEntryEnabled) {
                SearchFooterRow(
                    title = stringResource(id = R.string.stripe_institutionpicker_footer_item_manualentry),
                    titleColor = FinancialConnectionsTheme.colors.textBrand,
                    icon = R.drawable.stripe_ic_edit,
                    iconColor = Info100,
                    iconBackgroundColor = FinancialConnectionsTheme.colors.borderFocus,
                    modifier = Modifier.clickable { onManualEntryClick() }
                )
            }
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
    modifier: Modifier = Modifier
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
                style = FinancialConnectionsTheme.typography.captionTightEmphasized
            )
        }
    }
}

@Composable
@Preview
internal fun SearchFooterTest() {
    FinancialConnectionsPreview {
        SearchFooter(
            onManualEntryClick = {},
            manualEntryEnabled = false
        )
    }
}
