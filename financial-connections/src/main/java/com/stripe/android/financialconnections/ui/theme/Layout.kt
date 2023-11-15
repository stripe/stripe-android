package com.stripe.android.financialconnections.ui.theme

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.v3Typography

@Composable
internal fun Layout(
    content: LazyListScope.() -> Unit,
    footer: @Composable () -> Unit = {},
    inModal: Boolean = false,
    lazyListState: LazyListState = rememberLazyListState()
) {
    Column(
        Modifier
            .also { if (inModal.not()) it.fillMaxSize() }
            .padding(
                horizontal = 24.dp,
                vertical = 16.dp
            )
    ) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = inModal.not())
        ) {
            content()
        }
        footer()
    }
}

@Preview(showBackground = true)
@Suppress("MagicNumber")
@Composable
internal fun LayoutPreview() {
    FinancialConnectionsTheme {
        val state = rememberLazyListState()
        Layout(
            lazyListState = state,
            content = {
                item {
                    Text(
                        "Title",
                        style = v3Typography.headingXLarge
                    )
                }
                for (it in 1..50) {
                    item {
                        Text("Body item $it")
                    }
                }
            },
            footer = {
                Column(
                    Modifier.fillMaxWidth()
                ) {
                    FinancialConnectionsButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {}
                    ) {
                        Text("Button 1")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    FinancialConnectionsButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { }
                    ) {
                        Text("Button 1")
                    }
                }
            }
        )
    }
}
