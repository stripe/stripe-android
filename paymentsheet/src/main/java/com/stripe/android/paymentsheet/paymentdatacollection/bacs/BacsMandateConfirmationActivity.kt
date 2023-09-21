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
import com.stripe.android.utils.AnimationConstants
import kotlinx.coroutines.flow.collectLatest

internal class BacsMandateConfirmationActivity : AppCompatActivity() {
    private val starterArgs: BacsMandateConfirmationContract.Args? by lazy {
        BacsMandateConfirmationContract.Args.fromIntent(intent)
    }

    private val viewModel by viewModels<BacsMandateConfirmationViewModel> {
        val args = starterArgs ?: throw IllegalStateException(
            "Cannot start Bacs mandate confirmation flow with arguments"
        )

        BacsMandateConfirmationViewModel.Factory(application, args)
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
            viewModel.handleViewAction(BacsMandateConfirmationViewAction.OnCancelPressed)
        }

        setContent {
            BacsMandateConfirmationTheme {
                val bottomSheetState = rememberBottomSheetState()

                LaunchedEffect(bottomSheetState) {
                    viewModel.effect.collectLatest { effect ->
                        when (effect) {
                            is BacsMandateConfirmationEffect.CloseWithResult -> {
                                setResult(
                                    Activity.RESULT_OK,
                                    BacsMandateConfirmationResult.toIntent(intent, effect.result)
                                )
                                bottomSheetState.hide()
                                finish()
                            }
                        }
                    }
                }

                BottomSheet(
                    state = bottomSheetState,
                    onDismissed = {
                        viewModel.handleViewAction(BacsMandateConfirmationViewAction.OnCancelPressed)
                    }
                ) {
                    BacsMandateConfirmationFormScreen(viewModel)
                }
            }
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(AnimationConstants.FADE_IN, AnimationConstants.FADE_OUT)
    }

    private fun renderEdgeToEdge() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
    }
}
