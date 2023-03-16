package com.stripe.android.financialconnections

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.mvrx.Mavericks
import com.airbnb.mvrx.MavericksView
import com.airbnb.mvrx.withState
import com.stripe.android.financialconnections.FinancialConnectionsSheetViewEffect.FinishWithResult
import com.stripe.android.financialconnections.FinancialConnectionsSheetViewEffect.OpenAuthFlowWithUrl
import com.stripe.android.financialconnections.FinancialConnectionsSheetViewEffect.OpenNativeAuthFlow
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetNativeActivityArgs
import com.stripe.android.financialconnections.presentation.CreateBrowserIntentForUrl
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import com.stripe.android.financialconnections.utils.argsOrNull
import com.stripe.android.financialconnections.utils.viewModelLazy

internal class FinancialConnectionsSheetActivity :
    AppCompatActivity(R.layout.activity_financialconnections_sheet), MavericksView {

    val viewModel: FinancialConnectionsSheetViewModel by viewModelLazy()

    val args by argsOrNull<FinancialConnectionsSheetActivityArgs>()

    private val startBrowserForResult = registerForActivityResult(StartActivityForResult()) {
        viewModel.onBrowserActivityResult()
    }

    private val startNativeAuthFlowForResult = registerForActivityResult(StartActivityForResult()) {
        viewModel.onNativeAuthFlowResult(it)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (args == null) {
            finish()
        } else {
            viewModel.onEach { postInvalidate() }
            if (savedInstanceState != null) viewModel.onActivityRecreated()
        }

        onBackPressedDispatcher.addCallback {
            finishWithResult(FinancialConnectionsSheetActivityResult.Canceled)
        }
        animateLoadingSpinner()
    }

    private fun animateLoadingSpinner() {
        findViewById<ImageView>(R.id.spinner).startAnimation(
            AnimationUtils.loadAnimation(
                this,
                R.anim.anim_loading_spinner_rotation
            )
        )
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    /**
     * Handles new intents in the form of the redirect from the custom tab hosted auth flow
     */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        viewModel.handleOnNewIntent(intent)
    }

    /**
     * handle state changes here.
     */
    override fun invalidate() {
        withState(viewModel) { state ->
            state.viewEffect?.let { viewEffect ->
                when (viewEffect) {
                    is OpenAuthFlowWithUrl -> startBrowserForResult.launch(
                        CreateBrowserIntentForUrl(
                            context = this,
                            uri = Uri.parse(viewEffect.url)
                        )
                    )

                    is FinishWithResult -> finishWithResult(
                        viewEffect.result
                    )

                    is OpenNativeAuthFlow -> startNativeAuthFlowForResult.launch(
                        Intent(
                            this,
                            FinancialConnectionsSheetNativeActivity::class.java
                        ).also {
                            it.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                            it.putExtra(
                                Mavericks.KEY_ARG,
                                FinancialConnectionsSheetNativeActivityArgs(
                                    initialSyncResponse = viewEffect.initialSyncResponse,
                                    configuration = viewEffect.configuration
                                )
                            )
                        }
                    )
                }
                viewModel.onViewEffectLaunched()
            }
        }
    }

    private fun finishWithResult(result: FinancialConnectionsSheetActivityResult) {
        setResult(RESULT_OK, Intent().putExtras(result.toBundle()))
        finish()
    }
}
