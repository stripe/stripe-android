package com.stripe.android.financialconnections.features.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.theme.Brand50
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.v3Colors

@Composable
fun ExitModal(
    onExit: () -> Unit = { },
    onCancel: () -> Unit = { },
    loading: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .background(color = Brand50, shape = CircleShape)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.stripe_ic_checkbox_yes),
                contentDescription = "Web Icon",
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = "Are you sure you want to exit?",
            style = FinancialConnectionsTheme.v3Typography.headingMedium,
        )
        Text(
            text = "Your bank account won’t be connected to [Merchant] and all progress will be lost.",
        )
        FinancialConnectionsButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { }
        ) {
            Text(text = "Exit")
        }
        FinancialConnectionsButton(
            modifier = Modifier.fillMaxWidth(),
            type = FinancialConnectionsButton.Type.Secondary,
            onClick = { }
        ) {
            Text(text = "Cancel")
        }
    }

}

@Composable
@Preview
fun ExitModalPreview() {
    FinancialConnectionsTheme {
        Surface(color = v3Colors.backgroundSurface) {
            ExitModal(loading = false)
        }
    }
}