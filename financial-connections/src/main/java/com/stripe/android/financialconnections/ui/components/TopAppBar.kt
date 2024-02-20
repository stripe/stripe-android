@file:Suppress("ktlint:filename")

package com.stripe.android.financialconnections.ui.components

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.LocalNavHostController
import com.stripe.android.financialconnections.ui.LocalReducedBranding
import com.stripe.android.financialconnections.ui.LocalTestMode
import com.stripe.android.financialconnections.ui.theme.Attention200
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
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
    hideStripeLogo: Boolean = LocalReducedBranding.current,
    testMode: Boolean = LocalTestMode.current,
    elevation: Dp = 0.dp,
    showBack: Boolean = true,
    onCloseClick: () -> Unit
) {
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current
    val localBackPressed = onBackPressedDispatcher?.onBackPressedDispatcher

    val navController = LocalNavHostController.current
    val canGoBack by navController.collectCanGoBackAsState()

    val keyboardController = rememberKeyboardController()
    val scope = rememberCoroutineScope()

    TopAppBar(
        title = {
            Title(
                hideStripeLogo = hideStripeLogo,
                testmode = testMode
            )
        },
        elevation = elevation,
        navigationIcon = if (canGoBack && showBack) {
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
        backgroundColor = FinancialConnectionsTheme.colors.textWhite,
        contentColor = FinancialConnectionsTheme.colors.textBrand
    )
}

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
            tint = FinancialConnectionsTheme.colors.iconDefault
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
private fun Title(hideStripeLogo: Boolean, testmode: Boolean) = Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
    if (hideStripeLogo.not()) {
        Icon(
            modifier = Modifier.size(width = LOGO_WIDTH, height = LOGO_HEIGHT),
            painter = painterResource(id = R.drawable.stripe_logo),
            contentDescription = null // decorative element
        )
    }
    // show a test mode pill if in test mode
    if (testmode) {
        Text(
            modifier = Modifier
                .drawBehind {
                    drawRoundRect(
                        color = Attention200,
                        cornerRadius = CornerRadius(PILL_RADIUS)
                    )
                }
                .padding(vertical = PILL_VERTICAL_PADDING, horizontal = PILL_HORIZONTAL_PADDING),
            text = "Test",
            style = FinancialConnectionsTheme.typography.labelMediumEmphasized,
            color = FinancialConnectionsTheme.colors.textWhite
        )
    }
}

/**
 * calculates toolbar elevation based on [ScrollState]
 */
internal val ScrollState.elevation: Dp
    get() {
        return minOf(value.dp, AppBarDefaults.TopAppBarElevation)
    }

/**
 * calculates toolbar elevation based on [LazyListState]
 */
internal val LazyListState.elevation: Dp
    get() = if (firstVisibleItemIndex == 0) {
        // For the first element, use the minimum of scroll offset and default elevation
        // i.e. a value between 0 and 4.dp
        minOf(firstVisibleItemScrollOffset.toFloat().dp, AppBarDefaults.TopAppBarElevation)
    } else {
        // If not the first element, always set elevation and show the shadow
        AppBarDefaults.TopAppBarElevation
    }

@Composable
private fun NavHostController.collectCanGoBackAsState(): State<Boolean> {
    val canGoBack = remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        val listener = NavController.OnDestinationChangedListener { controller, _, _ ->
            canGoBack.value = controller.previousBackStackEntry != null
        }
        addOnDestinationChangedListener(listener)
        onDispose {
            removeOnDestinationChangedListener(listener)
        }
    }
    return canGoBack
}

@Preview(group = "Components", name = "TopAppBar")
@Composable
internal fun TopAppBarNoStripeLogoPreview() {
    FinancialConnectionsPreview {
        FinancialConnectionsTopAppBar {}
    }
}

@Preview(group = "Components", name = "TopAppBar - no Stripe logo")
@Composable
internal fun FinancialConnectionsTopAppBarPreview() {
    FinancialConnectionsPreview {
        FinancialConnectionsTopAppBar(
            hideStripeLogo = true
        ) {}
    }
}
