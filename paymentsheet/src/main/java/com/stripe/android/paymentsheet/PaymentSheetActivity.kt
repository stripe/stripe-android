package com.stripe.android.paymentsheet

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresPermission
import androidx.annotation.VisibleForTesting
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.common.ui.ElementsBottomSheetLayout
import com.stripe.android.paymentsheet.ui.BaseSheetActivity
import com.stripe.android.paymentsheet.ui.PaymentSheetScreen
import com.stripe.android.paymentsheet.utils.applicationIsTaskOwner
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.elements.bottomsheet.rememberStripeBottomSheetState
import com.stripe.android.uicore.utils.collectAsState
import com.stripe.stripeterminal.Terminal
import com.stripe.stripeterminal.external.callable.Callback
import com.stripe.stripeterminal.external.callable.Cancelable
import com.stripe.stripeterminal.external.callable.ConnectionTokenCallback
import com.stripe.stripeterminal.external.callable.ConnectionTokenProvider
import com.stripe.stripeterminal.external.callable.DiscoveryListener
import com.stripe.stripeterminal.external.callable.TapToPayReaderListener
import com.stripe.stripeterminal.external.callable.TerminalListener
import com.stripe.stripeterminal.external.models.DiscoveryConfiguration
import com.stripe.stripeterminal.external.models.Reader
import com.stripe.stripeterminal.external.models.TerminalException
import kotlinx.coroutines.flow.filterNotNull

internal class PaymentSheetActivity : BaseSheetActivity<PaymentSheetResult>(), TapToPayReaderListener {

    @VisibleForTesting
    internal var viewModelFactory: ViewModelProvider.Factory = PaymentSheetViewModel.Factory {
        requireNotNull(starterArgs)
    }

    override val viewModel: PaymentSheetViewModel by viewModels { viewModelFactory }

    private val starterArgs: PaymentSheetContractV2.Args? by lazy {
        PaymentSheetContractV2.Args.fromIntent(intent)
    }

    private var discoverCancelable: Cancelable? = null

    @OptIn(ExperimentalMaterialApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val validationResult = initializeArgs()
        super.onCreate(savedInstanceState)

        val validatedArgs = validationResult.getOrNull()
        if (validatedArgs == null) {
            finishWithError(error = validationResult.exceptionOrNull())
            return
        }

        if (ENABLE) {
            Terminal.initTerminal(
                context = applicationContext,
                tokenProvider = object : ConnectionTokenProvider {
                    override fun fetchConnectionToken(callback: ConnectionTokenCallback) {
                        throw Exception()
                    }
                },
                listener = object : TerminalListener {

                }
            )

            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_LOCATION)
            } else {
                onDiscoverReaders()
            }
        }

        viewModel.registerFromActivity(
            activityResultCaller = this,
            lifecycleOwner = this,
        )

        if (!applicationIsTaskOwner()) {
            viewModel.analyticsListener.cannotProperlyReturnFromLinkAndOtherLPMs()
        }

        setContent {
            StripeTheme {
                val isProcessing by viewModel.processing.collectAsState()

                val bottomSheetState = rememberStripeBottomSheetState(
                    confirmValueChange = { !isProcessing },
                )

                LaunchedEffect(Unit) {
                    viewModel.paymentSheetResult.filterNotNull().collect { sheetResult ->
                        setActivityResult(sheetResult)
                        bottomSheetState.hide()
                        viewModel.navigationHandler.closeScreens()
                        finish()
                    }
                }

                ElementsBottomSheetLayout(
                    state = bottomSheetState,
                    onDismissed = viewModel::onUserCancel,
                ) {
                    PaymentSheetScreen(viewModel)
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_LOCATION && grantResults.isNotEmpty()
            && grantResults[0] != PackageManager.PERMISSION_GRANTED
        ) {
            throw RuntimeException("Location services are required in order to connect to a reader.")
        }
    }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun onDiscoverReaders() {
        val isApplicationDebuggable = 0 != applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE
        val config = DiscoveryConfiguration.TapToPayDiscoveryConfiguration(isSimulated = isApplicationDebuggable)

        // Save this cancelable to an instance variable
        discoverCancelable = Terminal.getInstance().discoverReaders(
            config,
            object : DiscoveryListener {
                override fun onUpdateDiscoveredReaders(readers: List<Reader>) {
                    // Automatically connect to supported mobile readers
                }
            },
            object : Callback {
                override fun onSuccess() {
                    // Placeholder for handling successful operation
                }

                override fun onFailure(e: TerminalException) {
                    // Placeholder for handling exception
                }
            }
        )
    }

    private fun initializeArgs(): Result<PaymentSheetContractV2.Args?> {
        val starterArgs = this.starterArgs

        val result = if (starterArgs == null) {
            Result.failure(defaultInitializationError())
        } else {
            try {
                starterArgs.initializationMode.validate()
                starterArgs.config.asCommonConfiguration().validate()
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

    private companion object {
        const val REQUEST_CODE_LOCATION = 1000
        const val ENABLE = false
    }
}
