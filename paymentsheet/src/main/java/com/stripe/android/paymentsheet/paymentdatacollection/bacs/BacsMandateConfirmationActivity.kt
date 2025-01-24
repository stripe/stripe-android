package com.stripe.android.paymentsheet.paymentdatacollection.bacs

import android.app.Activity
import android.os.Build
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.LaunchedEffect
import androidx.core.view.WindowCompat
import com.stripe.android.common.ui.BottomSheetScaffold
import com.stripe.android.common.ui.ElementsBottomSheetLayout
import com.stripe.android.paymentsheet.parseAppearance
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationViewAction.OnBackPressed
import com.stripe.android.paymentsheet.ui.PaymentSheetTopBar
import com.stripe.android.paymentsheet.ui.PaymentSheetTopBarState
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.elements.bottomsheet.rememberStripeBottomSheetState
import com.stripe.android.uicore.utils.fadeOut
import kotlinx.coroutines.flow.collectLatest

internal class BacsMandateConfirmationActivity : AppCompatActivity() {
    private val starterArgs: BacsMandateConfirmationContract.Args by lazy {
        BacsMandateConfirmationContract.Args.fromIntent(intent) ?: throw IllegalStateException(
            "Cannot start Bacs mandate confirmation flow without arguments"
        )
    }

    private val viewModel by viewModels<BacsMandateConfirmationViewModel> {
        BacsMandateConfirmationViewModel.Factory(starterArgs)
    }

    @OptIn(ExperimentalMaterialApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        renderEdgeToEdge()

        onBackPressedDispatcher.addCallback {
            viewModel.handleViewAction(OnBackPressed)
        }

        starterArgs.appearance.parseAppearance()

        setContent {
            StripeTheme {
                val bottomSheetState = rememberStripeBottomSheetState()

                LaunchedEffect(bottomSheetState) {
                    viewModel.result.collectLatest { result ->
                        setResult(
                            Activity.RESULT_OK,
                            BacsMandateConfirmationResult.toIntent(intent, result)
                        )
                        bottomSheetState.hide()
                        finish()
                    }
                }

                ElementsBottomSheetLayout(
                    state = bottomSheetState,
                    onDismissed = { viewModel.handleViewAction(OnBackPressed) },
                ) {
                    BottomSheetScaffold(
                        topBar = {
                            PaymentSheetTopBar(
                                state = PaymentSheetTopBarState(
                                    showEditMenu = false,
                                    showTestModeLabel = false,
                                    isEditing = false,
                                    onEditIconPressed = {},
                                ),
                                canNavigateBack = false,
                                isEnabled = true,
                                handleBackPressed = {
                                    viewModel.handleViewAction(OnBackPressed)
                                },
                            )
                        },
                        content = {
                            BacsMandateConfirmationFormScreen(viewModel)
                        }
                    )
                }
            }
        }
    }

    override fun finish() {
        super.finish()
        fadeOut()
    }

    private fun renderEdgeToEdge() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
    }
}
