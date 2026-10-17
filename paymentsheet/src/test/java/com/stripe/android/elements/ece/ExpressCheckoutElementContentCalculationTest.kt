package com.stripe.android.elements.ece

import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
internal class ExpressCheckoutElementContentCalculationTest {
    @Test
    fun `calculates visible button count`(
        @TestParameter(valuesProvider = VisibleButtonCountCaseProvider::class)
        testCase: VisibleButtonCountCase,
    ) {
        val result = calculateVisibleButtonCount(
            buttonCount = testCase.buttonCount,
            maxColumns = testCase.maxColumns,
            maxRows = testCase.maxRows,
        )

        assertThat(result).isEqualTo(testCase.expected)
    }

    @Test
    fun `calculates column count`(
        @TestParameter(valuesProvider = ColumnCountCaseProvider::class)
        testCase: ColumnCountCase,
    ) {
        val result = calculateColumnCount(
            buttonCount = testCase.buttonCount,
            maxRows = testCase.maxRows,
        )

        assertThat(result).isEqualTo(testCase.expected)
    }
}

internal object VisibleButtonCountCaseProvider : TestParameterValuesProvider() {
    override fun provideValues(
        context: Context?,
    ): List<VisibleButtonCountCase> = listOf(
        VisibleButtonCountCase(
            name = "No buttons",
            buttonCount = 0,
            maxColumns = 2,
            maxRows = 2,
            expected = 0,
        ),
        VisibleButtonCountCase(
            name = "No limits",
            buttonCount = 5,
            maxColumns = null,
            maxRows = null,
            expected = 5,
        ),
        VisibleButtonCountCase(
            name = "Columns only",
            buttonCount = 5,
            maxColumns = 2,
            maxRows = null,
            expected = 5,
        ),
        VisibleButtonCountCase(
            name = "Rows only",
            buttonCount = 5,
            maxColumns = null,
            maxRows = 2,
            expected = 5,
        ),
        VisibleButtonCountCase(
            name = "Capacity below available buttons",
            buttonCount = 5,
            maxColumns = 2,
            maxRows = 2,
            expected = 4,
        ),
        VisibleButtonCountCase(
            name = "Capacity above available buttons",
            buttonCount = 3,
            maxColumns = 2,
            maxRows = 2,
            expected = 3,
        ),
    )
}

internal data class VisibleButtonCountCase(
    val name: String,
    val buttonCount: Int,
    val maxColumns: Int?,
    val maxRows: Int?,
    val expected: Int,
) {
    override fun toString(): String = name
}

internal object ColumnCountCaseProvider : TestParameterValuesProvider() {
    override fun provideValues(
        context: Context?,
    ): List<ColumnCountCase> = listOf(
        ColumnCountCase(
            name = "No buttons",
            buttonCount = 0,
            maxRows = 2,
            expected = 1,
        ),
        ColumnCountCase(
            name = "No limits",
            buttonCount = 5,
            maxRows = null,
            expected = 1,
        ),
        ColumnCountCase(
            name = "Maximum rows above button count",
            buttonCount = 3,
            maxRows = 5,
            expected = 1,
        ),
        ColumnCountCase(
            name = "Maximum rows equals button count",
            buttonCount = 3,
            maxRows = 3,
            expected = 1,
        ),
        ColumnCountCase(
            name = "Rows require even columns",
            buttonCount = 4,
            maxRows = 2,
            expected = 2,
        ),
        ColumnCountCase(
            name = "Rows require uneven columns",
            buttonCount = 5,
            maxRows = 2,
            expected = 3,
        ),
        ColumnCountCase(
            name = "Single row requires one column per button",
            buttonCount = 5,
            maxRows = 1,
            expected = 5,
        ),
    )
}

internal data class ColumnCountCase(
    val name: String,
    val buttonCount: Int,
    val maxRows: Int?,
    val expected: Int,
) {
    override fun toString(): String = name
}
