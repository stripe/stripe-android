package com.stripe.android.financialconnections.features.notice

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
import com.stripe.android.financialconnections.features.notice.NoticeSheetState.NoticeSheetContent
import com.stripe.android.financialconnections.features.notice.NoticeSheetState.NoticeSheetContent.DataAccess
import com.stripe.android.financialconnections.features.notice.NoticeSheetState.NoticeSheetContent.Legal
import com.stripe.android.financialconnections.features.notice.NoticeSheetState.ViewEffect.OpenUrl
import com.stripe.android.financialconnections.presentation.paneViewModel

@Composable
internal fun NoticeSheet(
    backStackEntry: NavBackStackEntry,
) {
    val viewModel: NoticeSheetViewModel = paneViewModel {
        NoticeSheetViewModel.factory(it, backStackEntry.arguments)
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
    content: NoticeSheetContent,
    onClickableTextClick: (String) -> Unit,
    onConfirmModalClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        when (content) {
            is Legal -> LegalDetailsBottomSheetContent(
                legalDetails = content.legalDetails,
                onConfirmModalClick = onConfirmModalClick,
                onClickableTextClick = onClickableTextClick
            )

            is DataAccess -> DataAccessBottomSheetContent(
                dataDialog = content.dataAccess,
                onConfirmModalClick = onConfirmModalClick,
                onClickableTextClick = onClickableTextClick
            )
        }
    }
}
