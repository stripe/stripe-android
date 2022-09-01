package com.stripe.android.financialconnections.features.institutionpicker

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme

@Composable
internal fun LoadingContent(
    title: String,
    content: String
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
    ) {
        CircularProgressIndicator(
            color = FinancialConnectionsTheme.colors.textBrand,
            modifier = Modifier
                .size(36.dp)
        )
        Spacer(modifier = Modifier.size(16.dp))
        Text(
            text = title,
            style = FinancialConnectionsTheme.typography.subtitle
        )
        Spacer(modifier = Modifier.size(16.dp))
        Text(
            text = content,
            style = FinancialConnectionsTheme.typography.body
        )
    }
}
