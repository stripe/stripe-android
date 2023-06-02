package com.stripe.android.customersheet.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.ModalBottomSheetLayout
import com.google.accompanist.navigation.material.bottomSheet
import com.google.accompanist.navigation.material.rememberBottomSheetNavigator

@OptIn(ExperimentalMaterialNavigationApi::class)
@Composable
internal fun CustomerBottomSheet(
    onClose: () -> Unit = {},
    sheetContent: @Composable ColumnScope.() -> Unit,
) {
    val bottomSheetNavigator = rememberBottomSheetNavigator()
    val navController = rememberNavController(bottomSheetNavigator)

    BackHandler(bottomSheetNavigator.navigatorSheetState.isVisible) {
        navController.popBackStack()
        onClose()
    }

    LaunchedEffect(navController.currentDestination) {
        if (navController.currentDestination == null) {
            navController.popBackStack()
            onClose()
        }
    }

    ModalBottomSheetLayout(
        bottomSheetNavigator = bottomSheetNavigator,
    ) {
        NavHost(navController, "sheet") {
            bottomSheet(route = "sheet") {
                sheetContent()
            }
        }
    }
}
