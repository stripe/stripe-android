package com.stripe.android.common.taptoadd

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.stripe.android.common.taptoadd.ui.TapToAddLayout
import com.stripe.android.common.taptoadd.ui.TapToAddNavigator
import com.stripe.android.common.taptoadd.ui.TapToAddTheme
import com.stripe.android.paymentsheet.ui.isDarkTheme
import com.stripe.android.uicore.isSystemDarkTheme
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
        TapToAddViewModel.Factory(
            argSupplier = { requireNotNull(args) },
            isSystemDarkSupplier = { isSystemDarkTheme() },
        )
    }

    @Inject
    lateinit var tapToAddNavigator: TapToAddNavigator

    @Inject
    lateinit var tapToAddRegistrar: TapToAddRegistrar

    @Inject
    lateinit var tapToAddImageRepository: TapToAddImageRepository

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

        val appearance = tapToAddArguments.paymentMethodMetadata.appearance
        configureSystemBars(
            isDark = appearance.themeMode.isDarkTheme(isSystemDarkTheme()),
        )

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

            TapToAddTheme(
                appearance = appearance,
                imageRepository = tapToAddImageRepository,
            ) {
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

    private fun configureSystemBars(isDark: Boolean) {
        val systemBarStyle = if (isDark) {
            SystemBarStyle.dark(
                scrim = Color.TRANSPARENT,
            )
        } else {
            SystemBarStyle.light(
                scrim = Color.TRANSPARENT,
                darkScrim = Color.TRANSPARENT,
            )
        }
        enableEdgeToEdge(
            statusBarStyle = systemBarStyle,
            navigationBarStyle = systemBarStyle,
        )
    }

    override fun finish() {
        super.finish()
        fadeOut()
    }
}
