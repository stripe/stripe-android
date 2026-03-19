package com.stripe.android.paymentelement.embedded.content

import android.app.Activity
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.stripe.android.common.ui.ElementsBottomSheetLayout
import com.stripe.android.paymentelement.embedded.form.FormResult
import com.stripe.android.paymentsheet.utils.renderEdgeToEdge
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.elements.bottomsheet.rememberStripeBottomSheetState
import com.stripe.android.uicore.utils.collectAsState
import com.stripe.android.uicore.utils.fadeOut

internal class EmbeddedSheetActivity : AppCompatActivity() {
    private val args: EmbeddedSheetArgs? by lazy {
        EmbeddedSheetContract.argsFromIntent(intent)
    }

    private val viewModel: EmbeddedSheetViewModel by viewModels {
        EmbeddedSheetViewModel.Factory {
            requireNotNull(args)
        }
    }

    private val navigator get() = viewModel.navigator

    @OptIn(ExperimentalMaterialApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val currentArgs = args
        if (currentArgs == null) {
            setDefaultCancelResult()
            finish()
            return
        }

        renderEdgeToEdge()

        navigator.registerActivityDeps(this, this)
        onBackPressedDispatcher.addCallback { navigator.handleBack() }

        setContent {
            StripeTheme {
                val screen by navigator.currentScreen.collectAsState()
                val bottomSheetState = rememberStripeBottomSheetState(
                    confirmValueChange = { screen.canDismiss() }
                )

                var hasResult by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    navigator.result.collect { result ->
                        setResult(RESULT_OK, EmbeddedSheetResult.toIntent(intent, result))
                        finish()
                        hasResult = true
                    }
                }

                ElementsBottomSheetLayout(
                    state = bottomSheetState,
                    onDismissed = navigator::onDismissed,
                ) {
                    if (!hasResult) {
                        screen.Content()
                    }
                }
            }
        }
    }

    private fun setDefaultCancelResult() {
        setResult(
            Activity.RESULT_OK,
            EmbeddedSheetResult.toIntent(
                intent,
                EmbeddedSheetResult.Form(FormResult.Cancelled(customerState = null))
            )
        )
    }

    override fun finish() {
        super.finish()
        fadeOut()
    }

    override fun onDestroy() {
        super.onDestroy()

        if (isFinishing && args != null) {
            navigator.onFinishing()
        }
    }
}
