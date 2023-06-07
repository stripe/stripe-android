package com.stripe.android.customersheet.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.SwipeableDefaults
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.google.accompanist.navigation.material.BottomSheetNavigator
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.ModalBottomSheetLayout
import com.google.accompanist.navigation.material.bottomSheet

@OptIn(ExperimentalMaterialNavigationApi::class)
@Composable
internal fun CustomerBottomSheet(
    navController: NavHostController,
    onClose: () -> Unit = {},
    sheetContent: @Composable ColumnScope.() -> Unit,
) {
    val bottomSheetNavigator = rememberBottomSheetNavigator()
    navController.navigatorProvider.addNavigator(bottomSheetNavigator)

    BackHandler(bottomSheetNavigator.navigatorSheetState.isVisible) {
        navController.popBackStack()
    }

    LaunchedEffect(navController.currentDestination) {
        if (navController.isInitialized && navController.currentDestination == null) {
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

private val NavController.isInitialized: Boolean
    get() = runCatching { graph }.isSuccess

@ExperimentalMaterialNavigationApi
@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun rememberBottomSheetNavigator(
    animationSpec: AnimationSpec<Float> = SwipeableDefaults.AnimationSpec,
    skipHalfExpanded: Boolean = true,
): BottomSheetNavigator {
    val sheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        animationSpec = animationSpec,
        skipHalfExpanded = skipHalfExpanded,
    )
    return remember { BottomSheetNavigator(sheetState) }
}
