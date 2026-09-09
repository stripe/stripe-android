package com.stripe.android.paymentsheet

import android.os.Bundle
import app.cash.burst.Burst
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentelement.taptoadd.TapToAddTest
import org.junit.Test

internal class BurstAndroidJUnitRunnerTest {
    @Test
    fun rebuildsClassArgumentWithAllBurstClasses() {
        val sourceClass = PaymentSheetTest::class.java
        val generatedClasses = listOf(
            Class.forName("${sourceClass.name}_PaymentConfigurationOnly_Activity"),
            Class.forName("${sourceClass.name}_PaymentConfigurationOnly_Compose"),
        )
        val arguments = Bundle().apply {
            putString(CLASS_ARGUMENT, sourceClass.name)
        }

        BurstAndroidJUnitRunner().rebuildWithAllBurstTests(arguments)

        val testsRegex = requireNotNull(arguments.getString(TESTS_REGEX_ARGUMENT)).toRegex()
        assertThat(arguments.containsKey(CLASS_ARGUMENT)).isFalse()
        assertThat(sourceClass.isAnnotationPresent(Burst::class.java)).isTrue()
        generatedClasses.forEach { generatedClass ->
            assertThat(
                testsRegex.containsMatchIn("${generatedClass.name}#testSuccessfulCardPayment")
            ).isTrue()
            assertThat(generatedClass.isAnnotationPresent(Burst::class.java)).isFalse()
            assertThat(sourceClass.isAssignableFrom(generatedClass)).isTrue()
        }
    }

    @Test
    fun rebuildsMethodArgumentWithAllBurstMethods() {
        val sourceClass = TapToAddTest::class.java
        val sourceMethodName = "successWithCompleteMode"
        val arguments = Bundle().apply {
            putString(CLASS_ARGUMENT, "${sourceClass.name}#$sourceMethodName")
        }

        BurstAndroidJUnitRunner().rebuildWithAllBurstTests(arguments)

        val testsRegex = requireNotNull(arguments.getString(TESTS_REGEX_ARGUMENT)).toRegex()
        val matchedMethods = sourceClass.methods
            .filter { it.isAnnotationPresent(Test::class.java) }
            .map { it.name }
            .filter { testsRegex.containsMatchIn("${sourceClass.name}#$it") }
        assertThat(arguments.containsKey(CLASS_ARGUMENT)).isFalse()
        assertThat(matchedMethods).containsExactly(
            "${sourceMethodName}_PaymentConfigurationOnly_PaymentSheet",
            "${sourceMethodName}_PaymentConfigurationOnly_Embedded",
        )
        assertThat(sourceClass.isAnnotationPresent(Burst::class.java)).isTrue()
    }

    @Test
    fun rebuildsMultipleClassSelection() {
        val burstClass = PaymentSheetTest::class.java
        val nonBurstClass = BurstAndroidJUnitRunnerTest::class.java
        val classArgument = "${burstClass.name},${nonBurstClass.name}"
        val arguments = Bundle().apply {
            putString(CLASS_ARGUMENT, classArgument)
        }

        BurstAndroidJUnitRunner().rebuildWithAllBurstTests(arguments)

        val testsRegex = requireNotNull(arguments.getString(TESTS_REGEX_ARGUMENT)).toRegex()
        assertThat(arguments.containsKey(CLASS_ARGUMENT)).isFalse()
        assertThat(
            testsRegex.containsMatchIn(
                "${burstClass.name}_PaymentConfigurationOnly_Activity#testSuccessfulCardPayment"
            )
        ).isTrue()
        assertThat(
            testsRegex.containsMatchIn("${nonBurstClass.name}#leavesNonBurstClassSelectionUnchanged")
        ).isTrue()
        assertThat(
            testsRegex.containsMatchIn("${nonBurstClass.name}_Other#someTest")
        ).isFalse()
    }

    @Test
    fun rebuildsMultipleBurstClassSelection() {
        val firstBurstClass = PaymentSheetTest::class.java
        val secondBurstClass = CardNumberControllerNetworkTest::class.java
        val arguments = Bundle().apply {
            putString(CLASS_ARGUMENT, "${firstBurstClass.name},${secondBurstClass.name}")
        }

        BurstAndroidJUnitRunner().rebuildWithAllBurstTests(arguments)

        val testsRegex = requireNotNull(arguments.getString(TESTS_REGEX_ARGUMENT)).toRegex()
        assertThat(arguments.containsKey(CLASS_ARGUMENT)).isFalse()
        assertThat(
            testsRegex.containsMatchIn(
                "${firstBurstClass.name}_PaymentConfigurationOnly_Activity#testSuccessfulCardPayment"
            )
        ).isTrue()
        assertThat(
            testsRegex.containsMatchIn(
                "${secondBurstClass.name}_PaymentConfigurationOnly_Activity#" +
                    "testNoCardMetadataRequestWhenAllFundingTypesAllowed"
            )
        ).isTrue()
    }

    @Test
    fun rebuildsMultipleMethodSelection() {
        val burstClass = TapToAddTest::class.java
        val burstMethod = "successWithCompleteMode"
        val nonBurstClass = BurstAndroidJUnitRunnerTest::class.java
        val nonBurstMethod = "leavesNonBurstClassSelectionUnchanged"
        val arguments = Bundle().apply {
            putString(
                CLASS_ARGUMENT,
                "${burstClass.name}#$burstMethod,${nonBurstClass.name}#$nonBurstMethod"
            )
        }

        BurstAndroidJUnitRunner().rebuildWithAllBurstTests(arguments)

        val testsRegex = requireNotNull(arguments.getString(TESTS_REGEX_ARGUMENT)).toRegex()
        assertThat(arguments.containsKey(CLASS_ARGUMENT)).isFalse()
        assertThat(
            testsRegex.containsMatchIn(
                "${burstClass.name}#${burstMethod}_PaymentConfigurationOnly_PaymentSheet"
            )
        ).isTrue()
        assertThat(
            testsRegex.containsMatchIn("${nonBurstClass.name}#$nonBurstMethod")
        ).isTrue()
        assertThat(
            testsRegex.containsMatchIn("${nonBurstClass.name}#${nonBurstMethod}_Other")
        ).isFalse()
    }

    @Test
    fun leavesMultipleNonBurstClassSelectionUnchanged() {
        val classArgument = "${BurstAndroidJUnitRunnerTest::class.java.name},${String::class.java.name}"
        val arguments = Bundle().apply {
            putString(CLASS_ARGUMENT, classArgument)
        }

        BurstAndroidJUnitRunner().rebuildWithAllBurstTests(arguments)

        assertThat(arguments.getString(CLASS_ARGUMENT)).isEqualTo(classArgument)
        assertThat(arguments.containsKey(TESTS_REGEX_ARGUMENT)).isFalse()
    }

    @Test
    fun leavesNonBurstClassSelectionUnchanged() {
        val classArgument = BurstAndroidJUnitRunnerTest::class.java.name
        val arguments = Bundle().apply {
            putString(CLASS_ARGUMENT, classArgument)
        }

        BurstAndroidJUnitRunner().rebuildWithAllBurstTests(arguments)

        assertThat(arguments.getString(CLASS_ARGUMENT)).isEqualTo(classArgument)
        assertThat(arguments.containsKey(TESTS_REGEX_ARGUMENT)).isFalse()
        assertThat(
            BurstAndroidJUnitRunnerTest::class.java.isAnnotationPresent(Burst::class.java)
        ).isFalse()
    }

    private companion object {
        const val CLASS_ARGUMENT = "class"
        const val TESTS_REGEX_ARGUMENT = "tests_regex"
    }
}
