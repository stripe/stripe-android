package com.stripe.android.financialconnections.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.v3Typography

@Composable
internal fun Layout(
    content: LazyListScope.() -> Unit,
    footer: @Composable () -> Unit = {},
    inModal: Boolean = false,
    showFooterShadowWhenScrollable: Boolean = true,
    lazyListState: LazyListState = rememberLazyListState()
) {
    Column(
        Modifier
            .also { if (inModal.not()) it.fillMaxSize() }
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f, fill = inModal.not())
        ) {
            if (showFooterShadowWhenScrollable && lazyListState.canScrollForward) {
                FooterTopShadow()
            }
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                content()
            }
        }
        Box(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 24.dp),
            content = { footer() }
        )
    }
}

@Composable
@Suppress("MagicNumber")
private fun BoxScope.FooterTopShadow() {
    val shadowSize = 4
    Box(
        modifier = Modifier.Companion
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .height(shadowSize.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.LightGray.copy(alpha = 0.2f)
                    ),
                    0.0f,
                    shadowSize.toFloat()
                )
            )
    )
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
