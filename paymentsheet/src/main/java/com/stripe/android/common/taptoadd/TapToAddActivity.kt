package com.stripe.android.common.taptoadd

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.stripe.android.common.taptoadd.ui.TapToAddLayout
import com.stripe.android.common.taptoadd.ui.TapToAddNavigator
import com.stripe.android.common.taptoadd.ui.TapToAddTheme
import com.stripe.android.uicore.utils.collectAsState
import com.stripe.android.uicore.utils.fadeOut
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

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
    lateinit var tapToAddNavigator: TapToAddNavigator

    @Inject
    lateinit var tapToAddRegistrar: TapToAddRegistrar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tapToAddArguments = args

        if (tapToAddArguments == null || !tapToAddArguments.paymentMethodMetadata.isTapToAddSupported) {
            finish()
            return
        }

        viewModel.component.subcomponentFactory.build(
            activityResultCaller = this,
            lifecycleOwner = this,
        ).inject(this)

        lifecycleScope.launch {
            tapToAddNavigator.result.collectLatest {
                setResult(
                    RESULT_OK,
                    TapToAddResult.toIntent(intent, it)
                )
                finish()
            }
        }

        setContent {
            val view = LocalView.current

            DisposableEffect(view) {
                val insetsController = WindowCompat.getInsetsController(window, view)

                insetsController.hide(WindowInsetsCompat.Type.statusBars())
                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

                onDispose {
                    insetsController.show(WindowInsetsCompat.Type.statusBars())
                }
            }

            TapToAddTheme {
                val systemBarStyle = remember {
                    SystemBarStyle.light(
                        scrim = Color.TRANSPARENT,
                        darkScrim = Color.TRANSPARENT,
                    )
                }

                LaunchedEffect(systemBarStyle) {
                    enableEdgeToEdge(
                        statusBarStyle = systemBarStyle,
                        navigationBarStyle = systemBarStyle,
                    )
                }

                val screen by tapToAddNavigator.screen.collectAsState()

                TapToAddLayout(
                    screen = screen,
                    onCancel = { action ->
                        tapToAddNavigator.performAction(action)
                    }
                )
            }
        }
    }

    override fun finish() {
        super.finish()
        fadeOut()
    }
}
