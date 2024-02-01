package com.stripe.android.financialconnections.features.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.v3Colors
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.v3Typography

@Composable
internal fun ExitModal(
    onExit: () -> Unit = { },
    onCancel: () -> Unit = { },
    description: TextResource,
    loading: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
    ) {
        ShapedIcon(
            painter = painterResource(id = R.drawable.stripe_ic_panel_arrow_right),
            contentDescription = stringResource(R.string.stripe_exit_modal_title)
        )
        Spacer(modifier = Modifier.size(16.dp))
        Text(
            text = stringResource(R.string.stripe_exit_modal_title),
            style = v3Typography.headingMedium,
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(text = description.toText().toString())
        Spacer(modifier = Modifier.size(24.dp))
        FinancialConnectionsButton(
            modifier = Modifier.fillMaxWidth(),
            loading = loading,
            onClick = onExit
        ) {
            Text(text = stringResource(id = R.string.stripe_exit_modal_cta_accept))
        }
        Spacer(modifier = Modifier.size(8.dp))
        FinancialConnectionsButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = !loading,
            type = FinancialConnectionsButton.Type.Secondary,
            onClick = onCancel
        ) {
            Text(text = stringResource(id = R.string.stripe_exit_modal_cta_cancel))
        }
    }
}

@Composable
@Preview
fun ExitModalPreview() {
    FinancialConnectionsTheme {
        Surface(color = v3Colors.backgroundSurface) {
            ExitModal(
                description = TextResource.StringId(R.string.stripe_exit_modal_desc, listOf("MerchantName")),
                loading = false
            )
        }
    }
}
