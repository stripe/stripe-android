package com.stripe.android.link.ui.oauth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.LinkTheme
import com.stripe.android.link.ui.ScrollableTopLevelColumn
import com.stripe.android.link.ui.image.LocalStripeImageLoader
import com.stripe.android.uicore.image.StripeImage
import com.stripe.android.uicore.image.StripeImageLoader

internal data class ConsentPane(
    val title: String,
    val userSection: UserSection,
    val scopesSection: ScopesSection,
    val disclaimer: String?,
    val denyButtonLabel: String?,
    val allowButtonLabel: String,
) {
    data class UserSection(
        val iconUrl: String,
        val label: String,
    )

    data class ScopesSection(
        val header: String,
        val scopes: List<Scope>,
    ) {
        data class Scope(
            val iconUrl: String,
            val header: String?,
            val description: String,
        )
    }
}

@Composable
internal fun OAuthConsentScreen(
    merchantLogoUrl: String?,
    pane: ConsentPane,
) {
    ScrollableTopLevelColumn {
        MerchantLogo(merchantLogoUrl)
        Title(pane.title)
        UserSection(pane.userSection)
        ScopesSection(pane.scopesSection)

        if (pane.disclaimer != null) {
            Spacer(Modifier.height(24.dp))
            Disclaimer(pane.disclaimer)
        }
        Spacer(Modifier.height(16.dp))
        ActionButtons(
            denyButtonLabel = pane.denyButtonLabel,
            allowButtonLabel = pane.allowButtonLabel,
            onAllow = {},
            onDeny = {},
        )
    }
}

@Composable
private fun MerchantLogo(
    merchantLogoUrl: String?,
) {
    val imageLoader = LocalStripeImageLoader.current
    val modifier = Modifier
        .clip(RoundedCornerShape(16.dp))
        .size(56.dp)
    if (merchantLogoUrl == null) {
        Box(modifier.background(Color.Red)) // TODO.
    } else {
        StripeImage(
            modifier = modifier,
            url = merchantLogoUrl,
            debugPainter = ColorPainter(LinkTheme.colors.iconBrand),
            imageLoader = imageLoader,
            contentDescription = null,
        )
    }
}

@Composable
private fun Title(title: String) {
    Text(
        modifier = Modifier.padding(
            vertical = 16.dp,
            horizontal = 32.dp,
        ),
        text = title,
        style = LinkTheme.typography.title,
        color = LinkTheme.colors.textPrimary,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun UserSection(section: ConsentPane.UserSection) {
    val imageLoader = LocalStripeImageLoader.current
    Row(
        modifier = Modifier
            .border(1.dp, LinkTheme.colors.textTertiary, CircleShape)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StripeImage(
            modifier = Modifier
                .clip(CircleShape)
                .size(24.dp),
            url = section.iconUrl,
            debugPainter = ColorPainter(LinkTheme.colors.iconBrand),
            imageLoader = imageLoader,
            contentDescription = null,
        )
        Text(
            modifier = Modifier.padding(horizontal = 8.dp),
            text = section.label,
            style = LinkTheme.typography.detail,
            color = LinkTheme.colors.textPrimary,
        )
    }
}

@Composable
private fun ScopesSection(section: ConsentPane.ScopesSection) {
    val imageLoader = LocalStripeImageLoader.current
    Text(
        modifier = Modifier
            .padding(top = 16.dp)
            .fillMaxWidth(),
        text = section.header,
        style = LinkTheme.typography.detail,
        color = LinkTheme.colors.textPrimary,
    )
    Spacer(Modifier.height(16.dp))
    Column(
        modifier = Modifier
            .border(1.dp, LinkTheme.colors.textTertiary, RoundedCornerShape(12.dp))
            .padding(16.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        section.scopes.forEach { scope ->
            ScopeItem(
                imageLoader = imageLoader,
                iconUrl = scope.iconUrl,
                header = scope.header,
                description = scope.description,
            )
        }
    }
}

@Composable
private fun ScopeItem(
    imageLoader: StripeImageLoader,
    iconUrl: String,
    header: String?,
    description: String,
) {
    Row(
        modifier = Modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StripeImage(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .size(32.dp),
            url = iconUrl,
            debugPainter = ColorPainter(LinkTheme.colors.surfaceTertiary),
            imageLoader = imageLoader,
            contentDescription = null,
        )
        Column(modifier = Modifier.weight(1f)) {
            if (header != null) {
                Text(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    text = header,
                    style = LinkTheme.typography.detailEmphasized,
                    color = LinkTheme.colors.textPrimary,
                )
            }
            Text(
                modifier = Modifier.padding(horizontal = 8.dp),
                text = description,
                style = LinkTheme.typography.detail,
                color = LinkTheme.colors.textTertiary,
            )
        }
    }
}

@Composable
internal fun Disclaimer(disclaimer: String) {
    Text(
        text = disclaimer,
        style = LinkTheme.typography.caption,
        color = LinkTheme.colors.textTertiary,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun ActionButtons(
    denyButtonLabel: String?,
    allowButtonLabel: String,
    onAllow: () -> Unit,
    onDeny: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (denyButtonLabel != null && onDeny != null) {
            ActionButton(
                modifier = Modifier.weight(1f),
                type = ActionButtonType.Secondary,
                label = denyButtonLabel,
                onClick = onDeny,
            )
        }
        ActionButton(
            modifier = Modifier.weight(1f),
            type = ActionButtonType.Primary,
            label = allowButtonLabel,
            onClick = onAllow,
        )
    }
}

@Composable
private fun ActionButton(
    type: ActionButtonType,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = when (type) {
        ActionButtonType.Secondary -> LinkTheme.colors.surfaceSecondary
        ActionButtonType.Primary -> LinkTheme.colors.buttonPrimary
    }
    val textColor = when (type) {
        ActionButtonType.Secondary -> LinkTheme.colors.textSecondary
        ActionButtonType.Primary -> LinkTheme.colors.textOnButtonPrimary
    }
    TextButton(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor),
        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp),
        onClick = onClick,
    ) {
        Text(
            text = label,
            style = LinkTheme.typography.bodyEmphasized,
            color = textColor,
        )
    }
}

private enum class ActionButtonType {
    Secondary,
    Primary,
}

@PreviewLightDark
@Composable
private fun OAuthConsentScreenPreview() {
    DefaultLinkTheme {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = LinkTheme.colors.surfacePrimary,
        ) {
            OAuthConsentScreen(
                merchantLogoUrl = null,
                ConsentPane(
                    title = "Connect Powdur\nwith Link",
                    userSection = ConsentPane.UserSection(
                        iconUrl = "",
                        label = "jane.diaz@example.com",
                    ),
                    scopesSection = ConsentPane.ScopesSection(
                        header = "Powdur will have access to:",
                        scopes = listOf(
                            ConsentPane.ScopesSection.Scope(
                                iconUrl = "",
                                header = "Account info",
                                description = "Name, email, and profile picture",
                            ),
                            ConsentPane.ScopesSection.Scope(
                                iconUrl = "",
                                header = "Addresses",
                                description = "Shipping addresses",
                            ),
                            ConsentPane.ScopesSection.Scope(
                                iconUrl = "",
                                header = "Wallet",
                                description = "Cards, bank accounts",
                            ),
                        ),
                    ),
                    denyButtonLabel = "Cancel",
                    allowButtonLabel = "Allow",
                    disclaimer = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
                        "Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
                )
            )
        }
    }
}
