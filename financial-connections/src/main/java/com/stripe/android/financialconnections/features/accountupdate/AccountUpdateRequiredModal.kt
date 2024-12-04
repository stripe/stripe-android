package com.stripe.android.financialconnections.features.accountupdate

import Alignment
import FinancialConnectionsGenericInfoScreen
import FinancialConnectionsGenericInfoScreen.Footer
import FinancialConnectionsGenericInfoScreen.Footer.GenericInfoAction
import FinancialConnectionsGenericInfoScreen.Header
import FinancialConnectionsGenericInfoScreen.Options
import VerticalAlignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavBackStackEntry
import com.stripe.android.financialconnections.features.common.GenericBottomSheetContent
import com.stripe.android.financialconnections.features.notice.NoticeSheetState.NoticeSheetContent.UpdateRequired
import com.stripe.android.financialconnections.features.notice.NoticeSheetState.NoticeSheetContent.UpdateRequired.Type.Supportability
import com.stripe.android.financialconnections.model.Image
import com.stripe.android.financialconnections.presentation.paneViewModel
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.uicore.utils.collectAsState

@Composable
internal fun AccountUpdateRequiredModal(
    backStackEntry: NavBackStackEntry,
) {
    val viewModel: AccountUpdateRequiredViewModel = paneViewModel {
        AccountUpdateRequiredViewModel.factory(it, backStackEntry.arguments)
    }

    val state by viewModel.stateFlow.collectAsState()

    AccountUpdateRequiredModalContent(
        payload = state.payload(),
        onContinue = viewModel::handleContinue,
        onCancel = viewModel::handleCancel,
    )
}

@Composable
private fun AccountUpdateRequiredModalContent(
    payload: UpdateRequired?,
    onContinue: () -> Unit,
    onCancel: () -> Unit,
) {
    if (payload?.generic != null) {
        GenericBottomSheetContent(
            screen = payload.generic,
            onClickableTextClick = {}, // no clickable links expected in this modal.
            onPrimaryButtonClick = onContinue,
            onSecondaryButtonClick = onCancel,
        )
    }
}
//
//@Preview(
//    showBackground = true,
//    group = "Account Update Required Modal",
//)
//@Composable
//internal fun AccountUpdateRequiredModalPreview() {
//    FinancialConnectionsPreview {
//        AccountUpdateRequiredModalContent(
//            payload = UpdateRequired(
//                generic = FinancialConnectionsGenericInfoScreen(
//                    id = "sampleScreen1",
//                    header = Header(
//                        title = "Update required",
//                        subtitle = "Backend driven update subtitle. An update is required on this account!",
//                        icon = Image(default = "BrandIcon"),
//                        alignment = Alignment.Left
//                    ),
//                    body = null,
//                    footer = Footer(
//                        primaryCta = GenericInfoAction(
//                            id = "primaryCta1",
//                            label = "Continue",
//                            icon = null,
//                        ),
//                        secondaryCta = GenericInfoAction(
//                            id = "primaryCta1",
//                            label = "Cancel",
//                            icon = null,
//                        ),
//                        belowCta = null
//                    ),
//                    options = Options(
//                        fullWidthContent = true,
//                        verticalAlignment = VerticalAlignment.Default
//                    )
//                ),
//                type = Supportability(
//                    institution = null,
//                ),
//            ),
//            onContinue = {},
//            onCancel = {},
//        )
//    }
//}
