package com.stripe.android.financialconnections.ui.theme

import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.v3Typography

@Composable
internal fun Layout(
    content: LazyListScope.() -> Unit,
    footer: @Composable () -> Unit = {},
    inModal: Boolean = false,
    showFooterDividerOnScroll: Boolean = true,
    lazyListState: LazyListState = rememberLazyListState()
) {
    val isScrolled = remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 0
        }
    }
    Column(
        Modifier
            .also { if (inModal.not()) it.fillMaxSize() }
    ) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .weight(1f, fill = inModal.not())
        ) {
            content()
        }
        if (showFooterDividerOnScroll) {
            Divider(
                modifier = Modifier.alpha(if (isScrolled.value) 1f else 0f),
                color = FinancialConnectionsTheme.v3Colors.border
            )
        }
        Box(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 24.dp),
            content = { footer() }
        )
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
