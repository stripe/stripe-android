package com.stripe.android.paymentsheet.paymentdatacollection.bacs

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.LaunchedEffect
import androidx.core.view.WindowCompat
import com.stripe.android.common.ui.BottomSheet
import com.stripe.android.common.ui.rememberBottomSheetState
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.parseAppearance
import com.stripe.android.paymentsheet.ui.PaymentSheetScaffold
import com.stripe.android.paymentsheet.ui.PaymentSheetTopBar
import com.stripe.android.paymentsheet.ui.PaymentSheetTopBarState
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.utils.fadeOut
import kotlinx.coroutines.flow.collectLatest
import com.stripe.android.R as StripeR
import com.stripe.android.ui.core.R as StripeUiCoreR

internal class BacsMandateConfirmationActivity : AppCompatActivity() {
    private val starterArgs: BacsMandateConfirmationContract.Args by lazy {
        BacsMandateConfirmationContract.Args.fromIntent(intent) ?: throw IllegalStateException(
            "Cannot start Bacs mandate confirmation flow without arguments"
        )
    }

    private val viewModel by viewModels<BacsMandateConfirmationViewModel> {
        BacsMandateConfirmationViewModel.Factory(starterArgs)
    }

    @SuppressLint("SourceLockedOrientationActivity")
    @OptIn(ExperimentalMaterialApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.O) {
            // In Oreo, Activities where `android:windowIsTranslucent=true` can't request
            // orientation. See https://stackoverflow.com/a/50832408/11103900
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        renderEdgeToEdge()

        onBackPressedDispatcher.addCallback {
            viewModel.handleViewAction(BacsMandateConfirmationViewAction.OnBackPressed)
        }

        starterArgs.appearance.parseAppearance()

        setContent {
            StripeTheme {
                val bottomSheetState = rememberBottomSheetState()

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

                BottomSheet(
                    state = bottomSheetState,
                    onDismissed = {
                        viewModel.handleViewAction(BacsMandateConfirmationViewAction.OnBackPressed)
                    }
                ) {
                    PaymentSheetScaffold(
                        topBar = {
                            PaymentSheetTopBar(
                                state = PaymentSheetTopBarState(
                                    icon = R.drawable.stripe_ic_paymentsheet_close,
                                    contentDescription = StripeUiCoreR.string.stripe_back,
                                    isEnabled = true,
                                    showEditMenu = false,
                                    showTestModeLabel = false,
                                    editMenuLabel = StripeR.string.stripe_edit
                                ),
                                handleBackPressed = {
                                    viewModel.handleViewAction(BacsMandateConfirmationViewAction.OnBackPressed)
                                },
                                toggleEditing = {},
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
