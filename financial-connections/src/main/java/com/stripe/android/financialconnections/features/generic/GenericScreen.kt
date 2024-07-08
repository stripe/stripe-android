package com.stripe.android.financialconnections.features.generic

import Alignment
import FinancialConnectionsGenericInfoScreen
import FinancialConnectionsGenericInfoScreen.Body
import FinancialConnectionsGenericInfoScreen.Footer
import FinancialConnectionsGenericInfoScreen.Header
import Size
import VerticalAlignment
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.LocalImageLoader
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.components.AnnotatedText
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.sdui.fromHtml
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.colors
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.typography
import com.stripe.android.financialconnections.ui.theme.Layout
import com.stripe.android.uicore.image.StripeImage

@Preview
@Composable
internal fun GenericScreenPreview(
    @PreviewParameter(GenericScreenPreviewParameterProvider::class) state: GenericScreenState
) {
    FinancialConnectionsPreview {
        Surface(color = colors.backgroundSurface) {
            GenericScreen(
                state = state,
                onClickableTextClick = {},
                onPrimaryButtonClick = {},
            )
        }
    }
}

@Composable
internal fun GenericScreen(
    state: GenericScreenState,
    onPrimaryButtonClick: () -> Unit,
    onClickableTextClick: (String) -> Unit,
) {
    val density = LocalDensity.current

    var containerHeight by remember { mutableStateOf(0.dp) }
    var footerHeight by remember { mutableStateOf(0.dp) }
    var contentHeight by remember { mutableStateOf(0.dp) }

    val spacing by remember {
        derivedStateOf {
            ((containerHeight - footerHeight - contentHeight) / 2).takeIf {
                state.screen.options?.verticalAlignment == VerticalAlignment.Centered && it > 0.dp
            }
        }
    }

    Layout(
        bodyPadding = PaddingValues(vertical = 24.dp),
        inModal = state.inModal,
        footer = state.screen.footer?.let {
            {
                GenericFooter(
                    payload = it,
                    onPrimaryButtonClick = onPrimaryButtonClick,
                    onClickableTextClick = onClickableTextClick,
                    modifier = Modifier.onGloballyPositioned {
                        with(density) {
                            footerHeight = it.size.height.toDp()
                        }
                    },
                )
            }
        },
        modifier = Modifier.onGloballyPositioned {
            with(density) {
                containerHeight = it.size.height.toDp()
            }
        }
    ) {
        spacing?.let {
            Spacer(modifier = Modifier.height(it))
        }

        Column(
            modifier = Modifier.onGloballyPositioned {
                with(density) {
                    contentHeight = it.size.height.toDp()
                }
            }
        ) {
            state.screen.header?.let {
                GenericHeader(
                    payload = it,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            state.screen.body?.let {
                GenericBody(
                    payload = it,
                    onClickableTextClick = onClickableTextClick,
                )
            }
        }

        spacing?.let {
            Spacer(modifier = Modifier.height(it))
        }
    }
}

@Composable
internal fun GenericBody(
    payload: Body,
    modifier: Modifier = Modifier,
    onClickableTextClick: (String) -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        payload.entries.forEachIndexed { index, entry: Body.Entry ->
            Box {
                when (entry) {
                    is Body.Entry.Image -> {
                        StripeImage(
                            url = entry.image.default.orEmpty(),
                            contentDescription = entry.alt,
                            imageLoader = LocalImageLoader.current,
                            errorContent = { },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    is Body.Entry.Text -> {
                        val font = when (entry.size) {
                            Size.XSmall -> typography.labelSmall
                            Size.Small -> typography.bodySmall
                            Size.Medium -> typography.bodyMedium
                            null -> typography.bodyMedium
                        }

                        AnnotatedText(
                            text = TextResource.Text(fromHtml(entry.text)),
                            onClickableTextClick = onClickableTextClick,
                            defaultStyle = font.copy(
                                textAlign = when (entry.alignment) {
                                    Alignment.Left -> TextAlign.Start
                                    Alignment.Center -> TextAlign.Center
                                    Alignment.Right -> TextAlign.End
                                    null -> TextAlign.Start
                                }
                            ),
                            modifier = Modifier.padding(horizontal = 24.dp),
                        )
                    }
                    else -> {
                        Log.e("GenericBody", "Unsupported entry type: $entry")
                    }
                }
            }

            if (index != payload.entries.lastIndex) {
                Spacer(modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
internal fun GenericHeader(
    payload: Header,
    modifier: Modifier = Modifier,
) {
    val contentAlignment = when (payload.alignment) {
        Alignment.Left -> TextAlign.Start
        Alignment.Center -> TextAlign.Center
        Alignment.Right -> TextAlign.End
        null -> TextAlign.Start
    }

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        // TODO align header based on textAlign
        // const isBrandIcon = content.header?.icon?.default.indexOf('BrandIcon') !== -1;
        // size={content.header.alignment === 'center' ? 'large' : 'medium'}
        payload.icon?.default?.let { iconUrl ->
            StripeImage(
                url = iconUrl,
                contentDescription = null,
                imageLoader = LocalImageLoader.current,
                errorContent = { },
                loadingContent = {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(Color.LightGray, RoundedCornerShape(16.dp))
                    )
                },
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(16.dp)),
            )

            Spacer(modifier = Modifier.size(32.dp))
        }

        if (payload.title != null) {
            AnnotatedText(
                text = TextResource.Text(payload.title),
                onClickableTextClick = {
                    Log.d("TILL123", it)
                },
                defaultStyle = typography.headingXLarge.copy(
                    textAlign = contentAlignment,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (payload.subtitle != null) {
            Spacer(modifier = Modifier.height(16.dp))

            AnnotatedText(
                text = TextResource.Text(fromHtml(payload.subtitle)),
                onClickableTextClick = { },
                defaultStyle = typography.bodyMedium.copy(
                    textAlign = contentAlignment,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
internal fun GenericFooter(
    payload: Footer,
    modifier: Modifier = Modifier,
    onPrimaryButtonClick: () -> Unit,
    onClickableTextClick: (String) -> Unit,
) {
    Column(modifier) {
        payload.disclaimer?.let { disclaimer ->
            AnnotatedText(
                modifier = Modifier.fillMaxWidth(),
                text = TextResource.Text(fromHtml(disclaimer)),
                onClickableTextClick = onClickableTextClick,
                defaultStyle = typography.labelSmall.copy(
                    color = colors.textDefault,
                    textAlign = TextAlign.Start,
                ),
            )

            Spacer(modifier = Modifier.size(16.dp))
        }

        payload.primaryCta?.let { primaryCta ->
            FinancialConnectionsButton(
                loading = false,
                type = FinancialConnectionsButton.Type.Primary,
                onClick = onPrimaryButtonClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = primaryCta.label)

                payload.primaryCta?.icon?.default?.let {
                    Spacer(modifier = Modifier.size(12.dp))
                    StripeImage(
                        url = it,
                        contentDescription = null,
                        imageLoader = LocalImageLoader.current,
                        errorContent = { },
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        payload.secondaryCta?.let { secondaryCta ->
            Spacer(modifier = Modifier.size(8.dp))

            FinancialConnectionsButton(
                loading = false,
                enabled = true,
                type = FinancialConnectionsButton.Type.Secondary,
                onClick = {},
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = secondaryCta.label)
            }
        }

        payload.belowCta?.let { belowCta ->
            Spacer(modifier = Modifier.size(12.dp))

            AnnotatedText(
                modifier = Modifier.fillMaxWidth(),
                text = TextResource.Text(fromHtml(belowCta.label)),
                onClickableTextClick = onClickableTextClick,
                defaultStyle = typography.labelSmall.copy(
                    color = colors.textDefault,
                    textAlign = TextAlign.Start,
                ),
            )
        }
    }
}

internal data class GenericScreenState(
    val screen: FinancialConnectionsGenericInfoScreen,
    val inModal: Boolean
)
