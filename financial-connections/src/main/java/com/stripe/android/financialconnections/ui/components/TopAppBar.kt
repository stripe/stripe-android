package com.stripe.android.financialconnections.ui.components

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.navigation.bottomsheet.BottomSheetNavigator
import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarState
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.LocalNavHostController
import com.stripe.android.financialconnections.ui.theme.Attention300
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.financialconnections.ui.theme.Theme
import com.stripe.android.financialconnections.utils.KeyboardController
import com.stripe.android.financialconnections.utils.rememberKeyboardController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private val LOGO_WIDTH = 50.dp
private val LOGO_HEIGHT = 20.dp
private val PILL_HORIZONTAL_PADDING = 4.dp
private val PILL_VERTICAL_PADDING = 2.dp
private const val PILL_RADIUS = 8f

@Composable
internal fun FinancialConnectionsTopAppBar(
    state: TopAppBarState,
    onCloseClick: () -> Unit
) {
    val elevation = animateDpAsState(
        targetValue = if (state.isElevated) AppBarDefaults.TopAppBarElevation else 0.dp,
        label = "TopAppBarElevation",
    )

    FinancialConnectionsTopAppBar(
        hideStripeLogo = state.hideStripeLogo || state.forceHideStripeLogo,
        testMode = state.isTestMode,
        verified = state.isVerified,
        theme = state.theme,
        elevation = elevation,
        allowBackNavigation = state.allowBackNavigation,
        onCloseClick = onCloseClick,
    )
}

@Composable
private fun FinancialConnectionsTopAppBar(
    hideStripeLogo: Boolean,
    testMode: Boolean,
    verified: Boolean,
    theme: Theme,
    elevation: State<Dp>,
    allowBackNavigation: Boolean,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current
    val localBackPressed = onBackPressedDispatcher?.onBackPressedDispatcher

    val navController = LocalNavHostController.current
    val canShowBackIcon by navController.collectCanShowBackIconAsState()

    val keyboardController = rememberKeyboardController()
    val scope = rememberCoroutineScope()

    TopAppBar(
        title = {
            Title(
                hideStripeLogo = hideStripeLogo,
                testmode = testMode,
                verified = verified,
                theme = theme,
            )
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
        backgroundColor = FinancialConnectionsTheme.colors.backgroundSurface,
        contentColor = FinancialConnectionsTheme.colors.textBrand,
        modifier = modifier.graphicsLayer {
            shadowElevation = elevation.value.toPx()
        }
    )
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
            imageVector = Icons.Filled.ArrowBack,
            contentDescription = "Back icon",
            tint = FinancialConnectionsTheme.colors.iconDefault,
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
            imageVector = Icons.Filled.Close,
            contentDescription = "Close icon",
            tint = FinancialConnectionsTheme.colors.iconDefault
        )
    }
}

@Composable
private fun Title(
    hideStripeLogo: Boolean,
    verified: Boolean,
    testmode: Boolean,
    theme: Theme,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (hideStripeLogo.not()) {
            Image(
                modifier = Modifier.size(width = LOGO_WIDTH, height = LOGO_HEIGHT),
                painter = painterResource(id = theme.icon),
                contentDescription = null // decorative element
            )
        }
        // show a test mode pill if in test mode
        if (testmode) {
            Text(
                modifier = Modifier
                    .drawBehind {
                        drawRoundRect(
                            color = Attention300,
                            cornerRadius = CornerRadius(PILL_RADIUS)
                        )
                    }
                    .padding(vertical = PILL_VERTICAL_PADDING, horizontal = PILL_HORIZONTAL_PADDING),
                text = "Test",
                style = FinancialConnectionsTheme.typography.labelMediumEmphasized,
                color = FinancialConnectionsTheme.colors.textWhite
            )
        }
        if (verified) {
            // load an image
            Image(
                modifier = Modifier.size(width = 20.dp, height = 20.dp),
                // drawable painter
                painter = painterResource(id = R.drawable.ic_verified),
                contentDescription = null // decorative element
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
            state = TopAppBarState(hideStripeLogo = false, isVerified = true),
            onCloseClick = {},
        )
    }
}

@Preview(group = "Components", name = "TopAppBar - Instant Debits")
@Composable
internal fun TopAppBarWithLinkLogoPreview() {
    FinancialConnectionsPreview {
        FinancialConnectionsTopAppBar(
            state = TopAppBarState(
                hideStripeLogo = false,
                isVerified = true,
                theme = Theme.LinkLight,
            ),
            onCloseClick = {},
        )
    }
}

@Preview(group = "Components", name = "TopAppBar - no Stripe logo")
@Composable
internal fun TopAppBarNoStripeLogoPreview() {
    FinancialConnectionsPreview {
        FinancialConnectionsTopAppBar(
            state = TopAppBarState(hideStripeLogo = true),
            onCloseClick = {},
        )
    }
}
