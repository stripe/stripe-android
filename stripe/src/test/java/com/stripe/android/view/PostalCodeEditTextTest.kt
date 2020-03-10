package com.stripe.android.view

import android.text.InputType
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PostalCodeEditTextTest {
    private val postalCodeEditText = PostalCodeEditText(
        ApplicationProvider.getApplicationContext()
    )

    @Test
    fun testConfigureForUs() {
        postalCodeEditText.configureForUs()
        assertThat(postalCodeEditText.inputType)
            .isEqualTo(InputType.TYPE_CLASS_NUMBER)
    }
}
