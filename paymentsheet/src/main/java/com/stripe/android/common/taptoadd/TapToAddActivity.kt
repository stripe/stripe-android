package com.stripe.android.common.taptoadd

import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.common.ui.ElementsBottomSheetLayout
import com.stripe.android.paymentelement.embedded.manage.ManageNavigator
import com.stripe.android.paymentsheet.utils.renderEdgeToEdge
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.elements.bottomsheet.rememberStripeBottomSheetState
import com.stripe.android.uicore.utils.collectAsState
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

        if (args == null) {
            finish()
            return
        }

        renderEdgeToEdge()

        viewModel.component.subcomponentFactory.build(
            activityResultCaller = this,
            lifecycleOwner = this,
        ).inject(this)

        onBackPressedDispatcher.addCallback {
            if (!tapToAddFlowManager.screen.value.isPerformingNetworkOperation()) {
                tapToAddFlowManager.performAction(ManageNavigator.Action.Back)
            }
        }

        setContent {
            StripeTheme {
                val screen by tapToAddFlowManager.screen.collectAsState()
                AnimatedContent(
                    targetState = screen,
                ) {
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        screen
                    }

                    var hasResult by remember { mutableStateOf(false) }
                    if (!hasResult) {
                        Box(modifier = Modifier.padding(bottom = 20.dp)) {
                            ScreenContent(manageNavigator, screen)
                        }
                        LaunchedEffect(screen) {
                            manageNavigator.result.collect { result ->
                                setManageResult(result == true)
                                finish()
                                hasResult = true
                            }
                        }
                    }
                }
            }
        }
    }
}
