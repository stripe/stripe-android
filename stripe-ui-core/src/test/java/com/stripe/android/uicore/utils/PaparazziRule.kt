package com.stripe.android.uicore.utils

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.detectEnvironment
import com.android.ide.common.rendering.api.SessionParams
import com.stripe.android.uicore.StripeTheme
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class PaparazziRule(
    vararg configOptions: Array<out PaparazziConfigOption>,
    private val boxModifier: Modifier = Modifier.defaultBoxModifier(),
) : TestRule {

    private val testCases: List<TestCase> = configOptions.toTestCases()

    private val defaultDeviceConfig = createPaparazziDeviceConfig()
    private val paparazzi = createPaparazzi(defaultDeviceConfig)

    private var description: Description? = null

    override fun apply(base: Statement, description: Description): Statement {
        this.description = description
        return object : Statement() {
            override fun evaluate() {
                base.evaluate()
            }
        }
    }

    fun snapshot(
        content: @Composable () -> Unit,
    ) {
        val description = requireNotNull(description) {
            "Description in PaparazziRule can't be null"
        }

        for (testCase in testCases) {
            testCase.initialize()

            // We need to update the entire Description to prevent Paparazzi from converting the
            // name to lowercase.
            val newDescription = Description.createTestDescription(
                description.className,
                description.methodName + testCase.name,
            )

            paparazzi.prepare(newDescription)

            try {
                val deviceConfig = testCase.apply(defaultDeviceConfig)
                if (deviceConfig != defaultDeviceConfig) {
                    paparazzi.unsafeUpdateConfig(deviceConfig)
                }

                paparazzi.snapshot {
                    StripeTheme {
                        Surface(color = MaterialTheme.colors.surface) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = boxModifier,
                            ) {
                                content()
                            }
                        }
                    }
                }
            } finally {
                paparazzi.close()
            }
        }
    }

    private fun createPaparazziDeviceConfig(): DeviceConfig {
        return DeviceConfig.PIXEL_6.copy(softButtons = false)
    }

    private fun createPaparazzi(deviceConfig: DeviceConfig): Paparazzi {
        return Paparazzi(
            deviceConfig = deviceConfig,
            // Needed to shrink the screenshot to the height of the composable
            renderingMode = SessionParams.RenderingMode.SHRINK,
            showSystemUi = false,
            environment = detectEnvironment().run {
                copy(compileSdkVersion = 33, platformDir = platformDir.replace("34", "33"))
            },
        )
    }
}

private fun Modifier.defaultBoxModifier(): Modifier {
    return padding(PaddingValues(vertical = 16.dp))
        .fillMaxWidth()
}

private fun Array<out Array<out PaparazziConfigOption>>.toTestCases(): List<TestCase> {
    return createPermutations(this).map { TestCase(it) }
}

private fun createPermutations(
    options: Array<out Array<out PaparazziConfigOption>>,
): List<List<PaparazziConfigOption>> {
    return (options.toSet()).fold(listOf(listOf())) { acc, set ->
        acc.flatMap { list -> set.map { element -> list + element } }
    }
}

private data class TestCase(
    val configOptions: List<PaparazziConfigOption>,
) {

    val name: String
        get() {
            val optionsSuffix = configOptions.joinToString(separator = ",") { it.toString() }
            return "[$optionsSuffix]"
        }

    fun initialize() {
        configOptions.forEach { it.initialize() }
    }

    fun apply(deviceConfig: DeviceConfig): DeviceConfig {
        return configOptions.fold(deviceConfig) { acc, option ->
            option.apply(acc)
        }
    }
}
