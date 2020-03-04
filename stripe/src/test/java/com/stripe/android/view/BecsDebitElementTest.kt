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
internal class BecsDebitElementTest : BaseViewTest<BecsDebitElementTestActivity>(
    BecsDebitElementTestActivity::class.java
) {
    private val activity: BecsDebitElementTestActivity by lazy {
        createStartedActivity()
    }
    private val becsDebitElement: BecsDebitElement by lazy {
        activity.becsDebitElement
    }

    private val nameEditText: StripeEditText by lazy {
        becsDebitElement.viewBinding.nameEditText
    }
    private val emailEditText: StripeEditText by lazy {
        becsDebitElement.viewBinding.emailEditText
    }
    private val bsbEditText: StripeEditText by lazy {
        becsDebitElement.viewBinding.bsbEditText
    }
    private val accountNumberEditText: StripeEditText by lazy {
        becsDebitElement.viewBinding.accountNumberEditText
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
    fun testBsbError() {
        assertThat(becsDebitElement.viewBinding.bsbTextInputLayout.isErrorEnabled)
            .isFalse()
        bsbEditText.setText("999")
        assertThat(becsDebitElement.viewBinding.bsbTextInputLayout.isErrorEnabled)
            .isTrue()
    }

    @Test
    fun testBsbHelperText() {
        assertThat(becsDebitElement.viewBinding.bsbTextInputLayout.isErrorEnabled)
            .isFalse()
        bsbEditText.setText("21")
        assertThat(becsDebitElement.viewBinding.bsbTextInputLayout.isErrorEnabled)
            .isFalse()
        assertThat(becsDebitElement.viewBinding.bsbTextInputLayout.isHelperTextEnabled)
            .isTrue()
    }

    @Test
    fun params_withEmptyFields_shouldReturnNull() {
        assertThat(becsDebitElement.params)
            .isNull()
    }

    @Test
    fun params_withValidInput_shouldReturnValidParams() {
        nameEditText.setText("Jenny Rosen")
        emailEditText.setText("jrosen@example.com")
        bsbEditText.setText(VALID_BSB_NUMBER)
        accountNumberEditText.setText(VALID_ACCOUNT_NUMBER)

        assertThat(becsDebitElement.params)
            .isEqualTo(
                PaymentMethodCreateParams.create(
                    PaymentMethodCreateParams.AuBecsDebit(
                        bsbNumber = "000-000",
                        accountNumber = VALID_ACCOUNT_NUMBER
                    ),
                    PaymentMethod.BillingDetails.Builder()
                        .setName("Jenny Rosen")
                        .setEmail("jrosen@example.com")
                        .build()
                )
            )
    }

    private companion object {
        private const val VALID_BSB_NUMBER = "000000"
        private const val VALID_ACCOUNT_NUMBER = "000123456"
    }
}
