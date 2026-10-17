package com.stripe.android.uicore.elements

import com.google.common.truth.Truth.assertThat
import org.junit.Test

internal class FieldValidationMessageComparatorTest {

    @Test
    fun `error is less than warning`() {
        val error = FieldValidationMessage.Error(1)
        val warning = FieldValidationMessage.Warning(2)

        assertThat(DefaultFieldValidationMessageComparator.compare(error, warning)).isLessThan(0)
    }

    @Test
    fun `error is less than null`() {
        val error = FieldValidationMessage.Error(1)

        assertThat(DefaultFieldValidationMessageComparator.compare(error, null)).isLessThan(0)
    }

    @Test
    fun `warning is less than null`() {
        val warning = FieldValidationMessage.Warning(1)

        assertThat(DefaultFieldValidationMessageComparator.compare(warning, null)).isLessThan(0)
    }

    @Test
    fun `warning is greater than error`() {
        val error = FieldValidationMessage.Error(1)
        val warning = FieldValidationMessage.Warning(2)

        assertThat(DefaultFieldValidationMessageComparator.compare(warning, error)).isGreaterThan(0)
    }

    @Test
    fun `null is greater than error`() {
        val error = FieldValidationMessage.Error(1)

        assertThat(DefaultFieldValidationMessageComparator.compare(null, error)).isGreaterThan(0)
    }

    @Test
    fun `null is greater than warning`() {
        val warning = FieldValidationMessage.Warning(1)

        assertThat(DefaultFieldValidationMessageComparator.compare(null, warning)).isGreaterThan(0)
    }

    @Test
    fun `two errors are equal`() {
        val error1 = FieldValidationMessage.Error(1)
        val error2 = FieldValidationMessage.Error(2)

        assertThat(DefaultFieldValidationMessageComparator.compare(error1, error2)).isEqualTo(0)
    }

    @Test
    fun `two warnings are equal`() {
        val warning1 = FieldValidationMessage.Warning(1)
        val warning2 = FieldValidationMessage.Warning(2)

        assertThat(DefaultFieldValidationMessageComparator.compare(warning1, warning2)).isEqualTo(0)
    }

    @Test
    fun `two nulls are equal`() {
        assertThat(DefaultFieldValidationMessageComparator.compare(null, null)).isEqualTo(0)
    }
}
