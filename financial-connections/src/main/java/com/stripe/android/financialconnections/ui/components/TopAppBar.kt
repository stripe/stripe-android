package com.stripe.android.financialconnections.ui.components

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.ThemeMode
import com.stripe.android.financialconnections.navigation.bottomsheet.BottomSheetNavigator
import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarState
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.LocalNavHostController
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.financialconnections.ui.theme.Theme
import com.stripe.android.financialconnections.ui.theme.isLink
import com.stripe.android.model.LinkBrand
import com.stripe.android.uicore.navigation.KeyboardController
import com.stripe.android.uicore.navigation.rememberKeyboardController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.stripe.android.uicore.R as StripeUiCoreR

private val LogoHeight = 20.dp
private val LinkLogoHeight = 24.dp

@Composable
internal fun FinancialConnectionsTopAppBar(
    state: TopAppBarState,
    onCloseClick: () -> Unit,
    onLogoClick: () -> Unit,
) {
    val elevation = animateDpAsState(
        targetValue = if (state.isElevated) AppBarDefaults.TopAppBarElevation else 0.dp,
        label = "TopAppBarElevation",
    )

    FinancialConnectionsTopAppBar(
        hideStripeLogo = state.hideStripeLogo || state.forceHideStripeLogo,
        theme = state.theme,
        linkBrand = state.linkBrand,
        elevation = elevation,
        allowBackNavigation = state.allowBackNavigation,
        logoClickable = state.logoClickNotice != null,
        onCloseClick = onCloseClick,
        onLogoClick = onLogoClick,
    )
}

@Composable
private fun FinancialConnectionsTopAppBar(
    hideStripeLogo: Boolean,
    theme: Theme,
    linkBrand: LinkBrand,
    elevation: State<Dp>,
    allowBackNavigation: Boolean,
    logoClickable: Boolean,
    onCloseClick: () -> Unit,
    onLogoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current
    val localBackPressed = onBackPressedDispatcher?.onBackPressedDispatcher

    val navController = LocalNavHostController.current
    val canShowBackIcon by navController.collectCanShowBackIconAsState()

    val keyboardController = rememberKeyboardController()
    val scope = rememberCoroutineScope()
    // Link DS 3.0 centers the logo across the full bar width, independent of the back button.
    val centerTitle = theme.isLink

    Box(modifier = modifier) {
        TopAppBar(
            title = {
                if (!centerTitle) {
                    Title(
                        hideStripeLogo = hideStripeLogo,
                        theme = theme,
                        linkBrand = linkBrand,
                        logoClickable = logoClickable,
                        onLogoClick = onLogoClick,
                    )
                }
            },
            elevation = 0.dp,
            navigationIcon = if (canShowBackIcon && allowBackNavigation) {
                {
                    BackButton(
                        scope = scope,
                        keyboardController = keyboardController,
                        localBackPressed = localBackPressed
                    )
                }
            } else {
                null
            },
            actions = {
                CloseButton(
                    scope = scope,
                    keyboardController = keyboardController,
                    onCloseClick = onCloseClick
                )
            },
            backgroundColor = FinancialConnectionsTheme.colors.background,
            contentColor = FinancialConnectionsTheme.colors.textAction,
            modifier = Modifier.graphicsLayer {
                shadowElevation = elevation.value.toPx()
            }
        )

        if (centerTitle) {
            Title(
                hideStripeLogo = hideStripeLogo,
                theme = theme,
                linkBrand = linkBrand,
                logoClickable = logoClickable,
                onLogoClick = onLogoClick,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun BackButton(
    scope: CoroutineScope,
    keyboardController: KeyboardController,
    localBackPressed: OnBackPressedDispatcher?
) {
    IconButton(
        onClick = {
            scope.launch {
                keyboardController.dismiss()
                localBackPressed?.onBackPressed()
            }
        },
    ) {
        Icon(
            painter = painterResource(StripeUiCoreR.drawable.stripe_ic_material_arrow_back),
            contentDescription = "Back icon",
            tint = FinancialConnectionsTheme.colors.icon,
            modifier = Modifier
                .testTag("top-app-bar-back-button")
                .semantics { testTagsAsResourceId = true }
        )
    }
}

@Composable
private fun CloseButton(
    scope: CoroutineScope,
    keyboardController: KeyboardController,
    onCloseClick: () -> Unit
) {
    IconButton(
        onClick = {
            scope.launch {
                keyboardController.dismiss()
                onCloseClick()
            }
        }
    ) {
        Icon(
            painter = painterResource(StripeUiCoreR.drawable.stripe_ic_material_close),
            contentDescription = "Close icon",
            tint = FinancialConnectionsTheme.colors.icon,
        )
    }
}

@Composable
private fun Title(
    hideStripeLogo: Boolean,
    theme: Theme,
    linkBrand: LinkBrand,
    logoClickable: Boolean,
    onLogoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (hideStripeLogo.not()) {
            val learnMore = stringResource(R.string.stripe_top_app_bar_logo_learn_more)
            Image(
                modifier = Modifier
                    .height(if (theme.isLink) LinkLogoHeight else LogoHeight)
                    .then(
                        if (logoClickable) {
                            Modifier.clickable(
                                role = Role.Button,
                                onClickLabel = learnMore,
                                onClick = onLogoClick,
                            )
                        } else {
                            Modifier
                        }
                    ),
                contentScale = ContentScale.FillHeight,
                painter = painterResource(id = theme.icon(linkBrand)),
                colorFilter = if (theme.isLink.not() && isSystemInDarkTheme()) {
                    ColorFilter.tint(FinancialConnectionsTheme.colors.textDefault)
                } else {
                    null
                },
                // The logo is decorative unless it can be clicked
                contentDescription = learnMore.takeIf { logoClickable },
            )
        }
    }
}

@Composable
private fun NavHostController.collectCanShowBackIconAsState(): State<Boolean> {
    val canShowBackIcon = remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        val listener = NavController.OnDestinationChangedListener { controller, destination, _ ->
            if (destination.navigatorName == BottomSheetNavigator::class.java.simpleName) {
                // We're looking at a bottom sheet, so don't change the back button
            } else {
                canShowBackIcon.value = controller.previousBackStackEntry != null
            }
        }
        addOnDestinationChangedListener(listener)
        onDispose {
            removeOnDestinationChangedListener(listener)
        }
    }
    return canShowBackIcon
}

@Preview(group = "Components", name = "TopAppBar")
@Composable
internal fun TopAppBarWithStripeLogoPreview() {
    FinancialConnectionsPreview {
        FinancialConnectionsTopAppBar(
            state = TopAppBarState(hideStripeLogo = false, linkBrand = LinkBrand.Link),
            onCloseClick = {},
            onLogoClick = {},
        )
    }
}

@Preview(group = "Components", name = "TopAppBar - OneLink")
@Composable
internal fun TopAppBarWithOneLinkLogoPreview() {
    FinancialConnectionsPreview(theme = Theme.Link(ThemeMode.Automatic)) {
        FinancialConnectionsTopAppBar(
            state = TopAppBarState(
                hideStripeLogo = false,
                linkBrand = LinkBrand.Onelink,
                theme = Theme.Link(ThemeMode.Automatic),
            ),
            onCloseClick = {},
            onLogoClick = {},
        )
    }
}

@Preview(group = "Components", name = "TopAppBar - Instant Debits")
@Composable
internal fun TopAppBarWithLinkLogoPreview() {
    FinancialConnectionsPreview(theme = Theme.Link(ThemeMode.Automatic)) {
        FinancialConnectionsTopAppBar(
            state = TopAppBarState(
                hideStripeLogo = false,
                theme = Theme.Link(ThemeMode.Automatic),
                linkBrand = LinkBrand.Link,
            ),
            onCloseClick = {},
            onLogoClick = {},
        )
    }
}

@Preview(group = "Components", name = "TopAppBar - no Stripe logo")
@Composable
internal fun TopAppBarNoStripeLogoPreview() {
    FinancialConnectionsPreview {
        FinancialConnectionsTopAppBar(
            state = TopAppBarState(hideStripeLogo = true, linkBrand = LinkBrand.Link),
            onCloseClick = {},
            onLogoClick = {},
        )
    }
}
