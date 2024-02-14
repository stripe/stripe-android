package com.stripe.android.financialconnections.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.get
import com.stripe.android.financialconnections.navigation.bottomsheet.BottomSheetNavigator
import com.stripe.android.financialconnections.navigation.pane
import com.stripe.android.financialconnections.navigation.rendersInBottomSheet
import com.stripe.android.financialconnections.ui.LocalNavHostController
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.colors
import com.stripe.android.financialconnections.ui.theme.Neutral900

@Composable
internal fun FinancialConnectionsModalBottomSheetLayout(
    sheetContent: @Composable ColumnScope.() -> Unit,
    sheetState: ModalBottomSheetState,
    content: @Composable () -> Unit
) {
    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetBackgroundColor = colors.backgroundSurface,
        sheetShape = RoundedCornerShape(20.dp, 20.dp, 0.dp, 0.dp),
        scrimColor = Neutral900.copy(alpha = 0.32f),
        sheetContent = sheetContent,
        content = content
    )
}

@Composable
internal fun FinancialConnectionsModalBottomSheetLayout(
    bottomSheetNavigator: BottomSheetNavigator,
    content: @Composable () -> Unit
) {
    val navController = LocalNavHostController.current

    LaunchedEffect(Unit) {
        navController.registerForBottomSheetDismissalFix(
            rendersInBottomSheet = { it.pane.rendersInBottomSheet },
        )
    }

    com.stripe.android.financialconnections.navigation.bottomsheet.ModalBottomSheetLayout(
        bottomSheetNavigator = bottomSheetNavigator,
        sheetBackgroundColor = colors.backgroundSurface,
        sheetShape = RoundedCornerShape(20.dp, 20.dp, 0.dp, 0.dp),
        scrimColor = Neutral900.copy(alpha = 0.32f),
        content = content,
    )
}

private suspend fun NavHostController.registerForBottomSheetDismissalFix(
    rendersInBottomSheet: (NavDestination) -> Boolean,
) {
    val bottomSheetNavigator = navigatorProvider[BottomSheetNavigator::class]
    bottomSheetNavigator.onDismiss {
        // The bottom sheet might be dismissed by the user swiping it down.
        // In this case, we have to manually tell the NavHostController to actually
        // pop the backstack. Otherwise, an empty overlay stays around and consumes
        // the next back press.
        val destination = currentDestination ?: return@onDismiss
        val isBottomSheet = rendersInBottomSheet(destination)
        if (isBottomSheet) {
            popBackStack()
        }
    }
}

private suspend fun BottomSheetNavigator.onDismiss(
    block: () -> Unit,
) {
    var wasVisible = navigatorSheetState.isVisible
    snapshotFlow { navigatorSheetState.isVisible }.collect { isVisible ->
        if (wasVisible && !isVisible) {
            block()
        }
        wasVisible = isVisible
    }
}
