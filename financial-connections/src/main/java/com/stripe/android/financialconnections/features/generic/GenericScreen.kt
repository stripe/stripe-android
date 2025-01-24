package com.stripe.android.financialconnections.features.generic

import Alignment
import FinancialConnectionsGenericInfoScreen
import FinancialConnectionsGenericInfoScreen.Body
import FinancialConnectionsGenericInfoScreen.Footer
import FinancialConnectionsGenericInfoScreen.Header
import Size
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.features.common.IconSize
import com.stripe.android.financialconnections.features.common.ShapedIcon
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
import androidx.compose.ui.Alignment as ComposeAlignment

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
                onSecondaryButtonClick = {},
                onPrimaryButtonClick = {},
            )
        }
    }
}

@Composable
internal fun GenericScreen(
    state: GenericScreenState,
    onPrimaryButtonClick: () -> Unit,
    onSecondaryButtonClick: () -> Unit,
    onClickableTextClick: (String) -> Unit,
) {
    Layout(
        bodyPadding = PaddingValues(vertical = 16.dp),
        inModal = state.inModal,
        footer = state.screen.footer?.let {
            {
                GenericFooter(
                    payload = it,
                    onPrimaryButtonClick = onPrimaryButtonClick,
                    onSecondaryButtonClick = onSecondaryButtonClick,
                    onClickableTextClick = onClickableTextClick,
                )
            }
        },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            state.screen.header?.let {
                GenericHeader(
                    payload = it,
                    onClickableTextClick = onClickableTextClick,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }

            state.screen.body?.let {
                GenericBody(
                    payload = it,
                    onClickableTextClick = onClickableTextClick,
                )
            }
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
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        payload.entries.forEach { entry: Body.Entry ->
            Box {
                when (entry) {
                    is Body.Entry.Image -> {
                        StripeImage(
                            url = entry.image.default.orEmpty(),
                            contentDescription = entry.alt,
                            imageLoader = LocalImageLoader.current,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    is Body.Entry.Text -> {
                        val font = entry.size.toComposeSize()

                        AnnotatedText(
                            text = TextResource.Text(fromHtml(entry.text)),
                            onClickableTextClick = onClickableTextClick,
                            defaultStyle = font.copy(
                                textAlign = entry.alignment.toComposeTextAlign()
                            ),
                            modifier = Modifier.padding(horizontal = 24.dp),
                        )
                    }
                    else -> {
                        Log.e("GenericBody", "Unsupported entry type: $entry")
                    }
                }
            }
        }
    }
}

@Composable
internal fun GenericHeader(
    payload: Header,
    onClickableTextClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isBrandIcon: Boolean =
        remember(payload.icon?.default) { payload.icon?.default?.contains("BrandIcon") == true }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        payload.icon?.default?.let { iconUrl ->
            ShapedIcon(
                modifier = Modifier.align(payload.alignment.toComposeAlignment()),
                backgroundShape = if (isBrandIcon) RoundedCornerShape(12.dp) else CircleShape,
                flushed = isBrandIcon,
                url = iconUrl,
                contentDescription = null,
                iconSize = if (payload.alignment == Alignment.Center) IconSize.Large else IconSize.Medium,
            )
        }

        if (payload.title != null) {
            AnnotatedText(
                text = TextResource.Text(payload.title),
                onClickableTextClick = onClickableTextClick,
                defaultStyle = typography.headingXLarge.copy(
                    textAlign = payload.alignment.toComposeTextAlign(),
                    color = colors.textDefault,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (payload.subtitle != null) {
            AnnotatedText(
                text = TextResource.Text(fromHtml(payload.subtitle)),
                onClickableTextClick = onClickableTextClick,
                defaultStyle = typography.bodyMedium.copy(
                    textAlign = payload.alignment.toComposeTextAlign(),
                    color = colors.textDefault,
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
    onSecondaryButtonClick: () -> Unit,
    onClickableTextClick: (String) -> Unit,
) {
    Column(
        modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
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
        }

        payload.primaryCta?.let { action ->
            GenericButton(
                type = FinancialConnectionsButton.Type.Primary,
                onClick = onPrimaryButtonClick,
                action = action,
            )
        }

        payload.secondaryCta?.let { secondaryCta ->
            GenericButton(
                type = FinancialConnectionsButton.Type.Secondary,
                onClick = onSecondaryButtonClick,
                action = secondaryCta,
            )
        }

        payload.belowCta?.let { belowCta ->
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

@Composable
private fun GenericButton(
    onClick: () -> Unit,
    type: FinancialConnectionsButton.Type,
    action: Footer.GenericInfoAction,
) {
    FinancialConnectionsButton(
        loading = false,
        type = type,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text = action.label)

        action.icon?.default?.let {
            Spacer(modifier = Modifier.size(12.dp))
            StripeImage(
                url = it,
                contentDescription = null,
                imageLoader = LocalImageLoader.current,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

private fun Alignment?.toComposeTextAlign(): TextAlign = when (this) {
    Alignment.Left -> TextAlign.Start
    Alignment.Center -> TextAlign.Center
    Alignment.Right -> TextAlign.End
    null -> TextAlign.Start
}

private fun Alignment?.toComposeAlignment() = when (this) {
    Alignment.Center -> ComposeAlignment.CenterHorizontally
    Alignment.Left -> ComposeAlignment.Start
    Alignment.Right -> ComposeAlignment.End
    null -> ComposeAlignment.Start
}

@Composable
private fun Size?.toComposeSize() = when (this) {
    Size.XSmall -> typography.labelSmall
    Size.Small -> typography.bodySmall
    Size.Medium -> typography.bodyMedium
    null -> typography.bodyMedium
}

internal data class GenericScreenState(
    val screen: FinancialConnectionsGenericInfoScreen,
    val inModal: Boolean
)
