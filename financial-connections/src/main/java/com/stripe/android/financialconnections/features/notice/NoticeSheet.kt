package com.stripe.android.financialconnections.features.notice

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.navigation.NavBackStackEntry
import com.stripe.android.financialconnections.features.common.DataAccessBottomSheetContent
import com.stripe.android.financialconnections.features.common.GenericBottomSheetContent
import com.stripe.android.financialconnections.features.common.LegalDetailsBottomSheetContent
import com.stripe.android.financialconnections.features.notice.NoticeSheetState.NoticeSheetContent
import com.stripe.android.financialconnections.features.notice.NoticeSheetState.NoticeSheetContent.DataAccess
import com.stripe.android.financialconnections.features.notice.NoticeSheetState.NoticeSheetContent.Generic
import com.stripe.android.financialconnections.features.notice.NoticeSheetState.NoticeSheetContent.Legal
import com.stripe.android.financialconnections.features.notice.NoticeSheetState.ViewEffect.OpenUrl
import com.stripe.android.financialconnections.presentation.paneViewModel
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.uicore.utils.collectAsState

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
        NoticeSheetContent(
            content = content,
            onClickableTextClick = viewModel::handleClickableTextClick,
            onConfirmModalClick = viewModel::handleConfirmModalClick,
            onViewEffectLaunched = viewModel::onViewEffectLaunched
        )
    }
}

@Composable
private fun NoticeSheetContent(
    content: NoticeSheetContent,
    onClickableTextClick: (String) -> Unit,
    onConfirmModalClick: () -> Unit,
    onViewEffectLaunched: () -> Unit,
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
            is Generic -> GenericBottomSheetContent(
                screen = content.generic,
                onClickableTextClick = onClickableTextClick,
                onPrimaryButtonClick = onConfirmModalClick,
                onSecondaryButtonClick = {} // TODO handle secondary button clicks.
            )
            /**
             * We're not expecting update required content on the generic notice sheet yet, as it's managed by
             * [com.stripe.android.financialconnections.features.accountupdate.AccountUpdateRequiredModalKt].
             */
            is NoticeSheetContent.UpdateRequired -> GenericBottomSheetContent(
                screen = content.generic,
                onClickableTextClick = onClickableTextClick,
                onPrimaryButtonClick = onConfirmModalClick,
                onSecondaryButtonClick = {}
            )
        }
        onViewEffectLaunched()
    }
}

@Composable
@Preview(group = "Notice Sheet")
internal fun NoticeSheetPreview(
    @PreviewParameter(NoticeSheetPreviewParameterProvider::class) content: NoticeSheetContent,
) {
    FinancialConnectionsPreview {
        NoticeSheetContent(
            content = content,
            onClickableTextClick = {},
            onConfirmModalClick = {},
            onViewEffectLaunched = {},
        )
    }
}
