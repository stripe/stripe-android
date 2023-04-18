package com.stripe.android.identity.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DobTextFieldConfigTest {
    @Test
    fun testErrorDateBefore1900() {
        assertThat(
            shouldShowErrorWithFocus(dateString = "01011899")
        ).isEqualTo(true)
    }

    @Test
    fun testErrorDateInFuture() {
        val dateFormat = SimpleDateFormat(
            "MMddyyyy",
            Locale.getDefault()
        )
        val tenDaysFromNowString =
            dateFormat.format(
                Calendar.getInstance().also { it.add(Calendar.DAY_OF_YEAR, 10) }.time
            )

        assertThat(
            shouldShowErrorWithFocus(dateString = tenDaysFromNowString)
        ).isEqualTo(true)
    }

    @Test
    fun testCorrectDate() {
        val dateFormat = SimpleDateFormat(
            "MMddyyyy",
            Locale.getDefault()
        )
        val tenDaysBeforeString =
            dateFormat.format(
                Calendar.getInstance().also { it.add(Calendar.DAY_OF_YEAR, -10) }.time
            )

        assertThat(
            shouldShowErrorWithFocus(dateString = tenDaysBeforeString)
        ).isEqualTo(false)
    }

    private fun shouldShowErrorWithFocus(dateString: String) =
        DobTextFieldConfig.determineState(dateString).shouldShowError(false)
}
