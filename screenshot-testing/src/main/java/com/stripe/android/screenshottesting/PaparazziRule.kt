package com.stripe.android.screenshottesting

import androidx.compose.foundation.layout.Box
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.detectEnvironment
import com.android.ide.common.rendering.api.RenderSession
import com.android.ide.common.rendering.api.SessionParams
import com.stripe.android.uicore.StripeTheme
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

class PaparazziRule(
    vararg configOptions: List<PaparazziConfigOption>,
    private val boxModifier: Modifier = Modifier,
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

        var errorResult: Throwable? = null

        for (testCase in testCases) {
            testCase.initialize()

            // We need to update the entire Description to prevent Paparazzi from converting the
            // name to lowercase.
            val newDescription = Description.createTestDescription(
                description.className,
                description.methodName + testCase.name,
            )

            try {
                paparazzi.apply(
                    base = object : Statement() {
                        override fun evaluate() {
                            val deviceConfig = testCase.apply(defaultDeviceConfig)
                            if (deviceConfig != defaultDeviceConfig) {
                                paparazzi.unsafeUpdateConfig(deviceConfig)
                            }

                            paparazzi.snapshot {
                                CompositionLocalProvider(LocalInspectionMode provides true) {
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
                            }
                        }
                    },
                    description = newDescription,
                ).evaluate()
            } catch (t: Throwable) {
                if (errorResult == null) {
                    errorResult = t
                }
            } finally {
                testCase.reset()
            }
        }

        dispose()

        if (errorResult != null) {
            // We want to generate all scenarios. So don't stop running the scenarios until all are complete.
            throw errorResult
        }
    }

    private fun createPaparazziDeviceConfig(): DeviceConfig {
        return DeviceConfig.PIXEL_6.copy(softButtons = false)
    }

    private fun createPaparazzi(deviceConfig: DeviceConfig): Paparazzi {
        return Paparazzi(
            maxPercentDifference = 0.001,
            deviceConfig = deviceConfig,
            // Needed to shrink the screenshot to the height of the composable
            renderingMode = SessionParams.RenderingMode.SHRINK,
            showSystemUi = false,
            environment = detectEnvironment().run {
                copy(compileSdkVersion = 33, platformDir = platformDir.replace("34", "33"))
            },
            theme = "Theme.MaterialComponents",
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun dispose() {
        val renderSession: RenderSession = (
            paparazzi::class.memberProperties
                .first { it.name == "bridgeRenderSession" } as KProperty1<Paparazzi, RenderSession>
            ).apply { isAccessible = true }.invoke(paparazzi)

        renderSession.disposeWithCompose()
    }
}

private fun Array<out List<PaparazziConfigOption>>.toTestCases(): List<TestCase> {
    if (isEmpty()) {
        // Use all defaults, but still run the test.
        return listOf(TestCase(emptyList()))
    }
    return createPermutations(this).map { TestCase(it) }
}

private fun createPermutations(
    options: Array<out List<PaparazziConfigOption>>,
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

    fun reset() {
        configOptions.forEach { it.reset() }
    }

    fun apply(deviceConfig: DeviceConfig): DeviceConfig {
        return configOptions.fold(deviceConfig) { acc, option ->
            option.apply(acc)
        }
    }
}
