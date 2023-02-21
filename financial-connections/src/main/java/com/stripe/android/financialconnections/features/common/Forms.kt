package com.stripe.android.financialconnections.features.common

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme

/**
 * Error text to show under form elements.
 */
@Composable
internal fun FormErrorText(error: Throwable) {
    Text(
        modifier = Modifier.padding(horizontal = 4.dp),
        text = error.localizedMessage ?: stringResource(id = R.string.stripe_error_generic_title),
        color = FinancialConnectionsTheme.colors.textCritical,
        style = FinancialConnectionsTheme.typography.caption
    )
}
