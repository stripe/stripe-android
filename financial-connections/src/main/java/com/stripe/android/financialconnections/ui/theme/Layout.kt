package com.stripe.android.financialconnections.ui.theme

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.presentation.parentViewModel
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsTopAppBar
import com.stripe.android.financialconnections.ui.components.elevation
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.typography

/**
 * A layout that contains a body, and an optional, bottom fixed footer.
 *
 * @param modifier the modifier to apply to the layout.
 * @param body the content of the layout.
 * @param footer the content of the footer.
 * @param inModal whether the layout is being used in a modal or not. If true, the [body] won't expand to fill the
 * available content.
 * @param showFooterShadowWhenScrollable whether to show a shadow at the top of the footer when the body is scrollable.
 * @param scrollState the [ScrollState] to use for the scrollable body.
 */
@Composable
internal fun Layout(
    modifier: Modifier = Modifier,
    bodyPadding: PaddingValues = PaddingValues(horizontal = 24.dp),
    inModal: Boolean = false,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    showFooterShadowWhenScrollable: Boolean = true,
    scrollState: ScrollState = rememberScrollState(),
    footer: (@Composable () -> Unit)? = null,
    body: @Composable ColumnScope.() -> Unit,
) {
    LayoutScaffold(
        canScrollForward = scrollState.canScrollForward,
        canScrollBackward = scrollState.canScrollBackward,
        inModal = inModal,
        showFooterShadowWhenScrollable = showFooterShadowWhenScrollable,
        modifier = modifier,
        footer = footer,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .animateContentSize(),
            verticalArrangement = verticalArrangement,
        ) {
            // Nested columns to achieve proper content padding
            Column(modifier = Modifier.padding(bodyPadding)) {
                body()
            }
        }
    }
}

/**
 * A layout that contains a body, and an optional, bottom fixed footer.
 *
 * @param modifier the modifier to apply to the layout.
 * @param body the content of the layout.
 * @param footer the content of the footer.
 * @param inModal whether the layout is being used in a modal or not. If true, the [body] won't expand to fill the
 * available content.
 * @param showFooterShadowWhenScrollable whether to show a shadow at the top of the footer when the body is scrollable.
 * @param lazyListState the [LazyListState] to use for the scrollable body.
 */
@Composable
internal fun LazyLayout(
    modifier: Modifier = Modifier,
    body: LazyListScope.() -> Unit,
    footer: (@Composable () -> Unit)? = null,
    bodyPadding: PaddingValues = PaddingValues(horizontal = 24.dp),
    inModal: Boolean = false,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    showFooterShadowWhenScrollable: Boolean = true,
    lazyListState: LazyListState = rememberLazyListState()
) {
    LayoutScaffold(
        canScrollForward = lazyListState.canScrollForward,
        canScrollBackward = lazyListState.canScrollBackward,
        inModal = inModal,
        showFooterShadowWhenScrollable = showFooterShadowWhenScrollable,
        modifier = modifier,
        footer = footer,
    ) {
        LazyColumn(
            state = lazyListState,
            verticalArrangement = verticalArrangement,
            contentPadding = bodyPadding,
        ) {
            body()
        }
    }
}

@Composable
private fun LayoutScaffold(
    canScrollBackward: Boolean,
    canScrollForward: Boolean,
    inModal: Boolean,
    showFooterShadowWhenScrollable: Boolean,
    modifier: Modifier = Modifier,
    footer: (@Composable () -> Unit)?,
    body: @Composable () -> Unit,
) {
    val parent = parentViewModel()

    LaunchedEffect(canScrollBackward) {
        parent.updateTopAppBarElevation(isElevated = canScrollBackward)
    }

    Column(
        modifier
            .also { if (inModal.not()) it.fillMaxSize() }
    ) {
        // Box to contain the layout body and an optional footer shadow drawn on top.
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f, fill = inModal.not())
        ) {
            // Footer shadow (top aligned)
            if (showFooterShadowWhenScrollable && canScrollForward) {
                FooterTopShadow()
            }
            // Body content
            body()
        }
        // Footer content (bottom aligned)
        footer?.let {
            Box(
                modifier = Modifier.padding(
                    top = 16.dp,
                    bottom = 24.dp,
                    start = 24.dp,
                    end = 24.dp,
                ),
                content = { it() }
            )
        }
    }
}

@Composable
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
@Composable
internal fun LayoutPreview() {
    FinancialConnectionsTheme {
        val state = rememberLazyListState()
        FinancialConnectionsScaffold(
            topBar = {
                FinancialConnectionsTopAppBar(
                    hideStripeLogo = false,
                    elevation = state.elevation,
                    onCloseClick = {}
                )
            },
            content = {
                LazyLayout(
                    lazyListState = state,
                    body = {
                        item {
                            Text(
                                "Title",
                                style = typography.headingXLarge
                            )
                        }
                        for (index in 1..50) {
                            item {
                                Text("Body item $index")
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
        )
    }
}
