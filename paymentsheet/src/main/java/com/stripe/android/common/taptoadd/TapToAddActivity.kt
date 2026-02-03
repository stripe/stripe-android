package com.stripe.android.common.taptoadd

import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.stripe.android.paymentsheet.utils.renderEdgeToEdge
import com.stripe.android.uicore.utils.collectAsState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.getValue

internal class TapToAddActivity : AppCompatActivity() {
    private val args: TapToAddContract.Args? by lazy {
        TapToAddContract.Args.fromIntent(intent)
    }

    private val viewModel: TapToAddViewModel by viewModels {
        TapToAddViewModel.Factory {
            requireNotNull(args)
        }
    }

    @Inject
    lateinit var tapToAddRegistrar: TapToAddRegistrar

    @Inject
    lateinit var tapToAddFlowManager: TapToAddFlowManager

    @OptIn(ExperimentalMaterialApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (args == null || args?.paymentMethodMetadata?.isTapToAddSupported != true) {
            finish()
            return
        }

        renderEdgeToEdge()

        viewModel.component.subcomponentFactory.build(
            activityResultCaller = this,
            lifecycleOwner = this,
        ).inject(this)

        onBackPressedDispatcher.addCallback {
            if (!tapToAddFlowManager.screen.value.isPerformingNetworkOperation) {
                tapToAddFlowManager.action(TapToAddFlowManager.Action.Close)
            }
        }

        lifecycleScope.launch {
            tapToAddFlowManager.result.collectLatest {
                setResult(it)
                finish()
            }
        }

        setContent {
            TapToAddTheme {
                val screen by tapToAddFlowManager.screen.collectAsState()

                TapToAddLayout(
                    screen = screen,
                    onCancel = {
                        tapToAddFlowManager.action(TapToAddFlowManager.Action.Close)
                    }
                )
            }
        }
    }

    private fun setResult(result: TapToAddResult) {
        setResult(
            RESULT_OK,
            TapToAddResult.toIntent(intent, result)
        )
    }
}
