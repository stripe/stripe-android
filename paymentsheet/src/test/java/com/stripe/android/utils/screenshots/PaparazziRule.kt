package com.stripe.android.utils.screenshots

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.androidHome
import app.cash.paparazzi.detectEnvironment
import com.android.ide.common.rendering.api.SessionParams
import com.stripe.android.ui.core.PaymentsTheme
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.lang.reflect.Field
import java.lang.reflect.Modifier as ReflectionModifier

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

    fun snapshot(
        padding: PaddingValues = PaddingValues(vertical = 16.dp),
        content: @Composable () -> Unit,
    ) {
        // This is to close the prepare done as part of the apply.
        // We need symmetric calls to prepare/close.
        paparazzi.close()
        val testCases = PermutationsFactory.create(*configOptions)

        for (testCase in testCases) {
            for (option in testCase) {
                option.initialize()
            }

            val deviceConfig = testCase.fold(defaultDeviceConfig) { acc, option ->
                option.apply(acc)
            }

            val name = generateName(testCase)

            val isDark = testCase.firstOrNull { it is SystemAppearance } == SystemAppearance.Dark

            paparazzi.prepare(description!!)

            if (deviceConfig != defaultDeviceConfig) {
                paparazzi.unsafeUpdateConfig(deviceConfig)
            }

            paparazzi.snapshot(name) {
                PaymentsTheme(
                    colors = PaymentsTheme.getColors(isDark),
                ) {
                    Surface(color = MaterialTheme.colors.surface) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(padding),
                        ) {
                            content()
                        }
                    }
                }
            }

            paparazzi.close()
        }
    }

    private fun generateName(options: List<PaparazziConfigOption>): String? {
        val optionsSuffix = options.joinToString(separator = ", ") { option ->
            "${option::class.java.simpleName}=$option"
        }

        return description?.let {
            "${it.className}_${it.methodName}_[$optionsSuffix]"
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
            setInt(field, field.modifiers and ReflectionModifier.FINAL.inv())
        }

        field.apply {
            isAccessible = true
            set(null, newValue)
        }
    }
}
