package com.stripe.android.view

import android.app.Activity
import android.text.InputType
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.textfield.TextInputLayout
import com.google.common.truth.Truth.assertThat
import com.stripe.android.R
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

// TODO: need to rerun this test
@RunWith(RobolectricTestRunner::class)
class PostalCodeEditTextTest {
    private val context = ContextThemeWrapper(
        ApplicationProvider.getApplicationContext(),
        R.style.StripeDefaultTheme
    )
    private val postalCodeEditText = PostalCodeEditText(context)

    @Test
    fun testConfigureForUs() {
        postalCodeEditText.config = PostalCodeEditText.Config.US
        assertThat(postalCodeEditText.inputType)
            .isEqualTo(InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD)
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
    fun changing_from_Us_to_other_country_should_allow_longer_postal() {
        postalCodeEditText.config = PostalCodeEditText.Config.US
        postalCodeEditText.setText("123456")
        assertThat(postalCodeEditText.postalCode)
            .isEqualTo("12345")

        postalCodeEditText.config = PostalCodeEditText.Config.Global
        postalCodeEditText.setText("123456")
        assertThat(postalCodeEditText.postalCode)
            .isEqualTo("123456")
    }

    @Test
    fun updateHint_whenTextInputLayoutHintEnabled_shouldSetHintOnTextInputLayout() {
        createActivity {
            val textInputLayout = TextInputLayout(it)
            textInputLayout.addView(postalCodeEditText)

            postalCodeEditText.config = PostalCodeEditText.Config.US
            assertThat(textInputLayout.hint)
                .isEqualTo("ZIP Code")
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
                .isEqualTo("ZIP Code")
        }
    }

    private fun createActivity(onActivityCallback: (Activity) -> Unit) {
        // TODO: will need to use a different activity for this
//        ActivityScenarioFactory(context).create<PaymentFlowActivity>(
//            PaymentSessionFixtures.PAYMENT_FLOW_ARGS
//        ).use { activityScenario ->
//            activityScenario.onActivity(onActivityCallback)
//        }
    }
}
