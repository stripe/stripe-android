package com.stripe.android.paymentsheet.benchmark

import android.content.Intent
import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream

@LargeTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMetricApi::class)
internal class FirstPresentationBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun coldWithBaselineProfile() = measure(CompilationMode.Partial(BaselineProfileMode.Require))

    @Test
    fun coldWithoutCompilation() = measure(CompilationMode.None())

    private companion object {
        const val LOAD_TIMEOUT_MILLIS = 10_000L
    }

    private fun measure(compilationMode: CompilationMode) {
        benchmarkRule.measureRepeated(
            packageName = "com.stripe.android.paymentsheet.benchmarkapp",
            metrics = listOf(
                FrameTimingMetric(),
                FirstLoadedFrameMetric(),
            ),
            compilationMode = compilationMode,
            startupMode = StartupMode.COLD,
            iterations = 10,
            setupBlock = { pressHome() },
        ) {
            val workload = InstrumentationRegistry.getArguments().getString("paymentsheet.workload") ?: "full"
            check(workload in setOf("full", "hidden", "captcha")) { "Unknown workload: $workload" }
            startActivityAndWait(
                Intent().setClassName(
                    "com.stripe.android.paymentsheet.benchmarkapp",
                    "com.stripe.android.paymentsheet.benchmarkapp.BenchmarkActivity",
                ).putExtra("workload", workload)
            )
            val loadedForm = By.text("Card information").pkg("com.stripe.android.paymentsheet.benchmarkapp")
            check(device.wait(Until.hasObject(loadedForm), LOAD_TIMEOUT_MILLIS)) {
                val hierarchy = ByteArrayOutputStream().also { device.dumpWindowHierarchy(it) }
                "Loaded PaymentSheet did not appear. Window hierarchy: $hierarchy"
            }
            device.waitForIdle()
        }
    }
}
