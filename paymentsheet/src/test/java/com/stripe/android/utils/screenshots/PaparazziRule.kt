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
    vararg configOptions: Array<out PaparazziConfigOption>,
) : TestRule {

    private val testCases: List<TestCase> = configOptions.toTestCases()

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

        val description = requireNotNull(description) {
            "Description in PaparazziRule can't be null"
        }

        for (testCase in testCases) {
            testCase.initialize()

            // TODO: Comment
            val newDescription = Description.createTestDescription(
                description.className,
                description.methodName + testCase.name,
            )

            paparazzi.prepare(newDescription)

            val deviceConfig = testCase.apply(defaultDeviceConfig)
            if (deviceConfig != defaultDeviceConfig) {
                paparazzi.unsafeUpdateConfig(deviceConfig)
            }

            paparazzi.snapshot {
                PaymentsTheme(colors = PaymentsTheme.getColors(testCase.isDarkTheme)) {
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

    private fun Array<out Array<out PaparazziConfigOption>>.toTestCases(): List<TestCase> {
        return createPermutations(this).map { TestCase(it) }
    }

    private fun createPermutations(
        options: Array<out Array<out PaparazziConfigOption>>,
    ): List<List<PaparazziConfigOption>> {
        @Suppress("UNCHECKED_CAST")
        return (options.toSet()).fold(listOf(listOf())) { acc, set ->
            acc.flatMap { list -> set.map { element -> list + element } }
        }
    }
}

private data class TestCase(
    val configOptions: List<PaparazziConfigOption>,
) {

    val isDarkTheme: Boolean
        get() = configOptions.find { it is SystemAppearance } == SystemAppearance.DarkTheme

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
