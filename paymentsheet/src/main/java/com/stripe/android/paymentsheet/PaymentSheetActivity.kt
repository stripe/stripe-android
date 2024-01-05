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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.stripe.android.common.ui.BottomSheet
import com.stripe.android.common.ui.rememberBottomSheetState
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherContractV2
import com.stripe.android.paymentsheet.ui.BaseSheetActivity
import com.stripe.android.paymentsheet.ui.PaymentSheetFlowType.Complete
import com.stripe.android.paymentsheet.ui.PaymentSheetScreen
import com.stripe.android.uicore.StripeTheme
import kotlinx.coroutines.flow.filterNotNull
import java.security.InvalidParameterException

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

        setContent {
            StripeTheme {
                val isProcessing by viewModel.processing.collectAsState()

                val bottomSheetState = rememberBottomSheetState(
                    confirmValueChange = { !isProcessing },
                )

                LaunchedEffect(Unit) {
                    viewModel.paymentSheetResult.filterNotNull().collect { sheetResult ->
                        setActivityResult(sheetResult)
                        bottomSheetState.hide()
                        finish()
                    }
                }

                BottomSheet(
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
            } catch (e: InvalidParameterException) {
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
