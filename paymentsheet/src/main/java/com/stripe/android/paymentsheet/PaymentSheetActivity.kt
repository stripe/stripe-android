package com.stripe.android.paymentsheet

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.stripe.android.common.ui.ElementsBottomSheetLayout
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherContractV2
import com.stripe.android.paymentsheet.ui.BaseSheetActivity
import com.stripe.android.paymentsheet.ui.PaymentSheetFlowType.Complete
import com.stripe.android.paymentsheet.ui.PaymentSheetScreen
import com.stripe.android.paymentsheet.utils.applicationIsTaskOwner
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.elements.bottomsheet.rememberStripeBottomSheetState
import kotlinx.coroutines.flow.filterNotNull

internal class PaymentSheetActivity : BaseSheetActivity<PaymentSheetResult>() {

    @VisibleForTesting
    internal var viewModelFactory: ViewModelProvider.Factory = PaymentSheetViewModel.Factory {
        requireNotNull(starterArgs)
    }

    override val viewModel: PaymentSheetViewModel by viewModels { viewModelFactory }

    private val starterArgs: PaymentSheetContractV2.Args? by lazy {
        PaymentSheetContractV2.Args.fromIntent(intent)
    }

    @OptIn(ExperimentalMaterialApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val validationResult = initializeArgs()
        super.onCreate(savedInstanceState)

        val validatedArgs = validationResult.getOrNull()
        if (validatedArgs == null) {
            finishWithError(error = validationResult.exceptionOrNull())
            return
        }

        viewModel.registerFromActivity(
            activityResultCaller = this,
            lifecycleOwner = this,
        )

        viewModel.setupGooglePay(
            lifecycleScope,
            registerForActivityResult(
                GooglePayPaymentMethodLauncherContractV2(),
                viewModel::onGooglePayResult
            )
        )

        if (!applicationIsTaskOwner()) {
            viewModel.cannotProperlyReturnFromLinkAndOtherLPMs()
        }

        setContent {
            StripeTheme {
                val systemUiController = rememberSystemUiController()
                val isProcessing by viewModel.processing.collectAsState()

                val bottomSheetState = rememberStripeBottomSheetState(
                    confirmValueChange = { !isProcessing },
                )

                LaunchedEffect(systemUiController) {
                    systemUiController.setNavigationBarColor(
                        color = Color.Transparent,
                        darkIcons = false,
                    )
                }

                LaunchedEffect(Unit) {
                    viewModel.paymentSheetResult.filterNotNull().collect { sheetResult ->
                        setActivityResult(sheetResult)
                        bottomSheetState.hide()
                        finish()
                    }
                }

                ElementsBottomSheetLayout(
                    state = bottomSheetState,
                    onDismissed = viewModel::onUserCancel,
                ) {
                    PaymentSheetScreen(viewModel, type = Complete)
                }
            }
        }
    }

    private fun initializeArgs(): Result<PaymentSheetContractV2.Args?> {
        val starterArgs = this.starterArgs

        val result = if (starterArgs == null) {
            Result.failure(defaultInitializationError())
        } else {
            try {
                starterArgs.initializationMode.validate()
                starterArgs.config.validate()
                starterArgs.config.appearance.parseAppearance()
                Result.success(starterArgs)
            } catch (e: IllegalArgumentException) {
                Result.failure(e)
            }
        }

        earlyExitDueToIllegalState = result.isFailure
        return result
    }

    override fun setActivityResult(result: PaymentSheetResult) {
        setResult(
            Activity.RESULT_OK,
            Intent().putExtras(PaymentSheetContractV2.Result(result).toBundle())
        )
    }

    private fun finishWithError(error: Throwable?) {
        val e = error ?: defaultInitializationError()
        setActivityResult(PaymentSheetResult.Failed(e))
        finish()
    }

    private fun defaultInitializationError(): IllegalArgumentException {
        return IllegalArgumentException("PaymentSheet started without arguments.")
    }
}
