package com.stripe.android.financialconnections.ui.theme

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.v3Typography
import kotlinx.coroutines.launch

@Composable
internal fun Layout(
    content: LazyListScope.() -> Unit,
    footer: @Composable () -> Unit = {},
    lazyListState: LazyListState = rememberLazyListState()
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(
                horizontal = 24.dp,
                vertical = 16.dp
            )
    ) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            content()
        }
        footer()
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Preview(showBackground = true)
@Composable
internal fun LayoutPreview() {
    FinancialConnectionsTheme {
        val state = rememberLazyListState()
        val scope = rememberCoroutineScope()
        Layout(
            lazyListState = state,
            content = {
                item {
                    Text(
                        "Doesn't stick",
                        style = v3Typography.headingXLarge

                    )
                }
                stickyHeader(key = "sticky") {
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White),
                        value = "", onValueChange = { },

                        )
                }
                (1..100).forEach {
                    item {
                        Text("Item $it")
                    }
                }
            },
            footer = {
                Column(
                    Modifier.fillMaxWidth()
                ) {
                    FinancialConnectionsButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            scope.launch {
                                state.animateScrollToItem(1)
                            }
                        }
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
