package com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection

import android.app.Activity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.LaunchedEffect
import com.stripe.android.common.ui.ElementsBottomSheetLayout
import com.stripe.android.paymentsheet.parseAppearance
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.elements.bottomsheet.rememberStripeBottomSheetState
import com.stripe.android.uicore.utils.collectAsState
import com.stripe.android.uicore.utils.fadeOut
import kotlinx.coroutines.flow.collectLatest

internal class CvcRecollectionActivity : AppCompatActivity() {
    private val args: CvcRecollectionContract.Args by lazy {
        CvcRecollectionContract.Args.fromIntent(intent) ?: throw IllegalStateException(
            "Cannot start CVC Recollection flow without args"
        )
    }

    private val viewModel by viewModels<CvcRecollectionViewModel> {
        CvcRecollectionViewModel.Factory(args)
    }

    @OptIn(ExperimentalMaterialApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        args.appearance.parseAppearance()
        setContent {
            StripeTheme {
                val bottomSheetState = rememberStripeBottomSheetState()
                val state = viewModel.viewState.collectAsState()

                LaunchedEffect(bottomSheetState) {
                    viewModel.result.collectLatest { result ->
                        setResult(
                            Activity.RESULT_OK,
                            CvcRecollectionResult.toIntent(intent, result)
                        )
                        bottomSheetState.hide()
                        finish()
                    }
                }

                ElementsBottomSheetLayout(
                    state = bottomSheetState,
                    onDismissed = {
                        viewModel.handleViewAction(CvcRecollectionViewAction.OnBackPressed)
                    },
                ) {
                    CvcRecollectionScreen(
                        lastFour = state.value.lastFour,
                        isTestMode = state.value.isTestMode,
                        cvcState = state.value.cvcState,
                        viewActionHandler = viewModel::handleViewAction
                    )
                }
            }
        }
    }

    override fun finish() {
        super.finish()
        fadeOut()
    }
}
