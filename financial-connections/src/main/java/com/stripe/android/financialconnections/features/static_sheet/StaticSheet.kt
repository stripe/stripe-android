package com.stripe.android.financialconnections.features.static_sheet

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.navigation.NavBackStackEntry
import com.stripe.android.financialconnections.features.common.DataAccessBottomSheetContent
import com.stripe.android.financialconnections.features.common.LegalDetailsBottomSheetContent
import com.stripe.android.financialconnections.features.static_sheet.StaticSheetState.ViewEffect.OpenUrl
import com.stripe.android.financialconnections.presentation.paneViewModel
import com.stripe.android.financialconnections.repository.StaticSheetContent

@Composable
internal fun StaticSheet(
    backStackEntry: NavBackStackEntry,
) {
    val viewModel: StaticSheetViewModel = paneViewModel {
        StaticSheetViewModel.factory(it, backStackEntry.arguments)
    }

    val uriHandler = LocalUriHandler.current
    val state by viewModel.stateFlow.collectAsState()

    state.viewEffect?.let { viewEffect ->
        LaunchedEffect(viewEffect) {
            when (viewEffect) {
                is OpenUrl -> uriHandler.openUri(viewEffect.url)
            }
        }
    }

    state.content?.let { content ->
        StaticBottomSheetContent(
            content = content,
            onClickableTextClick = viewModel::handleClickableTextClick,
            onConfirmModalClick = viewModel::handleConfirmModalClick,
        )
    }
}

@Composable
private fun StaticBottomSheetContent(
    content: StaticSheetContent,
    onClickableTextClick: (String) -> Unit,
    onConfirmModalClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        when (content) {
            is StaticSheetContent.Legal -> LegalDetailsBottomSheetContent(
                legalDetails = content.legalDetails,
                onConfirmModalClick = onConfirmModalClick,
                onClickableTextClick = onClickableTextClick
            )

            is StaticSheetContent.DataAccess -> DataAccessBottomSheetContent(
                dataDialog = content.dataAccess,
                onConfirmModalClick = onConfirmModalClick,
                onClickableTextClick = onClickableTextClick
            )
        }
    }
}
