package com.stripe.android.utils.screenshots

import android.graphics.Color
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.androidHome
import app.cash.paparazzi.detectEnvironment
import com.android.ide.common.rendering.api.SessionParams
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.parseAppearance
import com.stripe.android.ui.core.PaymentsTheme
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.lang.reflect.Field
import java.lang.reflect.Modifier

interface PaparazziConfigOption {

    fun apply(deviceConfig: DeviceConfig): DeviceConfig = deviceConfig

    fun initialize() {
        // Nothing to do
    }
}

enum class SystemAppearance2 : PaparazziConfigOption {
    Light, Dark;
}

enum class FontSize2(val scaleFactor: Float) : PaparazziConfigOption {
    DefaultFontSize(scaleFactor = 1f),
    LargeFontSize(scaleFactor = 1.2f),
    ExtraLargeFontSize(scaleFactor = 1.5f);

    override fun apply(deviceConfig: DeviceConfig): DeviceConfig {
        return deviceConfig.copy(
            fontScale = scaleFactor,
        )
    }
}

enum class PaymentSheetAppearance2(val appearance: PaymentSheet.Appearance) : PaparazziConfigOption {

    Default(appearance = PaymentSheet.Appearance()),

    Custom(
        appearance = PaymentSheet.Appearance(
            colorsLight = PaymentSheet.Colors.defaultLight.copy(
                primary = Color.RED,
            ),
            colorsDark = PaymentSheet.Colors.defaultDark.copy(
                primary = Color.RED,
            ),
            shapes = PaymentSheet.Shapes.default.copy(
                cornerRadiusDp = 8f,
            ),
        ),
    ),

    Crazy(
        appearance = PaymentSheet.Appearance(
            colorsLight = PaymentSheet.Colors(
                primary = Color.MAGENTA,
                surface = Color.CYAN,
                component = Color.YELLOW,
                componentBorder = Color.RED,
                componentDivider = Color.BLACK,
                onComponent = Color.BLUE,
                onSurface = Color.GRAY,
                subtitle = Color.WHITE,
                placeholderText = Color.DKGRAY,
                appBarIcon = Color.GREEN,
                error = Color.GREEN,
            ),
            colorsDark = PaymentSheet.Colors(
                primary = Color.MAGENTA,
                surface = Color.CYAN,
                component = Color.YELLOW,
                componentBorder = Color.RED,
                componentDivider = Color.BLACK,
                onComponent = Color.BLUE,
                onSurface = Color.GRAY,
                subtitle = Color.WHITE,
                placeholderText = Color.DKGRAY,
                appBarIcon = Color.GREEN,
                error = Color.GREEN,
            ),
            shapes = PaymentSheet.Shapes(
                cornerRadiusDp = 0.0f,
                borderStrokeWidthDp = 4.0f
            ),
            // TODO Move to stripe-test
//            typography = PaymentSheet.Typography.default.copy(
//                sizeScaleFactor = 1.1f,
//                fontResId = R.font.cursive
//            )
        ),
    );

    override fun initialize() {
        appearance.parseAppearance()
    }
}

class PaparazziRule(
    private vararg val configOptions: Array<out PaparazziConfigOption>,
) : TestRule {

    private val defaultDeviceConfig = DeviceConfig.PIXEL_6.copy(
        softButtons = false,
        screenHeight = 1,
    )

    private val paparazzi = createPaparazzi(defaultDeviceConfig)

    private var description: Description? = null

    override fun apply(base: Statement, description: Description): Statement {
        this.description = description
        return paparazzi.apply(base, description)
    }

    fun snapshot(content: @Composable () -> Unit) {
        // This is to close the prepare done as part of the apply.
        // We need symmetric calls to prepare/close.
        paparazzi.close()
        val testCases = PermutationsFactory.create(*configOptions)

        for (testCase in testCases) {
            for (option in testCase) {
                option.initialize()
            }

//            val deviceConfig = testCase.fold(defaultDeviceConfig) { acc, option ->
//                option.apply(acc)
//            }
//
//            if (deviceConfig != defaultDeviceConfig) {
//                paparazzi.unsafeUpdateConfig(deviceConfig)
//            }

            val name = generateName(testCase)

            val isDark = testCase.firstOrNull { it is SystemAppearance2 } == SystemAppearance2.Dark

            paparazzi.prepare(description!!)
            paparazzi.snapshot(name) {
//                CompositionLocalProvider(values = providers) {
                    // TODO: Make sure it's the right theme.
                    PaymentsTheme(
                        colors = PaymentsTheme.getColors(isDark),
                    ) {
                        Surface(color = MaterialTheme.colors.surface) {
                            Box(
                                contentAlignment = Alignment.Center,
                            ) {
                                content()
                            }
                        }
                    }
//                }
            }
            paparazzi.close()
        }
    }

    private fun generateName(values: List<PaparazziConfigOption>): String? {
        val suffix = values.joinToString(separator = "_") { it.toString() }
        return description?.let {
            "${it.className}_${it.methodName}_$suffix"
        }
    }

    private fun createPaparazzi(deviceConfig: DeviceConfig): Paparazzi {
        makePaparazziWorkForApi33()

        return Paparazzi(
            deviceConfig = deviceConfig,
            renderingMode = SessionParams.RenderingMode.V_SCROLL,
            environment = detectEnvironment().copy(
                platformDir = "${androidHome()}/platforms/android-32",
                compileSdkVersion = 32,
            ),
        )
    }

    private fun makePaparazziWorkForApi33() {
        val field = Build.VERSION::class.java.getField("CODENAME")
        val newValue = "REL"

        Field::class.java.getDeclaredField("modifiers").apply {
            isAccessible = true
            setInt(field, field.modifiers and Modifier.FINAL.inv())
        }

        field.apply {
            isAccessible = true
            set(null, newValue)
        }
    }
}
