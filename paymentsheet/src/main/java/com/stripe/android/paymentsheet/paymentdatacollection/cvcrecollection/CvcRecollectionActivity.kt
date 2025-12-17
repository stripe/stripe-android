package com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection

import android.app.Activity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
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

        // Check if required args are present, finish gracefully if not
        if (!hasRequiredArgs()) {
            finish()
            return
        }

        args.appearance.parseAppearance()
        setContent {
            StripeTheme {
                val bottomSheetState = rememberStripeBottomSheetState()
                val state by viewModel.viewState.collectAsState()

                val lifecycleOwner = LocalLifecycleOwner.current
                LaunchedEffect(bottomSheetState, lifecycleOwner) {
                    lifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                        viewModel.result.collectLatest { result ->
                            setResult(
                                Activity.RESULT_OK,
                                CvcRecollectionResult.toIntent(intent, result)
                            )
                            bottomSheetState.hide()
                            finish()
                        }
                    }
                }

                ElementsBottomSheetLayout(
                    state = bottomSheetState,
                    onDismissed = {
                        viewModel.handleViewAction(CvcRecollectionViewAction.OnBackPressed)
                    },
                ) {
                    CvcRecollectionScreen(
                        lastFour = state.lastFour,
                        isTestMode = state.isTestMode,
                        cvcState = state.cvcState,
                        viewActionHandler = viewModel::handleViewAction
                    )
                }
            }
        }
    }

    private fun hasRequiredArgs(): Boolean {
        return CvcRecollectionContract.Args.fromIntent(intent) != null
    }

    override fun finish() {
        super.finish()
        fadeOut()
    }
}
