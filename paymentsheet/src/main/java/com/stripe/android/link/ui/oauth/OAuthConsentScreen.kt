package com.stripe.android.link.ui.oauth

import androidx.compose.foundation.Image
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.LinkTheme
import com.stripe.android.link.ui.ScrollableTopLevelColumn
import com.stripe.android.link.ui.image.LocalStripeImageLoader
import com.stripe.android.model.ConsentUi
import com.stripe.android.uicore.image.StripeImage
import com.stripe.android.uicore.image.StripeImageLoader
import com.stripe.android.uicore.utils.collectAsState

@Composable
internal fun OAuthConsentScreen(
    viewModel: OAuthConsentViewModel,
) {
    val viewState by viewModel.viewState.collectAsState()
    val consentPane = viewState.consentPane
        ?: return // Invalid. VM will dismiss.
    OAuthConsentScreen(
        merchantLogoUrl = viewState.merchantLogoUrl,
        userEmail = viewState.userEmail,
        pane = consentPane,
        onAllowClick = viewModel::onAllowClick,
        onDenyClick = viewModel::onDenyClick,
    )
}

@Composable
internal fun OAuthConsentScreen(
    merchantLogoUrl: String?,
    userEmail: String,
    pane: ConsentUi.ConsentPane,
    onAllowClick: () -> Unit,
    onDenyClick: (() -> Unit)? = null,
) {
    ScrollableTopLevelColumn {
        MerchantLogo(merchantLogoUrl)
        Title(pane.title)
        UserSection(userEmail)
        ScopesSection(pane.scopesSection)

        pane.disclaimer?.let {
            Spacer(Modifier.height(24.dp))
            Disclaimer(it)
        }
        Spacer(Modifier.height(16.dp))
        ActionButtons(
            denyButtonLabel = pane.denyButtonLabel,
            allowButtonLabel = pane.allowButtonLabel,
            onAllowClick = onAllowClick,
            onDenyClick = onDenyClick
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
        .background(LinkTheme.colors.surfaceTertiary)
    if (merchantLogoUrl == null) {
        Image(
            modifier = modifier.padding(18.dp),
            painter = painterResource(com.stripe.android.ui.core.R.drawable.stripe_ic_business),
            contentDescription = null,
            colorFilter = ColorFilter.tint(LinkTheme.colors.iconPrimary)
        )
    } else {
        StripeImage(
            modifier = modifier,
            url = merchantLogoUrl,
            debugPainter = ColorPainter(LinkTheme.colors.iconBrand),
            imageLoader = imageLoader,
            contentDescription = null,
            errorContent = {
                Box(modifier.background(Color.Red)) // TODO.
            }
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
private fun UserSection(email: String) {
    Row(
        modifier = Modifier
            .border(1.dp, LinkTheme.colors.textTertiary, CircleShape)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 8.dp),
            text = email,
            style = LinkTheme.typography.detail,
            color = LinkTheme.colors.textPrimary,
        )
    }
}

@Composable
private fun ScopesSection(section: ConsentUi.ConsentPane.ScopesSection) {
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
                iconUrl = scope.icon.default,
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
    ) {
        val modifier = Modifier
            .padding(top = 2.dp)
            .size(16.dp)
        // TODO: Center align with top row.
        StripeImage(
            modifier = modifier,
            url = iconUrl,
            debugPainter = painterResource(com.stripe.android.ui.core.R.drawable.stripe_ic_lock),
            imageLoader = imageLoader,
            contentDescription = null,
            disableAnimations = true,
            colorFilter = ColorFilter.tint(LinkTheme.colors.iconPrimary),
            errorContent = {
                Box(modifier) // TODO.
            }
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
    onAllowClick: () -> Unit,
    onDenyClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (denyButtonLabel != null && onDenyClick != null) {
            ActionButton(
                modifier = Modifier.weight(1f),
                type = ActionButtonType.Secondary,
                label = denyButtonLabel,
                onClick = onDenyClick,
            )
        }
        ActionButton(
            modifier = Modifier.weight(1f),
            type = ActionButtonType.Primary,
            label = allowButtonLabel,
            onClick = onAllowClick,
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
            OAuthConsentScreenPreviewHelper(
                merchantLogoUrl = null,
                userEmail = "jane.diaz@example.com",
                pane = consentPanePreview
            )
        }
    }
}

@Composable
private fun OAuthConsentScreenPreviewHelper(
    merchantLogoUrl: String?,
    userEmail: String,
    pane: ConsentUi.ConsentPane,
) {
    OAuthConsentScreen(
        merchantLogoUrl = merchantLogoUrl,
        userEmail = userEmail,
        pane = pane,
        onAllowClick = {},
        onDenyClick = {},
    )
}

internal val consentPanePreview = run {
    val scopeIconUrl =
        "https://b.stripecdn.com/connections-statics-srv/assets/SailIcon--lock-primary-3x.png"
    val scopeIcon = ConsentUi.Icon(scopeIconUrl)
    ConsentUi.ConsentPane(
        title = "Connect Powdur\nwith Link",
        scopesSection = ConsentUi.ConsentPane.ScopesSection(
            header = "Powdur will have access to:",
            scopes = listOf(
                ConsentUi.ConsentPane.ScopesSection.Scope(
                    icon = scopeIcon,
                    header = "Account info",
                    description = "Name, email, and profile picture",
                ),
                ConsentUi.ConsentPane.ScopesSection.Scope(
                    icon = scopeIcon,
                    header = "Addresses",
                    description = "Shipping addresses",
                ),
                ConsentUi.ConsentPane.ScopesSection.Scope(
                    icon = scopeIcon,
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
}
