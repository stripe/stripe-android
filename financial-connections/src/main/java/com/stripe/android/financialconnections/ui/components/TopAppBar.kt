package com.stripe.android.financialconnections.ui.components

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
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
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.financialconnections.utils.rememberKeyboardController
import kotlinx.coroutines.launch

@Composable
internal fun FinancialConnectionsTopAppBar(
    hideStripeLogo: Boolean = LocalReducedBranding.current,
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
        title = if (hideStripeLogo) {
            { /* Empty content */ }
        } else {
            {
                Icon(
                    painter = painterResource(id = R.drawable.stripe_logo),
                    contentDescription = null // decorative element
                )
            }
        },
        elevation = elevation,
        navigationIcon = if (canGoBack && showBack) {
            {
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
        } else {
            null
        },
        actions = {
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
        },
        backgroundColor = FinancialConnectionsTheme.colors.textWhite,
        contentColor = FinancialConnectionsTheme.colors.textBrand
    )
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
