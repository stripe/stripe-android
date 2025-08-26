package com.stripe.android.link.ui.oauth

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.LinkTheme
import com.stripe.android.link.ui.ErrorText
import com.stripe.android.link.ui.LocalLinkContentScrollHandler
import com.stripe.android.link.ui.image.LocalStripeImageLoader
import com.stripe.android.model.ConsentUi
import com.stripe.android.uicore.image.StripeImage
import com.stripe.android.uicore.image.StripeImageLoader
import com.stripe.android.uicore.strings.resolve
import com.stripe.android.uicore.text.Html
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
        errorMessage = viewState.errorMessage,
        onAllowClick = viewModel::onAllowClick,
        onDenyClick = viewModel::onDenyClick,
    )
}

@Composable
internal fun OAuthConsentScreen(
    merchantLogoUrl: String?,
    userEmail: String,
    pane: ConsentUi.ConsentPane,
    errorMessage: ResolvableString? = null,
    onAllowClick: () -> Unit,
    onDenyClick: (() -> Unit)? = null,
) {
    val contentScrollHandler = LocalLinkContentScrollHandler.current
    val scrollState = rememberScrollState()

    LaunchedEffect(scrollState.canScrollBackward) {
        contentScrollHandler?.handleCanScrollBackwardChanged(scrollState.canScrollBackward)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MerchantLogo(merchantLogoUrl)
            Spacer(Modifier.height(16.dp))
            Title(pane.title)
            Spacer(Modifier.height(24.dp))
            UserSection(userEmail)
            Spacer(Modifier.height(24.dp))
            ScopesSection(pane.scopesSection)
            Spacer(Modifier.height(8.dp))
            errorMessage?.let {
                Spacer(Modifier.height(16.dp))
                ErrorSection(it)
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            pane.disclaimer?.let {
                Spacer(Modifier.height(16.dp))
                Disclaimer(it)
            }
            Spacer(Modifier.height(16.dp))
            ActionButtons(
                denyButtonLabel = pane.denyButtonLabel,
                allowButtonLabel = pane.allowButtonLabel,
                onAllowClick = onAllowClick,
                onDenyClick = onDenyClick
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ErrorSection(
    errorMessage: ResolvableString,
) {
    ErrorText(
        modifier = Modifier
            .border(1.dp, LinkTheme.colors.outline, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        text = errorMessage.resolve(),
    )
}

@Composable
private fun MerchantLogo(
    merchantLogoUrl: String?,
) {
    val imageLoader = LocalStripeImageLoader.current
    val modifier = Modifier
        .clip(RoundedCornerShape(16.dp))
        .size(56.dp)
        .background(LinkTheme.colors.surfaceSecondary)
    if (merchantLogoUrl == null) {
        DefaultMerchantLogo(modifier)
    } else {
        StripeImage(
            modifier = modifier,
            url = merchantLogoUrl,
            debugPainter = ColorPainter(LinkTheme.colors.iconBrand),
            imageLoader = imageLoader,
            contentDescription = null,
            errorContent = {
                DefaultMerchantLogo(modifier)
            }
        )
    }
}

@Composable
private fun DefaultMerchantLogo(modifier: Modifier) {
    Image(
        modifier = modifier.padding(18.dp),
        painter = painterResource(com.stripe.android.uicore.R.drawable.stripe_ic_business),
        contentDescription = null,
        colorFilter = ColorFilter.tint(LinkTheme.colors.iconPrimary)
    )
}

@Composable
private fun Title(title: String) {
    Text(
        modifier = Modifier.fillMaxWidth(),
        text = title,
        style = LinkTheme.typography.title,
        textAlign = TextAlign.Center,
        color = LinkTheme.colors.textPrimary,
    )
}

@Composable
private fun UserSection(email: String) {
    Row(
        modifier = Modifier.border(1.dp, LinkTheme.colors.outline, CircleShape),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
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
        modifier = Modifier.fillMaxWidth(),
        text = section.header,
        style = LinkTheme.typography.detail,
        color = LinkTheme.colors.textTertiary,
    )
    Spacer(Modifier.height(16.dp))
    Column(
        modifier = Modifier
            .border(1.dp, LinkTheme.colors.outline, RoundedCornerShape(12.dp))
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
            .clip(RoundedCornerShape(10.dp))
            .size(36.dp)
            .background(LinkTheme.colors.surfaceSecondary)
            .padding(10.dp)
        StripeImage(
            modifier = modifier,
            url = iconUrl,
            debugPainter = painterResource(com.stripe.android.ui.core.R.drawable.stripe_ic_lock),
            imageLoader = imageLoader,
            contentDescription = null,
            disableAnimations = true,
            colorFilter = ColorFilter.tint(LinkTheme.colors.iconPrimary),
            errorContent = {
                DefaultScopeIcon(modifier)
            }
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            if (header != null) {
                Text(
                    text = header,
                    style = LinkTheme.typography.detailEmphasized,
                    color = LinkTheme.colors.textPrimary,
                )
            }
            Html(
                html = description,
                style = LinkTheme.typography.detail,
                color = LinkTheme.colors.textTertiary,
            )
        }
    }
}

@Composable
private fun DefaultScopeIcon(modifier: Modifier) {
    Image(
        modifier = modifier,
        painter = painterResource(com.stripe.android.ui.core.R.drawable.stripe_ic_lock),
        contentDescription = null,
    )
}

@Composable
internal fun Disclaimer(disclaimer: String) {
    Html(
        html = disclaimer,
        color = LinkTheme.colors.textTertiary,
        style = LinkTheme.typography.caption.copy(textAlign = TextAlign.Center),
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
        ActionButtonType.Primary -> LinkTheme.colors.onButtonPrimary
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
@VisibleForTesting
internal fun OAuthConsentScreenPreview() {
    DefaultLinkTheme {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = LinkTheme.colors.surfacePrimary,
        ) {
            OAuthConsentScreenPreviewHelper(
                merchantLogoUrl = null,
                userEmail = "jane.diaz@example.com",
                pane = consentPanePreview,
            )
        }
    }
}

@Composable
private fun OAuthConsentScreenPreviewHelper(
    merchantLogoUrl: String?,
    userEmail: String,
    pane: ConsentUi.ConsentPane,
    errorMessage: ResolvableString? = null,
) {
    OAuthConsentScreen(
        merchantLogoUrl = merchantLogoUrl,
        userEmail = userEmail,
        pane = pane,
        errorMessage = errorMessage,
        onAllowClick = {},
        onDenyClick = {},
    )
}

internal val consentPanePreview = run {
    val scopeIconUrl =
        "https://b.stripecdn.com/connections-statics-srv/assets/SailIcon--lock-primary-3x.png"
    val scopeIcon = ConsentUi.Icon(scopeIconUrl)
    ConsentUi.ConsentPane(
        title = "Connect Powdur with Link",
        scopesSection = ConsentUi.ConsentPane.ScopesSection(
            header = "Powdur will have access to:",
            scopes = listOf(
                ConsentUi.ConsentPane.ScopesSection.Scope(
                    icon = scopeIcon,
                    header = "Account",
                    description = "View and manage your name, email, phone, and shipping addresses",
                ),
                ConsentUi.ConsentPane.ScopesSection.Scope(
                    icon = scopeIcon,
                    header = "Wallet",
                    description = "View and manage your cards and bank accounts",
                ),
                ConsentUi.ConsentPane.ScopesSection.Scope(
                    icon = scopeIcon,
                    header = "Identity",
                    description = "View your identity information (date of birth, address, " +
                        "<a href=''>ID documents</a>)",
                ),
            ),
        ),
        denyButtonLabel = "Cancel",
        allowButtonLabel = "Continue",
        disclaimer = "Lorem ipsum <a href=''>dolor</a> sit amet, consectetur adipiscing elit. " +
            "Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
    )
}
