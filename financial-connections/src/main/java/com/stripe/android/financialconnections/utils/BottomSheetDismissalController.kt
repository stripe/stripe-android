package com.stripe.android.financialconnections.utils

import androidx.compose.runtime.snapshotFlow
import androidx.navigation.NavHostController
import androidx.navigation.get
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.navigation.bottomsheet.BottomSheetNavigator
import com.stripe.android.financialconnections.navigation.pane
import com.stripe.android.financialconnections.navigation.rendersInBottomSheet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * This method helps fix an issue with bottom sheets where the NavController's current
 * destination isn't updated when the sheet is dismissed by swiping or tapping the scrim.
 */
internal suspend fun NavHostController.keepInSyncWithBottomSheet() {
    val dismissalController = BottomSheetDismissalController(this)
    dismissalController.onDismissedBySwipe {
        popBackStack()
    }
}

internal class BottomSheetDismissalController(
    private val isBottomSheetVisibleFlow: Flow<Boolean>,
    private val currentPaneFlow: Flow<FinancialConnectionsSessionManifest.Pane>,
) {

    constructor(
        navController: NavHostController,
    ) : this(
        isBottomSheetVisibleFlow = navController.isBottomSheetVisibleFlow,
        currentPaneFlow = navController.currentPaneFlow,
    )

    suspend fun onDismissedBySwipe(action: () -> Unit) {
        var wasSheetVisible = false

        combine(isBottomSheetVisibleFlow, currentPaneFlow) { sheetVisible, pane ->
            val wasDismissed = wasSheetVisible && !sheetVisible
            wasSheetVisible = sheetVisible
            wasDismissed && pane.rendersInBottomSheet
        }.collect { shouldPopBackStack ->
            if (shouldPopBackStack) {
                // The sheet was dismissed, but the NavController doesn't know about it yet.
                action()
            }
        }
    }
}

private val NavHostController.isBottomSheetVisibleFlow: Flow<Boolean>
    get() {
        val bottomSheetNavigator = navigatorProvider[BottomSheetNavigator::class]
        return snapshotFlow { bottomSheetNavigator.navigatorSheetState.isVisible }
    }

private val NavHostController.currentPaneFlow: Flow<FinancialConnectionsSessionManifest.Pane>
    get() = currentBackStackEntryFlow.map { it.destination.pane }
