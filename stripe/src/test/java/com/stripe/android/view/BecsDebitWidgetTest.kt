package com.stripe.android.view

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class BecsDebitWidgetTest : BaseViewTest<BecsDebitWidgetTestActivity>(
    BecsDebitWidgetTestActivity::class.java
) {
    private val activity: BecsDebitWidgetTestActivity by lazy {
        createStartedActivity()
    }
    private val becsDebitWidget: BecsDebitWidget by lazy {
        activity.becsDebitWidget
    }

    private val nameEditText: StripeEditText by lazy {
        becsDebitWidget.viewBinding.nameEditText
    }
    private val emailEditText: StripeEditText by lazy {
        becsDebitWidget.viewBinding.emailEditText
    }
    private val bsbEditText: BecsDebitBsbEditText by lazy {
        becsDebitWidget.viewBinding.bsbEditText
    }
    private val accountNumberEditText: BecsDebitAccountNumberEditText by lazy {
        becsDebitWidget.viewBinding.accountNumberEditText
    }

    @BeforeTest
    fun setup() {
        resumeStartedActivity(activity)
    }

    @AfterTest
    override fun tearDown() {
        super.tearDown()
        activity.finish()
    }

    @Test
    fun testBsbHelperText() {
        assertThat(becsDebitWidget.viewBinding.bsbTextInputLayout.isErrorEnabled)
            .isFalse()
        bsbEditText.setText("21")
        assertThat(becsDebitWidget.viewBinding.bsbTextInputLayout.isErrorEnabled)
            .isFalse()
        assertThat(becsDebitWidget.viewBinding.bsbTextInputLayout.isHelperTextEnabled)
            .isTrue()
    }

    @Test
    fun params_whenBsbInvalid_enablesError() {
        assertThat(becsDebitWidget.viewBinding.bsbTextInputLayout.isErrorEnabled)
            .isFalse()
        bsbEditText.setText("123")
        becsDebitWidget.params
        assertThat(becsDebitWidget.viewBinding.bsbTextInputLayout.isErrorEnabled)
            .isTrue()
        assertThat(bsbEditText.errorMessage)
            .isEqualTo("The BSB you entered is incomplete.")
    }

    @Test
    fun params_withEmptyFields_shouldReturnNull() {
        assertThat(becsDebitWidget.params)
            .isNull()
    }

    @Test
    fun params_withValidInput_shouldReturnValidParams() {
        nameEditText.setText("Jenny Rosen")
        emailEditText.setText("jrosen@example.com")
        bsbEditText.setText(VALID_BSB_NUMBER)
        accountNumberEditText.setText(VALID_ACCOUNT_NUMBER)

        assertThat(becsDebitWidget.params)
            .isEqualTo(
                PaymentMethodCreateParams.create(
                    PaymentMethodCreateParams.AuBecsDebit(
                        bsbNumber = "000000",
                        accountNumber = VALID_ACCOUNT_NUMBER
                    ),
                    PaymentMethod.BillingDetails.Builder()
                        .setName("Jenny Rosen")
                        .setEmail("jrosen@example.com")
                        .build()
                )
            )
    }

    @Test
    fun bsbChange_shouldUpdateAccountNumberEditTextMinLength() {
        // default
        assertThat(accountNumberEditText.minLength)
            .isEqualTo(5)

        // Stripe BSB
        bsbEditText.setText("00")
        assertThat(accountNumberEditText.minLength)
            .isEqualTo(9)

        // Stripe BSB
        bsbEditText.setText("80")
        assertThat(accountNumberEditText.minLength)
            .isEqualTo(4)
    }

    @Test
    fun validParamsCallback_shouldBeCalledWhenInputChanges() {
        var isValidArg = false
        var callbacks = 0
        becsDebitWidget.validParamsCallback = object : BecsDebitWidget.ValidParamsCallback {
            override fun onInputChanged(isValid: Boolean) {
                callbacks++
                isValidArg = isValid
            }
        }

        assertThat(isValidArg)
            .isFalse()
        assertThat(callbacks)
            .isEqualTo(0)

        nameEditText.setText("Jenny Rosen")
        assertThat(isValidArg)
            .isFalse()
        assertThat(callbacks)
            .isEqualTo(1)

        emailEditText.setText("jrosen@example.com")
        assertThat(isValidArg)
            .isFalse()
        assertThat(callbacks)
            .isEqualTo(2)

        bsbEditText.setText("000000")
        assertThat(isValidArg)
            .isFalse()
        // there is an additional callback because "000000" is formatted to "000-000"
        assertThat(callbacks)
            .isEqualTo(4)

        accountNumberEditText.setText("000123456")
        assertThat(isValidArg)
            .isTrue()
        assertThat(callbacks)
            .isEqualTo(5)
    }

    private companion object {
        private const val VALID_BSB_NUMBER = "000000"
        private const val VALID_ACCOUNT_NUMBER = "000123456"
    }
}
