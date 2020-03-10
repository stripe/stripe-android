package com.stripe.android.view

import android.app.Activity
import android.content.Context
import android.text.InputType
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.textfield.TextInputLayout
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.stripe.android.CustomerSession
import com.stripe.android.PaymentSessionFixtures
import kotlin.test.BeforeTest
import kotlin.test.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PostalCodeEditTextTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val postalCodeEditText = PostalCodeEditText(context)

    @BeforeTest
    fun setup() {
        CustomerSession.instance = mock()
    }

    @Test
    fun testConfigureForUs() {
        postalCodeEditText.config = PostalCodeEditText.Config.US
        assertThat(postalCodeEditText.inputType)
            .isEqualTo(InputType.TYPE_CLASS_NUMBER)
    }

    @Test
    fun postalCode_whenConfiguredForUs_shouldValidate() {
        postalCodeEditText.config = PostalCodeEditText.Config.US
        assertThat(postalCodeEditText.postalCode)
            .isNull()

        postalCodeEditText.setText("1234")
        assertThat(postalCodeEditText.postalCode)
            .isNull()

        postalCodeEditText.setText("123 5")
        assertThat(postalCodeEditText.postalCode)
            .isNull()

        postalCodeEditText.setText("12345")
        assertThat(postalCodeEditText.postalCode)
            .isEqualTo("12345")
    }

    @Test
    fun updateHint_whenTextInputLayoutHintEnabled_shouldSetHintOnTextInputLayout() {
        createActivity {
            val textInputLayout = TextInputLayout(it)
            textInputLayout.addView(postalCodeEditText)

            postalCodeEditText.config = PostalCodeEditText.Config.US
            assertThat(textInputLayout.hint)
                .isEqualTo("ZIP code")
        }
    }

    @Test
    fun updateHint_whenTextInputLayoutHintDisabled_shouldSetHintOnEditText() {
        createActivity {
            val textInputLayout = TextInputLayout(it)
            textInputLayout.isHintEnabled = false
            textInputLayout.addView(postalCodeEditText)

            postalCodeEditText.config = PostalCodeEditText.Config.US
            assertThat(textInputLayout.hint)
                .isNull()
            assertThat(postalCodeEditText.hint)
                .isEqualTo("ZIP code")
        }
    }

    private fun createActivity(onActivityCallback: (Activity) -> Unit) {
        ActivityScenarioFactory(context).create<PaymentFlowActivity>(
            PaymentSessionFixtures.PAYMENT_FLOW_ARGS
        ).use { activityScenario ->
            activityScenario.onActivity(onActivityCallback)
        }
    }
}
